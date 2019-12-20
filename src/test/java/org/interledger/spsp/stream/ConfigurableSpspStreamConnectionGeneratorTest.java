package org.interledger.spsp.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.interledger.codecs.stream.StreamCodecContextFactory;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.SharedSecret;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.receiver.ServerSecretSupplier;
import org.interledger.stream.receiver.SpspStreamConnectionGenerator;
import org.interledger.stream.receiver.StatelessStreamReceiver;
import org.interledger.stream.receiver.StreamConnectionGenerator;

import com.google.common.io.BaseEncoding;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * Unit tests for {@link ConfigurableSpspStreamConnectionGenerator}.
 */
public class ConfigurableSpspStreamConnectionGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerSecretSupplier serverSecretSupplier;
  private JavaxStreamEncryptionService streamEncryptionService;
  private StreamConnectionGenerator connectionGenerator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    // Always new and unique for each run, unless overridden in a test.
    final byte[] serverSecret = Random.randBytes(32);
    this.serverSecretSupplier = () -> serverSecret;

    this.streamEncryptionService = new JavaxStreamEncryptionService();
    this.connectionGenerator = new ConfigurableSpspStreamConnectionGenerator();
  }

  @Test
  public void fulfillsPacketsSentToJavaReceiver() throws IOException {
    serverSecretSupplier = () -> new byte[32]; // All 0's
    final InterledgerAddress ilpAddress = InterledgerAddress.of("example.connie.bob");

    // This prepare packet was taken from Quilt's IlpPacketEmitter using the shared-secret defined below to encrypt
    // the Stream frames.
    // Receiver Addr: example.connie.bob.QeJvQtFp7eRiNhnoAg9PkusR
    // SharedSecret:  nHYRcu5KM5pyw8XehssZtvhEgCgkKP4Do5kJUpk84G4
    final byte[] bytes = BaseEncoding.base16().decode(
      "0C81AC00000000000000013230323031313235323235323431303435158251C52603B11F2C1612F5D4E852CBD63137B10D76B21D02"
        + "452CE9AD1549E22B6578616D706C652E636F6E6E69652E626F622E51654A7651744670376552694E686E6F416739506B75735246FD3B"
        + "915F81129603E73BFE46E1798539372A7D8A64529E954E1612FCF126E13D605FBF8E3709D8A9AFEE49312312718AED03F11B7C46B247"
        + "5FF3A5D2364FD4AEE229B84A618B"
    );

    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    final InterledgerPreparePacket prepare = StreamCodecContextFactory.oer().read(InterledgerPreparePacket.class, bais);

    final SharedSecret sharedSecret = connectionGenerator
      .deriveSecretFromAddress(() -> serverSecretSupplier.get(), ilpAddress.with("QeJvQtFp7eRiNhnoAg9PkusR"));

    assertThat(BaseEncoding.base64().encode(sharedSecret.key()))
      .withFailMessage("Did not regenerate the same shared secret")
      .isEqualTo("nHYRcu5KM5pyw8XehssZtvhEgCgkKP4Do5kJUpk84G4=");

    final StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
      serverSecretSupplier, connectionGenerator, streamEncryptionService,
      StreamCodecContextFactory.oer()
    );

    final Denomination denomination = Denomination.builder()
      .assetScale((short) 9)
      .assetCode("ABC")
      .build();

    streamReceiver.receiveMoney(prepare, ilpAddress, denomination).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment().validateCondition(prepare.getExecutionCondition()))
        .withFailMessage("fulfillment generated does not hash to the expected condition")
        .isTrue(),
      rejectPacket -> fail()
    );
  }

  @Test
  public void fulfillsPacketsSentToJavascriptReceiver() throws IOException {

    // This was created by the JS ilp-protocol-stream library
    //let ilp_address = Address::from_str("test.peerB").unwrap();
    final InterledgerAddress ilpAddress = InterledgerAddress.of("test.peerB");

    // This prepare packet was taken from the JS implementation
    final byte[] bytes = BaseEncoding.base16().decode(
      "0C819900000000000001F43230313931303238323134313533383338F31A96346C613011947F39A0F1F4E573C2FC3E7E53797672B01D28"
        + "98E90C9A0723746573742E70656572422E4E6A584430754A504275477A353653426D4933755836682D3B6CC484C0D4E9282275D4B37"
        + "C6AE18F35B497DDBFCBCE6D9305B9451B4395C3158AA75E05BF27582A237109EC6CA0129D840DA7ABD96826C8147D0D"
    );

    final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
    final InterledgerPreparePacket prepare = StreamCodecContextFactory.oer().read(InterledgerPreparePacket.class, bais);

    serverSecretSupplier = () -> new byte[32]; // All 0's

    final SharedSecret sharedSecret = connectionGenerator
      .deriveSecretFromAddress(() -> serverSecretSupplier.get(), prepare.getDestination());

    assertThat(BaseEncoding.base16().encode(sharedSecret.key()))
      .withFailMessage("Did not regenerate the same shared secret")
      .isEqualTo("B7D09D2E16E6F83C55B60E42FCD7C2B8ED49624A1DF73C59B383DBE2E8690309");

    final StatelessStreamReceiver streamReceiver = new StatelessStreamReceiver(
      serverSecretSupplier, connectionGenerator, streamEncryptionService,
      StreamCodecContextFactory.oer()
    );

    final Denomination denomination = Denomination.builder()
      .assetScale((short) 9)
      .assetCode("ABC")
      .build();

    streamReceiver.receiveMoney(prepare, ilpAddress, denomination).handle(
      fulfillPacket -> assertThat(fulfillPacket.getFulfillment().validateCondition(prepare.getExecutionCondition()))
        .withFailMessage("fulfillment generated does not hash to the expected condition")
        .isTrue(),
      rejectPacket -> fail()
    );
  }

  @Test
  public void usingConfigurableSpspStreamConnectionGenerator() {
    testDecrypt(new ConfigurableSpspStreamConnectionGenerator());
  }

  @Test
  public void usingSpspStreamConnectionGenerator() {
    testDecrypt(new SpspStreamConnectionGenerator());
  }

  /**
   * Helper method to validate that {@code connectionGenerator} can generate a shared secret and a receiver address,
   * both of which can be used to re-derive a shared secret that can encrypt/decrypt the same data.
   */
  private void testDecrypt(final StreamConnectionGenerator connectionGenerator) {
    Objects.requireNonNull(connectionGenerator);

    final StreamConnectionDetails connectionDetails =
      connectionGenerator.generateConnectionDetails(serverSecretSupplier, InterledgerAddress.of("test.address.foo"));

    final SharedSecret derivedSharedSecret = connectionGenerator
      .deriveSecretFromAddress(() -> serverSecretSupplier.get(), connectionDetails.destinationAddress());

    // Assert that encrypt + decrypt are the same for the generated shared-secret and the derived shared secret
    final String unencrypted = "bar";
    final byte[] cipherText = streamEncryptionService.encrypt(connectionDetails.sharedSecret(), unencrypted.getBytes());

    assertThat(streamEncryptionService.decrypt(derivedSharedSecret, cipherText))
      .isEqualTo(unencrypted.getBytes());
  }
}
