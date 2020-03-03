package org.interledger.spsp.server.client;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@JsonSerialize(as = ImmutableAccountSettingsResponse.class)
@JsonDeserialize(as = ImmutableAccountSettingsResponse.class)
public abstract class AccountSettingsResponse extends AccountSettings.AbstractAccountSettings {

  public static ImmutableAccountSettingsResponse.Builder builder() {
    return ImmutableAccountSettingsResponse.builder();
  }

  public abstract PaymentPointer paymentPointer();
}
