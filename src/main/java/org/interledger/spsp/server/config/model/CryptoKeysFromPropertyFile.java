package org.interledger.spsp.server.config.model;


import org.interledger.connector.settings.ConnectorKey;
import org.interledger.connector.settings.ConnectorKeys;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * An implementation of {@link ConnectorKeys} that can be used by Spring Boot to load these properties from a YAML
 * file.
 */
@SuppressWarnings("unused")
public class CryptoKeysFromPropertyFile implements ConnectorKeys {

  private CryptoKeyFromPropertyFile secret0;

  private CryptoKeyFromPropertyFile accountSettings;

  @Override
  public ConnectorKey secret0() {
    return secret0;
  }

  public void setSecret0(CryptoKeyFromPropertyFile secret0) {
    this.secret0 = secret0;
  }

  @Override
  public ConnectorKey accountSettings() {
    return accountSettings;
  }

  public void setAccountSettings(CryptoKeyFromPropertyFile accountSettings) {
    this.accountSettings = accountSettings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ConnectorKeys)) {
      return false;
    }
    ConnectorKeys that = (ConnectorKeys) o;
    return secret0.equals(that.secret0()) &&
      accountSettings.equals(that.accountSettings());
  }

  @Override
  public int hashCode() {
    return Objects.hash(secret0, accountSettings);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", CryptoKeysFromPropertyFile.class.getSimpleName() + "[", "]")
      .add("secret0=" + secret0)
      .add("accountSettings=" + accountSettings)
      .toString();
  }
}
