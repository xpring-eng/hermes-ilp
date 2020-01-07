package org.interledger.spsp.server.grpc;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

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
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.ConnectorBalanceClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.zalando.problem.ThrowableProblem;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, BalanceServiceGrpcTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class BalanceServiceGrpcTests {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";
  public static final int WIRE_MOCK_PORT = 32456;
  public static final String ADMIN_AUTH_TOKEN = "YWRtaW46cGFzc3dvcmQ=";
  private static final Network network = Network.newNetwork();
  private static final int CONNECTOR_PORT = 8080;
  private AccountId accountIdHermes;
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIRE_MOCK_PORT);

  static {
    Testcontainers.exposeHostPorts(WIRE_MOCK_PORT);
  }

  @ClassRule
  public static GenericContainer interledgerNode = new GenericContainer<>("interledger4j/java-ilpv4-connector:nightly")
    .withExposedPorts(CONNECTOR_PORT)
    .withNetwork(network)
    //.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger)) // uncomment to see logs
    ;
  @Autowired
  BalanceServiceGrpc balanceServiceGrpc;

  private ConnectorAdminClient adminClient;
  private JwksServer jwtServer;
  private ObjectMapper objectMapper = ObjectMapperFactory.create();
  HttpUrl issuer;

  @Before
  public void setUp() throws JsonProcessingException {

    jwtServer = new JwksServer();
    resetJwks();
    issuer = HttpUrl.parse("http://host.testcontainers.internal:" + wireMockRule.port());

    accountIdHermes = AccountId.of("hermes");
    this.adminClient = ConnectorAdminClient
      .construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic " + ADMIN_AUTH_TOKEN);
      });

    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);

    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwtAuthSettings.tokenIssuer().get().toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwtAuthSettings.tokenAudience().get());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwtAuthSettings.tokenSubject());

    try {
      this.adminClient.createAccount(
        AccountSettings.builder()
          .accountId(accountIdHermes)
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
        logger.info("Hermes account already exists. If you want to update the account, delete it and try again with new settings.");
      }
    }
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

  /**
   * To test the server, make calls with a real stub using the in-process channel, and verify
   * behaviors or state changes from the client side.
   */
  @Test
  public void getBalanceTest() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
      .forName(serverName).directExecutor().addService(balanceServiceGrpc).build().start());

    IlpServiceGrpc.IlpServiceBlockingStub blockingStub = IlpServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);
    String jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(10));

    GetBalanceResponse reply =
      blockingStub.getBalance(GetBalanceRequest.newBuilder()
        .setAccountId(accountIdHermes.value())
        .setJwt(jwt)
        .build());

    logger.info("Reply: " + reply);
    assertThat(reply.getAccountId()).isEqualTo("hermes");
    assertThat(reply.getAssetCode()).isEqualTo("XRP");
    assertThat(reply.getAssetScale()).isEqualTo(9);
    assertThat(reply.getClearingBalance()).isEqualTo(0L);
    assertThat(reply.getPrepaidAmount()).isEqualTo(0L);
    assertThat(reply.getNetBalance()).isZero();
  }

  private ImmutableJwtAuthSettings defaultAuthSettings(HttpUrl issuer) {
    return JwtAuthSettings.builder()
      .tokenIssuer(issuer)
      .tokenSubject("foo")
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

  public static class TestConfig {

    @Bean
    @Primary
    public ConnectorBalanceClient balanceClient() {
      return ConnectorBalanceClient.construct(getInterledgerBaseUri());
    }
  }
}


