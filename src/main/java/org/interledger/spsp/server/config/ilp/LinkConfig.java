package org.interledger.spsp.server.config.ilp;

import static org.interledger.spsp.server.config.ilp.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.Link;
import org.interledger.link.LinkFactoryProvider;
import org.interledger.link.LinkId;
import org.interledger.link.LoopbackLink;
import org.interledger.link.LoopbackLinkFactory;
import org.interledger.link.PacketRejector;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.PingLoopbackLinkFactory;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkFactory;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.spsp.server.model.SpspServerSettings;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.function.Supplier;

@Configuration
@Import({IlpOverHttpConfig.class})
public class LinkConfig {

  @Autowired
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttpClient ilpOverHttpClient;

  @Autowired
  private SpspServerSettings spspServerSettings;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private Decryptor decryptor;

  @Autowired
  private LinkFactoryProvider linkFactoryProvider;

  /**
   * The link to the parent connector that this SpspServer is a child of.
   *
   * @return A {@link Link}.
   */
  @Bean
  protected Link parentLink() {
    final IlpOverHttpLinkSettings linkSettings = IlpOverHttpLinkSettings
      .fromCustomSettings(spspServerSettings.parentAccountSettings().customSettings())
      .build();

    final Link<?> parentLink = linkFactoryProvider.getLinkFactory(IlpOverHttpLink.LINK_TYPE)
      .constructLink(() -> spspServerSettings.operatorAddress(), linkSettings);

    parentLink.setLinkId(LinkId.of("parentConnector"));

    return parentLink;
  }

  @Bean
  protected LoopbackLinkFactory loopbackLinkFactory(PacketRejector packetRejector) {
    return new LoopbackLinkFactory(packetRejector);
  }

  @Bean
  protected PingLoopbackLinkFactory unidirectionalPingLinkFactory() {
    return new PingLoopbackLinkFactory();
  }

  @Bean
  protected LinkFactoryProvider linkFactoryProvider(
    final LoopbackLinkFactory loopbackLinkFactory, final PingLoopbackLinkFactory pingLoopbackLinkFactory
  ) {
    final LinkFactoryProvider provider = new LinkFactoryProvider();

    // Register known types...Spring will register proper known types based upon config...
    provider.registerLinkFactory(LoopbackLink.LINK_TYPE, loopbackLinkFactory);
    provider.registerLinkFactory(PingLoopbackLink.LINK_TYPE, pingLoopbackLinkFactory);

    // The value passed-in here as `encryptedConnectorPropertyStringBytes` will actually be an encrypted property as
    // encrypted via connector-crypto-cli. For testing purposes, reference the SpspServer properties for a given
    // account.
    org.interledger.link.http.auth.Decryptor linkDecryptor = encryptedConnectorPropertyStringBytes -> decryptor.decrypt(
      EncryptedSecret.fromEncodedValue(new String(encryptedConnectorPropertyStringBytes))
    );

    provider.registerLinkFactory(
      IlpOverHttpLink.LINK_TYPE,
      new IlpOverHttpLinkFactory(
        ilpOverHttpClient, linkDecryptor, objectMapper, InterledgerCodecContextFactory.oer()
      )
    );

    return provider;
  }

  @Bean
  protected PacketRejector packetRejector(final Supplier<SpspServerSettings> spspServerSettingsSupplier) {
    return new PacketRejector(() -> spspServerSettingsSupplier.get().operatorAddress());
  }
}
