package org.interledger.spsp.server.config.crypto;

import static org.interledger.connector.core.ConfigConstants.ENABLED;
import static org.interledger.connector.core.ConfigConstants.TRUE;
import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.INTERLEDGER_HERMES_KEYSTORE_GCP;
import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.INTERLEDGER_HERMES_KEYSTORE_LOCATION_ID;

import org.interledger.crypto.EncryptionService;
import org.interledger.crypto.impl.GcpEncryptionService;

import com.google.api.gax.core.CredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = INTERLEDGER_HERMES_KEYSTORE_GCP, name = ENABLED, havingValue = TRUE)
public class GcpCryptoConfig {

  @Value("${" + INTERLEDGER_HERMES_KEYSTORE_LOCATION_ID + "}")
  private String gcpLocationId;

  @Bean
  EncryptionService encryptionService(GcpProjectIdProvider gcpProjectIdProvider,
                                      CredentialsProvider credentialsProvider) {
    return new GcpEncryptionService(gcpProjectIdProvider.getProjectId(), gcpLocationId, credentialsProvider);
  }

}
