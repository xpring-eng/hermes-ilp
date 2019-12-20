package org.interledger.spsp.server.model;

import org.interledger.connector.settings.ConnectorKey;
import org.interledger.connector.settings.ConnectorKeys;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.spsp.server.model.ImmutableSpspServerSettings.Builder;

import org.immutables.value.Value;
import org.immutables.value.Value.Modifiable;

/**
 * A view of the settings currently configured for this Connector.
 */
@Value.Immutable(intern = true)
@Modifiable
public interface SpspServerSettings {

  static Builder builder() {
    return ImmutableSpspServerSettings.builder();
  }

  /**
   * The ILP Address of this connector. Note that the Connector's initial properties may not specify an address, in
   * which case the default will be {@link Link#SELF}. In this case the Connector will use IL-DCP to obtain its
   * operating address.
   *
   * @return The ILP address of this connector.
   */
  @Value.Default
  default InterledgerAddress operatorAddress() {
    return Link.SELF;
  }

  /**
   * Flag to control if shared secrets must be 32 bytes
   *
   * @return true if required otherwise anything goes
   */
  @Value.Default
  default boolean isRequire32ByteSharedSecrets() {
    return false;
  }

  /**
   * Keys the connector will use for various core functions.
   *
   * @return keys
   */
  @Value.Default
  default ConnectorKeys keys() {
    return ConnectorKeys.builder()
      .accountSettings(ConnectorKey.builder().alias("accountSettings").version("1").build())
      .secret0(ConnectorKey.builder().alias("secret0").version("1").build())
      .build();
  }

  /**
   * The account settings for the parent account of this Receiver.
   *
   * @return A {@link ParentAccountSettings}.
   */
  ParentAccountSettings parentAccountSettings();

}
