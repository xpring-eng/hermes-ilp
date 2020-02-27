package org.interledger.spsp.server.util;

import java.util.Optional;

public final class OptionalAuthToken {

  public static Optional<String> of(String authToken) {
    return Optional.ofNullable(authToken);
  }
}
