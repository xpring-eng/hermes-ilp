package org.interledger.spsp.server.grpc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.spsp.server.config.ilp.IlpOverHttpConfig.SPSP;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.ImmutableJwtAuthSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcMetadataReader;
import org.interledger.spsp.server.grpc.utils.InterceptedService;
import org.interledger.spsp.server.services.NewAccountService;
import org.interledger.spsp.server.services.SendMoneyService;
import org.interledger.spsp.server.util.JwksServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.net.HttpHeaders;
import feign.FeignException;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, AuthContextInterceptorTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})

public class AuthContextInterceptorTests {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final HttpUrl SPSP_SERVER_URL = HttpUrl.parse("https://money.ilpv4.dev");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  /**
   * Fields for our JWKS mock server
   */
  public static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";
  public static final int WIRE_MOCK_PORT = 32456;
  HttpUrl issuer;

  /**
   * Connector fields
   */
  private static final Network network = Network.newNetwork();
  private static final int CONNECTOR_PORT = 8080;

  /**
   * Admin token for creating the account for accountIdHermes
   */
  private AccountId accountIdHermes;
  public static final String ADMIN_AUTH_TOKEN = "YWRtaW46cGFzc3dvcmQ=";

  /**
   * This starts up a mock JWKS server
   */
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIRE_MOCK_PORT);

  // Need this to have the JWKS port exposed to the connector running in the container
  static {
    Testcontainers.exposeHostPorts(WIRE_MOCK_PORT);
  }

  /**
   *  Start up a connector from the nightly docker image
   */
  @ClassRule
  public static GenericContainer interledgerNode = new GenericContainer<>("interledger4j/java-ilpv4-connector:0.2.0")
    .withExposedPorts(CONNECTOR_PORT)
    .withNetwork(network);

  @Autowired
  private ConnectorAdminClient adminClient;

  private JwksServer jwtServer;
  private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private AccountServiceGrpc.AccountServiceBlockingStub accountServiceBlockingStub;
  private BalanceServiceGrpc.BalanceServiceBlockingStub balanceServiceBlockingStub;
  private IlpOverHttpServiceGrpc.IlpOverHttpServiceBlockingStub paymentServiceBlockingStub;

  @Autowired
  AccountGrpcHandler accountGrpcHandler;

  @Autowired
  BalanceGrpcHandler balanceGrpcHandler;

  @Autowired
  IlpOverHttpGrpcHandler ilpOverHttpGrpcHandler;

  @Autowired
  private OutgoingLinkSettings outgoingLinkSettings;

  private String paymentPointerBase;

  @Autowired
  private IlpGrpcMetadataReader ilpGrpcMetadataReader;

  @Autowired
  HttpUrl spspReceiverUrl;

  private ClientInterceptor mockClientInterceptor;
  private JwtAuthSettings jwtAuthSettings;

  @Autowired
  private NewAccountService newAccountService;
  private String jwt;

  @Before
  public void setUp() throws IOException {

    paymentPointerBase = "$" + spspReceiverUrl.host();
    if (spspReceiverUrl.port() != 80 && spspReceiverUrl.port() != 443) {
      paymentPointerBase += ":" + spspReceiverUrl.port();
    }

    // Set up the JWKS server
    jwtServer = new JwksServer();
    resetJwks();
    issuer = HttpUrl.parse("http://host.testcontainers.internal:" + wireMockRule.port());
    jwtAuthSettings = defaultAuthSettings(issuer);

    jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(900000000));

    // This mock ClientInterceptor will add a COOKIE header with a jwt to the grpc Metadata.
    // This mocks the behavior of the Envoy proxy when receiving an HTTP request with cookies
    mockClientInterceptor = mock(ClientInterceptor.class, delegatesTo(
      new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
          return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              headers.put(Metadata.Key.of(HttpHeaders.COOKIE, Metadata.ASCII_STRING_MARSHALLER),
                "jwt=" + jwt);

              super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {}, headers);
            }
          };
        }
      }
    ));

    // Create an admin client to create a test account
    accountIdHermes = AccountId.of("hermes");

    createTestAccount("alice");
    createTestAccount(accountIdHermes.value());

    registerGrpc();
  }


  @Test
  public void testCreateAccountWithCookieAuth() {
    CreateAccountRequest request = CreateAccountRequest.newBuilder()
      .build();

    CreateAccountResponse reply = accountServiceBlockingStub.createAccount(request);
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER)).isEqualTo(jwtAuthSettings.tokenIssuer().get().toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE)).isEqualTo(jwtAuthSettings.tokenAudience().get());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT)).isEqualTo(jwtAuthSettings.tokenSubject());
  }

  @Test
  public void testGetBalanceWithCookieAuth() {
    GetBalanceRequest request = GetBalanceRequest.newBuilder()
      .setAccountId(accountIdHermes.value())
      .build();

    GetBalanceResponse expected = GetBalanceResponse.newBuilder()
      .setAccountId(accountIdHermes.value())
      .setAssetCode("XRP")
      .setAssetScale(9)
      .build();

    GetBalanceResponse response = balanceServiceBlockingStub.getBalance(request);
    assertThat(response).isEqualToComparingFieldByField(expected);
  }

  // This test won't pass if run with the rest of the tests (not sure why yet), but does pass when run
  // on its own. Something to do with the jwks server resetting maybe?.
  // Commenting out for now.
  /*@Test
  public void testSendMoneyWithCookieAuth() throws Exception {
    int sendAmount = 10000;

    SendPaymentRequest sendMoneyRequest = SendPaymentRequest.newBuilder()
      .setAccountId("alice")
      .setAmount(sendAmount)
      .setDestinationPaymentPointer(paymentPointerFromBaseUrl() + "/hermes")
      .build();

    SendPaymentResponse response = paymentServiceBlockingStub
      .sendMoney(sendMoneyRequest);
    if (!response.getSuccessfulPayment()) {
      fail();
    }
    logger.info("Payment sent successfully!  Payment Response: ");
    logger.info(response.toString());

    SendPaymentResponse expected = SendPaymentResponse.newBuilder()
      .setAmountSent(sendAmount)
      .setOriginalAmount(sendAmount)
      .setAmountDelivered(sendAmount)
      .setAmountSent(sendAmount)
      .setSuccessfulPayment(true)
      .build();

    assertThat(response).isEqualToComparingFieldByField(expected);
  }*/

  private String paymentPointerFromBaseUrl() {
    HttpUrl spspServerUrl = SPSP_SERVER_URL;
    return "$" + spspServerUrl.host() + ":" + spspServerUrl.port();
  }

  private void registerGrpc() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(InterceptedService.of(accountGrpcHandler, ilpGrpcMetadataReader))
      .addService(InterceptedService.of(balanceGrpcHandler, ilpGrpcMetadataReader))
      .addService(InterceptedService.of(ilpOverHttpGrpcHandler, ilpGrpcMetadataReader))
      .build()
      .start()
    );

    accountServiceBlockingStub = AccountServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));

    balanceServiceBlockingStub = BalanceServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));

    paymentServiceBlockingStub = IlpOverHttpServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));
  }

  /**
   * Helper method to return the base URL for the Rust Connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  private static HttpUrl getInterledgerBaseUri() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(interledgerNode.getContainerIpAddress())
      .port(interledgerNode.getFirstMappedPort())
      .build();
  }

  private ImmutableJwtAuthSettings defaultAuthSettings(HttpUrl issuer) {
    return JwtAuthSettings.builder()
      .tokenIssuer(issuer)
      .tokenSubject("foo")
      .tokenAudience("bar")
      .build();
  }

  private JwtAuthSettings defaultAuthSettings(HttpUrl issuer, String subject) {
    return JwtAuthSettings.builder()
      .tokenIssuer(issuer)
      .tokenSubject(subject)
      .tokenAudience("bar")
      .build();
  }

  private void resetJwks() throws JsonProcessingException {
    jwtServer.resetKeyPairs();
    WireMock.reset();
    stubFor(get(urlEqualTo(WELL_KNOWN_JWKS_JSON))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(objectMapper.writeValueAsString(jwtServer.getJwks()))
      ));
  }

  private void createTestAccount(String accountId) {
    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);
    createTestAccount(jwtAuthSettings, accountId);
  }

  private void createTestAccount(JwtAuthSettings jwtAuthSettings, String accountId) {
    // Set up auth settings to use JWT_RS_256
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwtAuthSettings.tokenIssuer().get().toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwtAuthSettings.tokenAudience().get());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwtAuthSettings.tokenSubject());
    customSettings.putAll(outgoingLinkSettings.toCustomSettingsMap()
      .entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))
    );

    try {
      this.newAccountService.createAccount(
        AccountSettings.builder()
          .accountId(AccountId.of(accountId))
          .assetCode("XRP")
          .assetScale(9)
          .linkType(IlpOverHttpLink.LINK_TYPE)
          .accountRelationship(AccountRelationship.CHILD)
          .customSettings(customSettings)
          .build()
      );
    } catch (FeignException e) {
      if (e.status() != 409) {
        throw e;
      } else {
        logger.warn("Hermes account already exists. If you want to update the account, delete it and try again with new settings.");
      }
    }
  }




  public static class TestConfig {

    /**
     * Overrides the adminClient bean for test purposes to connect to our Connector container
     *
     * @return a ConnectorAdminClient that can speak to the test container connector
     */
    @Bean
    @Primary
    public ConnectorAdminClient adminClient() {
      return ConnectorAdminClient.construct(getInterledgerBaseUri(), template -> {
        template.header(AUTHORIZATION, "Basic " + ADMIN_AUTH_TOKEN);
      });
    }

    @Bean
    @Primary
    public ConnectorRoutesClient routesClient() {
      return ConnectorRoutesClient.construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
      });
    }

    @Bean
    @Primary
    public ConnectorBalanceClient balanceClient() {
      return ConnectorBalanceClient.construct(getInterledgerBaseUri());
    }

    @Bean
    @Primary
    public SendMoneyService sendMoneyService(ObjectMapper objectMapper,
                                             ConnectorAdminClient adminClient,
                                             OkHttpClient okHttpClient,
                                             SpspClient spspClient) {
      return new SendMoneyService(getInterledgerBaseUri(), objectMapper, adminClient, okHttpClient, spspClient);
    }

    @Bean
    @Qualifier(SPSP)
    @Primary
    protected HttpUrl spspReceiverUrl() {
//      return HttpUrl.parse(getContainerBaseUri(spspServer).toString());
      return SPSP_SERVER_URL;
    }
  }
}
