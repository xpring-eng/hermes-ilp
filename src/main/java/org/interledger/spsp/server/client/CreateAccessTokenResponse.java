package org.interledger.spsp.server.client;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@JsonDeserialize(as = ImmutableCreateAccessTokenResponse.class)
@JsonSerialize(as = ImmutableCreateAccessTokenResponse.class)
public interface CreateAccessTokenResponse {

  static ImmutableCreateAccessTokenResponse.Builder builder() {
    return ImmutableCreateAccessTokenResponse.builder();
  }

  Long id();

  AccountId accountId();

  /**
   * access token.
   * @return
   */
  String rawToken();

  Instant createdAt();

}