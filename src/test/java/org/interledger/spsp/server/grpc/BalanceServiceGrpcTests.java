package org.interledger.spsp.server.grpc;


import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.ConnectorBalanceClient;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

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

  @ClassRule
  public static GenericContainer interledgerNode = new GenericContainer<>("interledger4j/java-ilpv4-connector:nightly")
    .withExposedPorts(CONNECTOR_PORT)
    .withNetwork(network)
    //.withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger)) // uncomment to see logs
    ;
  @Autowired
  BalanceServiceGrpc balanceServiceGrpc;

  private ConnectorAdminClient adminClient;

  @Before
  public void setUp() {
    accountIdHermes = AccountId.of("hermes");
    this.adminClient = ConnectorAdminClient
      .construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic " + ADMIN_AUTH_TOKEN);
      });

    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, accountIdHermes.value());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=");

    /*customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://xpringsandbox.auth0.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "0r1frZ59eylDsMH3acVeSJD5KI6puEho");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, "github|4440345");*/

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

    String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImtpZCI6Ik1rTTJPRGMxUVRKR05qSXlRelJFUmtKQk5qRTNNMFpCTkRsRFJEQkVSVFF3UWpWRk5VSkNNdyJ9.eyJuaWNrbmFtZSI6Im5oYXJ0bmVyIiwibmFtZSI6Im5oYXJ0bmVyQGdtYWlsLmNvbSIsInBpY3R1cmUiOiJodHRwczovL2F2YXRhcnMwLmdpdGh1YnVzZXJjb250ZW50LmNvbS91LzQ0NDAzNDU_dj00IiwidXBkYXRlZF9hdCI6IjIwMTktMTItMjdUMTg6NTI6MDMuMTg1WiIsImVtYWlsIjoibmhhcnRuZXJAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8veHByaW5nc2FuZGJveC5hdXRoMC5jb20vIiwic3ViIjoiZ2l0aHVifDQ0NDAzNDUiLCJhdWQiOiIwcjFmclo1OWV5bERzTUgzYWNWZVNKRDVLSTZwdUVobyIsImlhdCI6MTU3NzQ3MjczMywiZXhwIjoxNjA3MjczNzA1fQ.vZWmkBxeMhdYBjMhuZrTstXy-4K6lcoND_p2zUTM_bo";
    GetBalanceResponse reply =
      blockingStub.getBalance(GetBalanceRequest.newBuilder()
        .setAccountId(accountIdHermes.value())
        .setJwt(jwt)
        .build());

    logger.info("Reply: " + reply);
    assertThat(reply.getNetBalance()).isZero();
  }

  public static class TestConfig {

    @Bean
    @Primary
    public ConnectorBalanceClient balanceClient() {
      return ConnectorBalanceClient.construct(getInterledgerBaseUri());
    }
  }
}


