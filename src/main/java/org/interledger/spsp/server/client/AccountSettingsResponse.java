package org.interledger.spsp.server.client;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableAccountSettingsResponse.class)
@JsonDeserialize(as = ImmutableAccountSettingsResponse.class)
public interface AccountSettingsResponse extends AccountSettings {

  static ImmutableAccountSettingsResponse.Builder builder() {
    return ImmutableAccountSettingsResponse.builder();
  }

  PaymentPointer paymentPointer();

  @Value.Immutable
  @Value.Modifiable
  @JsonSerialize(as = ImmutableAccountSettingsResponse.class)
  @JsonDeserialize(as = ImmutableAccountSettingsResponse.class)
  abstract class AbstractAccountSettingsResponse implements AccountSettingsResponse {

    @Override
    public abstract PaymentPointer paymentPointer();
  }
}
