package org.interledger.spsp.server.model;

import org.interledger.spsp.server.model.ImmutableBearerToken.Builder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.immutables.value.Value;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * A strongly types class that holds a Bearer token conforming to RFC-6750.
 *
 * @see "https://tools.ietf.org/html/rfc6750#section-6.1.1"
 */
@Immutable
@JsonSerialize(as = ImmutableBearerToken.class, using = ToStringSerializer.class)
@JsonDeserialize(as = ImmutableBearerToken.class)
public abstract class BearerToken {

  public static final String BEARER_SPACE = "Bearer ";

  public static BearerToken fromRawToken(final String rawToken) {
    if(rawToken == null){
      throw new BadCredentialsException("Bearer tokens must not be null");
    }
    return builder().value(BEARER_SPACE + rawToken).build();
  }

  public static BearerToken fromBearerTokenValue(final String bearerTokenValue) {
    if(bearerTokenValue == null){
      throw new BadCredentialsException("Bearer tokens must not be null");
    }
    return builder().value(bearerTokenValue).build();
  }

  public static Builder builder() {
    return ImmutableBearerToken.builder();
  }

  /**
   * The value of this bearer token, including the "Bearer " prefix.
   *
   * @return A {@link String} containing this token's value.
   */
  public abstract String value();

  /**
   * The raw token. E.g., if an Authorization header has a value of "Bearer foo", then the string returned from this
   * method would be "foo".
   *
   * @return The underlying auth token, without the "Bearer " prefix.
   */
  @Derived
  public String rawToken() {
    return value() != null && value().length() > 7 ? value().substring(7) : "";
  }

  /**
   * Returns the token in the following format: "Bearer " + {rawToken}.
   */
  @Override
  public String toString() {
    return BEARER_SPACE + rawToken();
  }

  @Value.Check
  public BearerToken normalize() {
    if (!this.value().startsWith(BEARER_SPACE)) {
      throw new BadCredentialsException("BearerTokens must start with the prefix \"Bearer \"");
    }

    if (this.rawToken().length() <= 0) {
      throw new BadCredentialsException("BearerTokens must have a non-empty raw token");
    }

    return this;
  }
}
