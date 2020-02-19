package org.interledger.spsp.server.model;

import org.interledger.spsp.server.services.AccountGeneratorService;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableCreateAccountRestRequest.class)
public interface CreateAccountRestRequest {

  static ImmutableCreateAccountRestRequest.Builder builder() {
    return ImmutableCreateAccountRestRequest.builder();
  }

  @Value.Default
  default String accountId() {
    return AccountGeneratorService.generateAccountId();
  };

  @Value.Default
  default String assetCode() {
    return "XRP";
  };

  @Value.Default
  default int assetScale() {
    return 9;
  };

  @Value.Default
  default String description() {
    return "";
  };

}
