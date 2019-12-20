package org.interledger.spsp.server.model;

import org.interledger.connector.core.immutables.Wrapped;
import org.interledger.connector.core.immutables.Wrapper;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;
import org.immutables.value.Value;

import java.io.Serializable;

/**
 * Wrapped immutable classes for providing type-safe identifiers.
 */
public class Ids {

  /**
   * A wrapper that defines a unique identifier for an account.
   */
  @Value.Immutable(intern = true)
  @Wrapped
  @JsonSerialize(as = AccountId.class)
  @JsonDeserialize(as = AccountId.class)
  static abstract class _AccountId extends Wrapper<String> implements Serializable {

    @Override
    public String toString() {
      return this.value();
    }

    @Value.Check
    public _AccountId enforceSize() {
      Preconditions.checkArgument(
        this.value().length() < 64,
        "AccountId must not be longer than 64 characters!"
      );
      return this;
    }
  }
}
