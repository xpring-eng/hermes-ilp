package org.interledger.spsp.server.client;

import org.interledger.spsp.server.services.AccountGeneratorService;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableCreateAccountRestRequest.class)
public interface CreateAccountRestRequest {

  static ImmutableCreateAccountRestRequest.Builder builder(String assetCode, Integer assetScale) {
    return ImmutableCreateAccountRestRequest.builder().assetCode(assetCode).assetScale(assetScale);
  }

  @Value.Default
  default String accountId() {
    return AccountGeneratorService.generateAccountId();
  }


  String assetCode();

  Integer assetScale();

  @Value.Default
  default String description() {
    return "";
  };

}
