package org.interledger.spsp.server.util;

import java.util.Objects;
import java.util.Optional;

public final class OptionalAuthToken {

  public static Optional<String> of(Optional<String> authToken) {
    Objects.requireNonNull(authToken);
    return authToken.map(t -> t.substring(t.indexOf(" ") + 1));
  }

  public static Optional<String> of(String authToken) {
    if (authToken != null) {
      authToken = authToken.substring(authToken.indexOf(" ") + 1);
      return of(Optional.ofNullable(authToken.isEmpty() ? null : authToken));
    }

    return of(Optional.empty());
  }
}
