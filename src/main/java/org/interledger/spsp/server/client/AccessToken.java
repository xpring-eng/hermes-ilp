package org.interledger.spsp.server.client;

import org.interledger.connector.accounts.AccountId;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.time.Instant;

@Value.Immutable
@JsonDeserialize(as = ImmutableAccessToken.class)
@JsonSerialize(as = ImmutableAccessToken.class)
public interface AccessToken {

  static ImmutableAccessToken.Builder builder() {
    return ImmutableAccessToken.builder();
  }

  Long id();

  AccountId accountId();

  Instant createdAt();

}