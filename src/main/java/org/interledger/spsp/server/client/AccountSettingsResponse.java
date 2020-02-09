package org.interledger.spsp.server.client;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.PaymentPointer;

import org.immutables.value.Value;

@Value.Immutable
public interface AccountSettingsResponse extends AccountSettings {

  static ImmutableAccountSettingsResponse.Builder builder() {
    return ImmutableAccountSettingsResponse.builder();
  }

  PaymentPointer paymentPointer();

}
