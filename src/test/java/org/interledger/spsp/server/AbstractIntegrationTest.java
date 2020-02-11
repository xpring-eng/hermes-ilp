package org.interledger.spsp.server;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.link.http.ImmutableJwtAuthSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.JwksServer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;

public abstract class AbstractIntegrationTest {

  /**
   * Fields for our JWKS mock server
   */
  public static final String WELL_KNOWN_JWKS_JSON = "/.well-known/jwks.json";
  public static final int WIRE_MOCK_PORT = 32456;
  protected HttpUrl issuer;
  protected ObjectMapper objectMapper = ObjectMapperFactory.create();

  /**
   * This starts up a mock JWKS server
   */
  @Rule
  public WireMockRule wireMockRule = new WireMockRule(WIRE_MOCK_PORT);

  protected JwksServer jwtServer;

  // Need this to have the JWKS port exposed to the connector running in the container
  static {
    Testcontainers.exposeHostPorts(WIRE_MOCK_PORT);
  }

  protected JwtAuthSettings jwtAuthSettings;

  /**
   * Connector fields
   */
  protected static final Network network = Network.newNetwork();
  protected static final int CONNECTOR_PORT = 8080;

  /**
   * Admin token for creating the account for accountIdHermes
   */
  protected AccountId accountIdHermes;
  public static final String ADMIN_AUTH_TOKEN = "YWRtaW46cGFzc3dvcmQ=";

  /**
   *  Start up a connector from the nightly docker image
   */
  @ClassRule
  public static GenericContainer interledgerNode = new GenericContainer<>("interledger4j/java-ilpv4-connector:0.2.0") // FIXME use nightly
    .withExposedPorts(CONNECTOR_PORT)
    .withNetwork(network);

  protected String paymentPointerBase;

  @Autowired
  protected HttpUrl spspReceiverUrl;

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

    // Create an admin client to create a test account
    accountIdHermes = AccountId.of("hermes");
    jwtAuthSettings = defaultAuthSettings(issuer);
  }

  /**
   * Helper method to return the base URL for the Rust Connector.
   *
   * @return An {@link HttpUrl} to communicate with.
   */
  protected static HttpUrl getInterledgerBaseUri() {
    return new HttpUrl.Builder()
      .scheme("http")
      .host(interledgerNode.getContainerIpAddress())
      .port(interledgerNode.getFirstMappedPort())
      .build();
  }

  protected void resetJwks() throws JsonProcessingException {
    jwtServer.resetKeyPairs();
    WireMock.reset();
    stubFor(get(urlEqualTo(WELL_KNOWN_JWKS_JSON))
      .willReturn(aResponse()
        .withStatus(200)
        .withBody(objectMapper.writeValueAsString(jwtServer.getJwks()))
      ));
  }

  protected ImmutableJwtAuthSettings defaultAuthSettings(HttpUrl issuer) {
    return JwtAuthSettings.builder()
      .tokenIssuer(issuer)
      .tokenSubject("foo")
      .tokenAudience("bar")
      .build();
  }
}
