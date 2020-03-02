package org.interledger.spsp.server.config.ilp;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;

import com.google.common.base.Preconditions;

import java.util.Base64;

public class ConnectorAdminAuthProvider {

  private final String adminPasswordConfig;
  private final EncryptionService encryptionService;

  public ConnectorAdminAuthProvider(String adminPasswordConfig, EncryptionService encryptionService) {
    this.adminPasswordConfig = Preconditions.checkNotNull(adminPasswordConfig);
    this.encryptionService = encryptionService;
  }

  public String getAdminAuth() {
    if (adminPasswordConfig != null && adminPasswordConfig.startsWith("enc")) {
      return "Basic " + Base64.getEncoder().encodeToString(
        encryptionService.decrypt(EncryptedSecret.fromEncodedValue(adminPasswordConfig))
      );
    } else {
      return "Basic " + Base64.getEncoder().encodeToString(adminPasswordConfig.getBytes());
    }
  }

}
