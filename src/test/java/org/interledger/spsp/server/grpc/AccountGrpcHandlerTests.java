package org.interledger.spsp.server.grpc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.interledger.spsp.server.AbstractIntegrationTest;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcMetadataReader;
import org.interledger.spsp.server.grpc.utils.InterceptedService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    classes = {HermesServerApplication.class, AccountGrpcHandlerTests.TestConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.main.allow-bean-definition-overriding=true"})
public class AccountGrpcHandlerTests extends AbstractGrpcHandlerTest {

  private Logger logger = LoggerFactory.getLogger(this.getClass());

  private AccountServiceGrpc.AccountServiceBlockingStub blockingStub;

  @Autowired
  AccountGrpcHandler accountGrpcHandler;

  @Autowired
  private OutgoingLinkSettings outgoingLinkSettings;

  @Before
  public void setUp() throws IOException {
    super.setUp();

    // Set up auth settings to use JWT_RS_256
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
        logger.warn("Hermes account already exists. If you want to update the account, delete it and try again with new settings.");
      }
    }
  }

  @Override
  public void registerGrpc() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(InterceptedService.of(accountGrpcHandler, ilpGrpcMetadataReader))
      .build()
      .start()
    );

    blockingStub = AccountServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  /**
   * Sends a request to the {@link AccountServiceGrpc} getAccount method for user 'connie'.
   */
  @Test
  public void getAccountForHermes() {

    GetAccountResponse reply =
        blockingStub.getAccount(GetAccountRequest.newBuilder().setAccountId(accountIdHermes.value()).build());

    logger.info(reply.toString());
    assertThat(reply.getAccountId()).isEqualTo("hermes");
    assertThat(reply.getAssetScale()).isEqualTo(9);
    assertThat(reply.getAssetCode()).isEqualTo("XRP");
    assertThat(reply.getLinkType()).isEqualTo(IlpOverHttpLink.LINK_TYPE.value());
    assertThat(reply.getAccountRelationship()).isEqualTo(AccountRelationship.CHILD.toString());
    assertThat(reply.getPaymentPointer()).isEqualTo(paymentPointerBase + "/" + accountIdHermes.value());
  }

  /**
   * Gets account for foo.  Should throw a {@link StatusRuntimeException} with code {@code Status.NOT_FOUND}
   */
  @Test
  public void getAccountFooFailsAccountNotFound() {
    AccountId accountId = AccountId.of("imaginary friend");

    try {
      GetAccountResponse reply =
        blockingStub.getAccount(GetAccountRequest.newBuilder().setAccountId(accountId.value()).build());
      fail();
    } catch (StatusRuntimeException e){
      System.out.println("Failed successfully.  Error status: " + e.getStatus());
      assertEquals(e.getStatus(), Status.NOT_FOUND);
    }
  }

  @Test
  public void createAccountWithTokenButNoRequest() {
    when(ilpGrpcMetadataReader.authorization(any())).thenReturn("Basic password");
    CreateAccountRequest request = CreateAccountRequest.newBuilder()
      .build();

    CreateAccountResponse reply = blockingStub.createAccount(request);
    logger.info(reply.toString());

    assertThat(reply.getAccountId()).startsWith("user_");
    assertThat(reply.getAssetCode()).isEqualTo("XRP");
    assertThat(reply.getAssetScale()).isEqualTo(9);
    assertThat(reply.getPaymentPointer()).isEqualTo(paymentPointerBase + "/" + reply.getAccountId());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().isEqualTo("password");
  }

  @Test
  public void createAccountWithNoTokenAndNoRequest() {
    CreateAccountResponse reply = blockingStub.createAccount(null);
    logger.info(reply.toString());

    assertThat(reply.getAccountId()).startsWith("user_");
    assertThat(reply.getAssetCode()).isEqualTo("XRP");
    assertThat(reply.getAssetScale()).isEqualTo(9);
    assertThat(reply.getPaymentPointer()).isEqualTo(paymentPointerBase + "/" + reply.getAccountId());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().isNotEmpty();
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().doesNotStartWith("enc:jks");
  }

  @Test
  public void createAccountWithSimpleAuthAndFullRequest() {
    when(ilpGrpcMetadataReader.authorization(any())).thenReturn("Basic password");
    CreateAccountRequest request = CreateAccountRequest.newBuilder()
      .setAccountId("foo")
      .setAssetCode("USD")
      .setAssetScale(4)
      .build();

    CreateAccountResponse reply = blockingStub.createAccount(request);
    logger.info(reply.toString());

    assertThat(reply.getAccountId()).isEqualTo("foo");
    assertThat(reply.getAssetCode()).isEqualTo("USD");
    assertThat(reply.getAssetScale()).isEqualTo(4);
    assertThat(reply.getPaymentPointer()).isEqualTo(paymentPointerBase + "/" + reply.getAccountId());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().isEqualTo("password");
  }

  /**
   * Creates an account through Hermes Grpc
   */
  @Test
  public void createAccountTestWithJwtTokenAndFullRequest() {
    String accountID = "AccountServiceGRPCTest";
    String accountDescription = "Noah's test account";

    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);
    String jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(10));

    Map<String, String> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwtAuthSettings.tokenIssuer().get().toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwtAuthSettings.tokenAudience().get());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwtAuthSettings.tokenSubject());

    customSettings.putAll(outgoingLinkSettings.toCustomSettingsMap()
      .entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))
    );

    CreateAccountResponse expected = CreateAccountResponse.newBuilder()
      .setAccountRelationship("CHILD")
      .setAssetCode("XRP")
      .setAssetScale(9)
      .putAllCustomSettings(customSettings)
      .setAccountId(accountID)
      .setDescription(accountDescription)
      .setLinkType(IlpOverHttpLink.LINK_TYPE_STRING)
      .setIsConnectionInitiator(true)
      .setIlpAddressSegment(accountID)
      .setBalanceSettings(CreateAccountResponse.BalanceSettings.newBuilder().build())
      .setIsChildAccount(true)
      .setIsInternal(false)
      .setIsSendRoutes(true)
      .setIsReceiveRoutes(false)
      .setMaxPacketsPerSecond(0)
      .setIsParentAccount(false)
      .setIsPeerAccount(false)
      .setIsPeerOrParentAccount(false)
      .setPaymentPointer(paymentPointerBase + "/AccountServiceGRPCTest")
      .build();

    CreateAccountRequest.Builder request = CreateAccountRequest.newBuilder()
      .setAccountId(accountID)
      .setAssetCode("XRP")
      .setAssetScale(9)
      .setDescription(accountDescription);

    when(ilpGrpcMetadataReader.authorization(any())).thenReturn("Bearer " + jwt);

    CreateAccountResponse reply = blockingStub.createAccount(request.build());
    logger.info(reply.toString());

    assertThat(expected)
      .isEqualToIgnoringGivenFields(reply, "customSettings_", "createdAt_", "memoizedHashCode", "modifiedAt_");
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER)).isEqualTo(jwtAuthSettings.tokenIssuer().get().toString());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE)).isEqualTo(jwtAuthSettings.tokenAudience().get());
    assertThat(reply.getCustomSettingsMap().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT)).isEqualTo(jwtAuthSettings.tokenSubject());
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
    public ConnectorBalanceClient balanceClient() {
      return ConnectorBalanceClient.construct(getInterledgerBaseUri());
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
    public IlpGrpcMetadataReader ilpGrpcMetadataReader() {
      return mock(IlpGrpcMetadataReader.class);
    }
  }
}


