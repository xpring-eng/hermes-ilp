package org.interledger.spsp.server.controllers;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.core.InterledgerConstants.ALL_ZEROS_FULFILLMENT;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpResponse;
import org.interledger.link.Link;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.server.SpspServerApplication;
import org.interledger.spsp.server.controllers.IlpOverHttpStreamReceiverTest.TestConfiguration;
import org.interledger.spsp.server.model.SpspServerSettings;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.SenderAmountMode;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = WebEnvironment.RANDOM_PORT,
  classes = {SpspServerApplication.class, TestConfiguration.class}
)
@ActiveProfiles({"test"})
public class IlpOverHttpStreamReceiverTest {

  private static final String ALICE = "alice";
  private static final InterledgerAddress SIMULATED_CONNECTOR_OPERATOR = InterledgerAddress.of("example.connie");
  private static final InterledgerAddress SPSP_OPERATOR = SIMULATED_CONNECTOR_OPERATOR.with(ALICE);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @LocalServerPort
  private int randomServerPort;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  @Autowired
  private SpspServerSettings spspServerSettings;

  private HttpUrl spspServerUrl;

  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  // This link simulates a parent Connector that will be making prepare-packet requests to this SPSP receiver. This
  // is used to make IL-DCP requests.
  //private IlpOverHttpLink linkToSimulatedParentConnector;

  // The Link that can connect to the running SPSP server's `/ilp` endpoint (e.g., simulates traffic coming from the
  // simulated Connector).
  private Link<?> linkToSpspServer;

  private static OkHttpClient newHttpClient() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
      .cookieJar(NO_COOKIES)
      .connectTimeout(5000, TimeUnit.MILLISECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS);
    return builder.connectionPool(connectionPool).build();
  }

  @Before
  public void setUp() {
    initMocks(this);

    spspServerUrl = HttpUrl.parse("http://localhost:" + randomServerPort);

    // In order to connect to the SPSP server, we need to construct a Link that has custom-settings configured properly
    // because the Link uses these custom-settings to load various pieces of information (as opposed to the Java
    // properties). See https://github.com/hyperledger/quilt/issues/383 for the issue that will fix this problem, and
    // mean that we can merely use Java here instead of having to deal with a Map of custom settings.

    //    final IlpOverHttpLinkSettings linkSettingsForLinkToSpspServer = IlpOverHttpLinkSettings
    //      .builder()
    //      .incomingHttpLinkSettings(IncomingLinkSettings.builder()
    //        .authType(AuthType.SIMPLE)
    //        .encryptedTokenSharedSecret("alice:c2ho")
    //        .build())
    //      .outgoingHttpLinkSettings(OutgoingLinkSettings.builder()
    //        .tokenSubject("n/a")
    //        .authType(AuthType.SIMPLE)
    //        .encryptedTokenSharedSecret(ENCRYPTED_SHH)
    //        .url(HttpUrl.parse(spspServerUrl + /ilp"))
    //        .build())
    //      .build();

    final Map customSettings = new HashMap();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, AuthType.SIMPLE.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, "n/a");

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, AuthType.SIMPLE.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "n/a");

    // Encrypted Secret is `alice:c2ho`
    customSettings.put(
      OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET,
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADNEgGPoHf9LP8yoFdZ29XbeIwYmQdDXuTD1k0ATBNa1LRoeSBQmK"
    );
    customSettings.put(
      OutgoingLinkSettings.HTTP_OUTGOING_URL,
      spspServerUrl.newBuilder().addPathSegment("ilp").build().toString()
    );

    final IlpOverHttpLinkSettings linkSettingsForLinkToSpspServer = IlpOverHttpLinkSettings
      .fromCustomSettings(customSettings)
      .build();

    this.linkToSpspServer = linkFactoryProvider.getLinkFactory(IlpOverHttpLink.LINK_TYPE)
      .constructLink(() -> spspServerSettings.operatorAddress(), linkSettingsForLinkToSpspServer);
    linkToSpspServer.setLinkId(LinkId.of("linkToSpspServer"));
  }

  @Test
  public void prepareWithNoStreamPackets() {
    linkToSpspServer.sendPacket(
      InterledgerPreparePacket.builder()
        .destination(InterledgerAddress.of("example.connie.alice"))
        .executionCondition(ALL_ZEROS_FULFILLMENT.getCondition())
        .expiresAt(Instant.now())
        .amount(UnsignedLong.ONE)
        .build()
    ).handle(
      fulfillPacket -> fail(
        "Should have rejected due to no STREAM Frame data in the Prepare packet but fulfilled: " + fulfillPacket),
      rejectPacket -> {
        assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT);
        assertThat(rejectPacket.getMessage()).isEqualTo("No STREAM frames in Prepare packet");
        assertThat(rejectPacket.getTriggeredBy()).isEqualTo(Optional.of(SPSP_OPERATOR));
        assertThat(rejectPacket.getData().length).isEqualTo(0);
      }
    );
  }

  @Test
  public void prepareWithInvalidReceiverAddress() throws IOException {

    // Prepare Packet for `example.connie.alice.foo` generated using a server_secret that matches the one used in this
    // test's application.yml file.
    final byte[] preparePacketWithStreamFrames = BaseEncoding.base16().decode(
      "0C8199000000000000000132303230313132363136343630393730319F57180B122F1A5979B25EE7D8A53F3E79EEB2E6B45D193A897BB5E"
        + "5FCAA8209186578616D706C652E636F6E6E69652E616C6963652E666F6F4600AA20178F9ED6E2B66143C6F26CB3D4BB442DF44C6EFCB"
        + "68AD1B4E24C767205855820270C241F55A5D4EE401ABFDF389DE2D4A0AC82370E6B0D98D8F1E49595C200EE29C203");

    InterledgerPreparePacket preparePacketWithInvalidAddress = InterledgerCodecContextFactory.oer()
      .read(InterledgerPreparePacket.class, new ByteArrayInputStream(preparePacketWithStreamFrames));

    linkToSpspServer.sendPacket(preparePacketWithInvalidAddress).handle(
      fulfillPacket -> fail("Should have rejected due to invalid ILP address token but fulfilled: " + fulfillPacket),
      rejectPacket -> {
        // TODO: Once See https://github.com/hyperledger/quilt/issues/378 is fixed, this should become an F06.
        //assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT);
        assertThat(rejectPacket.getCode()).isEqualTo(InterledgerErrorCode.T00_INTERNAL_ERROR);
        assertThat(rejectPacket.getMessage())
          .isEqualTo("{\"title\":\"Internal Server Error\",\"status\":500,\"detail\":\"Tag mismatch!\"}");
        assertThat(rejectPacket.getTriggeredBy()).isEqualTo(Optional.of(SPSP_OPERATOR));
        assertThat(rejectPacket.getData().length).isEqualTo(0);
      }
    );
  }

  @Test
  public void prepareWithValidStreamFrames() {
    // Create SPSP client and fetch shared secret and destination address using SPSP client
    SpspClient spspClient = new SpspClient(newHttpClient(), ALICE + ":" + "shh", spspServerUrl.toString());
    StreamConnectionDetails connectionDetails = spspClient.getStreamConnectionDetails(
      spspServerUrl.newBuilder().addPathSegment(ALICE).build()
    );

    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(linkToSpspServer);
    final SendMoneyResult paymentResult = simpleStreamSender.sendMoney(SendMoneyRequest.builder()
      .sourceAddress(InterledgerAddress.of("example.ilpOverHttpStreamReceivertest.prepareWithValidStreamFrames"))
      .senderAmountMode(SenderAmountMode.SENDER_AMOUNT)
      .denomination(Denomination.builder()
        .assetCode("USD")
        .assetScale((short) 2)
        .build())
      .destinationAddress(connectionDetails.destinationAddress())
      .timeout(Duration.of(10, ChronoUnit.SECONDS))
      .amount(UnsignedLong.ONE)
      .paymentTracker(new FixedSenderAmountPaymentTracker(UnsignedLong.ONE))
      .sharedSecret(connectionDetails.sharedSecret())
      .build()).join();

    assertThat(paymentResult.numRejectPackets()).isEqualTo(0);
    assertThat(paymentResult.totalPackets()).isEqualTo(1);
    assertThat(paymentResult.numFulfilledPackets()).isEqualTo(1);
    assertThat(paymentResult.amountDelivered()).isEqualTo(UnsignedLong.ONE);
    assertThat(paymentResult.amountLeftToSend()).isEqualTo(UnsignedLong.ZERO);
    assertThat(paymentResult.originalAmount()).isEqualTo(UnsignedLong.ONE);
    assertThat(paymentResult.amountSent()).isEqualTo(UnsignedLong.ONE);
  }

  @Configuration
  public static class TestConfiguration {

    @Bean
    @Primary
    IldcpFetcher ildcpFetcherMock() {
      IldcpFetcher ildcpFetcher = mock(IldcpFetcher.class);

      when(ildcpFetcher.fetch(any())).thenReturn(IldcpResponse.builder()
        .clientAddress(InterledgerAddress.of("example.connie.alice"))
        .assetCode("USD")
        .assetScale((short) 2)
        .build());

      return ildcpFetcher;
    }

  }
}
