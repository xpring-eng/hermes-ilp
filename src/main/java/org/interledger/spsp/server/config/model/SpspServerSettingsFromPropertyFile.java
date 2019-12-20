package org.interledger.spsp.server.config.model;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.spsp.server.model.SpspServerSettings;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * POJO class for automatic mapping of configuration properties via Spring's {@link ConfigurationProperties}
 * annotation.
 */
@ConfigurationProperties(prefix = "interledger.spsp-server")
public class SpspServerSettingsFromPropertyFile implements SpspServerSettings {

  private InterledgerAddress operatorAddress = Link.SELF;

  private boolean require32ByteSharedSecrets;

  private CryptoKeysFromPropertyFile keys;

  private ParentAccountSettingsFromPropertyFile parentAccount;

  public void setOperatorAddress(InterledgerAddress operatorAddress) {
    this.operatorAddress = operatorAddress;
  }

  @Override
  public InterledgerAddress operatorAddress() {
    return operatorAddress;
  }

  @Override
  public boolean isRequire32ByteSharedSecrets() {
    return require32ByteSharedSecrets;
  }

  public void setRequire32ByteSharedSecrets(boolean require32ByteSharedSecrets) {
    this.require32ByteSharedSecrets = require32ByteSharedSecrets;
  }

  @Override
  public CryptoKeysFromPropertyFile keys() {
    return keys;
  }

  public void setKeys(CryptoKeysFromPropertyFile cryptoKeys) {
    this.keys = cryptoKeys;
  }

  @Override
  public ParentAccountSettingsFromPropertyFile parentAccountSettings() {
    return parentAccount;
  }

  public void setParentAccount(ParentAccountSettingsFromPropertyFile parentAccount) {
    this.parentAccount = parentAccount;
  }
}
