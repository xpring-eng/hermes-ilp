package org.interledger.spsp.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

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
import org.interledger.link.http.SimpleAuthSettings;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.client.ConnectorTokensClient;
import org.interledger.spsp.server.util.JwksServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import okhttp3.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.time.Instant;

/**
 * Helper class for creating the connector and spsp containers for integration tests.
 */
public class TestIlpContainers {

  public static final String SPSP = "spsp";
  public static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";
  public static final int WIRE_MOCK_PORT = 30133;
  public static final String ADMIN_AUTH_TOKEN = "YWRtaW46cGFzc3dvcmQ=";
  private static final int WEB_PORT = 8080;
  private static ObjectMapper objectMapper = ObjectMapperFactory.create();

  private WireMockServer wireMockServer = new WireMockServer(wireMockConfig().port(WIRE_MOCK_PORT));
  private JwksServer jwtServer = new JwksServer();
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Network network = Network.newNetwork();
  private GenericContainer interledgerNode;
  private GenericContainer spspServer;

  private final HttpUrl issuer = HttpUrl.parse("http://host.testcontainers.internal:" + WIRE_MOCK_PORT);

  public static TestIlpContainers createContainers() {
    TestIlpContainers containers = new TestIlpContainers();
    Testcontainers.exposeHostPorts(WIRE_MOCK_PORT);
    containers.wireMockServer.start();
    WireMock.configureFor("localhost", WIRE_MOCK_PORT);
    containers.startIlpNode();
    containers.createReceiverAccount();
    containers.startSpsp();
    containers.resetJwks();
    return containers;
  }

  public void stop() {
    interledgerNode.stop();
    spspServer.stop();
    wireMockServer.stop();
  }

  private void createReceiverAccount() {
    AccountId accountId = AccountId.of(SPSP);
    IlpOverHttpLinkSettings authSettings = IlpOverHttpLinkSettings.builder()
      .incomingLinkSettings(IncomingLinkSettings.builder().authType(IlpOverHttpLinkSettings.AuthType.SIMPLE)
        .simpleAuthSettings(SimpleAuthSettings.forAuthToken("shh"))
        .build()
      ).build();

    adminClient().createAccount(AccountSettings.builder()
      .accountId(accountId)
      .assetCode("XRP")
      .assetScale(9)
      .isSendRoutes(false)
      .isReceiveRoutes(false)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .customSettings(authSettings.toCustomSettingsMap())
      .build());
  }

  private void startIlpNode() {
    interledgerNode = new GenericContainer<>("interledger4j/java-ilpv4-connector:0.3.2-SNAPSHOT")
      .withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger))
      .withNetworkAliases("connector")
      .withExposedPorts(WEB_PORT)
      .withNetwork(network);
    interledgerNode.start();
  }

  private void startSpsp() {
    spspServer = new GenericContainer<>("interledger4j/spsp-server:0.1-SNAPSHOT")
      .withExposedPorts(WEB_PORT)
      .withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer (logger))
      .withNetworkAliases("spsp")
      .withEnv("interledger.spsp_server.parent_account.custom_settings.ilpOverHttp.outgoing.token_subject", "spsp")
      .withEnv("interledger.spsp_server.parent_account.custom_settings.ilpOverHttp.outgoing.shared_secret",
        "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=")
      .withEnv("interledger.spsp_server.parent_account.custom_settings.ilpOverHttp.outgoing.url",
        "http://connector:" + WEB_PORT + "/accounts/" + SPSP + "/ilp")
      .withNetwork(network);
    spspServer.start();
  }

  public ConnectorAdminClient adminClient() {
    return ConnectorAdminClient.construct(getNodeBaseUri(), template -> {
      template.header(AUTHORIZATION, "Basic " + ADMIN_AUTH_TOKEN);
    });
  }

  public ConnectorRoutesClient routesClient() {
    return ConnectorRoutesClient.construct(getNodeBaseUri(), template -> {
      template.header("Authorization", "Basic " + ADMIN_AUTH_TOKEN);
    });
  }

  public ConnectorBalanceClient balanceClient() {
    return ConnectorBalanceClient.construct(getNodeBaseUri());
  }

  public ConnectorTokensClient tokensClient() {
    return ConnectorTokensClient.construct(getNodeBaseUri());
  }

  /**
   * Helper method to return the base URL for the connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  public HttpUrl getNodeBaseUri() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(interledgerNode.getContainerIpAddress())
      .port(interledgerNode.getFirstMappedPort())
      .build();
  }

  /**
   * Helper method to return the base URL for the connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  public HttpUrl getSpspBaseUri() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(spspServer.getContainerIpAddress())
      .port(spspServer.getFirstMappedPort())
      .build();
  }


  public HttpUrl getIssuerUrl() {
    return issuer;
  }

  public String paymentPointerBase() {
    return "$" + spspServer.getContainerIpAddress() + ":" + spspServer.getFirstMappedPort();
  }

  public void resetJwks() {
    jwtServer.resetKeyPairs();
    WireMock.reset();
    try {
      stubFor(get(urlEqualTo(WELL_KNOWN_JWKS_JSON))
        .willReturn(aResponse()
          .withStatus(200)
          .withBody(objectMapper.writeValueAsString(jwtServer.getJwks()))
        ));
    } catch (JsonProcessingException e) {
      // this should never happen
      throw new IllegalStateException(e);
    }
  }

  public String createJwt(String subject, int expirySeconds) {
    JwtAuthSettings jwtAuthSettings = defaultJwtAuthSettings(subject);
    return jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(600));
  }

  public String createJwt(String subject) {
    return createJwt(subject, 600);
  }

  public ImmutableJwtAuthSettings defaultJwtAuthSettings(String subject) {
    return JwtAuthSettings.builder()
      .tokenIssuer(getIssuerUrl())
      .tokenSubject(subject)
      .tokenAudience("bar")
      .build();
  }

}
