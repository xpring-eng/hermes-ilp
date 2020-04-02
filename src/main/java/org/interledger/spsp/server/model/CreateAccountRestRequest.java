package org.interledger.spsp.server.model;

import org.interledger.connector.accounts.AccountId;
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
  default AccountId accountId() {
    return AccountGeneratorService.generateAccountId();
  }


  String assetCode();

  Integer assetScale();

  @Value.Default
  default String description() {
    return "";
  };

}
