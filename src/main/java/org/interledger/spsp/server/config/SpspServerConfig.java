package org.interledger.spsp.server.config;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.spsp.server.config.crypto.CryptoConfig;
import org.interledger.spsp.server.config.ildcp.IldcpConfig;
import org.interledger.spsp.server.config.ilp.LinkConfig;
import org.interledger.spsp.server.config.ilp.StreamConfig;
import org.interledger.spsp.server.config.jackson.JacksonConfig;
import org.interledger.spsp.server.config.model.SpspServerSettingsFromPropertyFile;
import org.interledger.spsp.server.config.web.SpringSpspServerWebMvc;
import org.interledger.spsp.server.model.SpspServerSettings;
import org.interledger.stream.crypto.Random;
import org.interledger.stream.receiver.ServerSecretSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Base64;
import java.util.function.Supplier;

@Configuration
@EnableConfigurationProperties({SpspServerSettingsFromPropertyFile.class})
@Import({
  JacksonConfig.class,
  CryptoConfig.class,
  LinkConfig.class,
  StreamConfig.class,
  SpringSpspServerWebMvc.class,
  IldcpConfig.class,
})
public class SpspServerConfig {

  @Value("${interledger.spsp-server.server-secret}")
  private String serverSecretB64;

  @Autowired
  private ApplicationContext applicationContext;

  /**
   * <p>This is a supplier that can be given to beans for later usage after the application has started. This
   * supplier will not resolve to anything until the `SpspServerSettings` bean has been loaded into the
   * application-context, which occurs via the EnableConfigurationProperties annotation on this class.
   * </p>
   */
  @Bean
  protected Supplier<SpspServerSettings> spspServerSettingsSupplier() {
    return () -> applicationContext.getBean(SpspServerSettings.class);
  }

  @Bean
  protected Supplier<InterledgerAddress> operatorAddress() {
    return () -> Link.SELF;
  }

  @Bean
  protected ServerSecretSupplier serverSecretSupplier() {
    final byte[] serverSecret;
    if (serverSecretB64 != null) {
      serverSecret = Base64.getDecoder().decode(serverSecretB64);
    } else {
      // if `interledger.spspServer.serverSecret` is not specified, this value will be regenerated on every server
      // restart.
      serverSecret = Random.randBytes(32);
    }
    return () -> serverSecret;
  }

}
