package org.interledger.spsp.server.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * Utils for helping with Auth.
 */
public class AuthUtils {

  public static final String JWT_COOKIE_NAME = "jwt";
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthUtils.class);

  /**
   * If {@code authorizationHeader} is present, treat it like a Bearer token and strip off the "Bearer" prefix,
   * returning the token itself without that prefix.
   *
   * @param authorizationHeader A {@link String} representing the Authorization header as taken from an incomoing HTTP
   *                            request.
   *
   * @return An optionally-present bearer token as taken from the `Authorization` header.
   *
   * @throws BadCredentialsException if {@code authorizationHeader} doesn't begin with the prefix "Bearer " or is there
   *                                 is no token after the Bearer prefix.
   * @deprecated TODO Remove if unused.
   */
  @Deprecated
  public static String getBearerTokenFromAuthorizationHeader(final Optional<String> authorizationHeader) {
    return Objects.requireNonNull(authorizationHeader)
      .filter(authHeader -> authHeader.startsWith("Bearer "))
      .map(authHeader -> authHeader.substring(7))
      .orElseThrow(() -> new BadCredentialsException("Requests must have a valid Authorization header"));
  }

  /**
   * Obtain a JWT from the optionally-supplied {@code authorizationHeader} String.
   *
   * @param authorizationHeader A {@link String} containing an Authorization header, which should include a "Bearer "
   *                            prefix.
   *
   * @return An optionally decoded {@link DecodedJWT}.
   */
  public static Optional<DecodedJWT> getJwt(final Optional<String> authorizationHeader) {
    Objects.requireNonNull(authorizationHeader);
    try {
      final String bearerToken = getBearerTokenFromAuthorizationHeader(authorizationHeader);
      final DecodedJWT jwt = JWT.decode(bearerToken);
      if (jwt.getExpiresAt().before(new Date())) {
        throw new BadCredentialsException("JWT is expired");
      }
      return Optional.of(jwt);
    } catch (BadCredentialsException e) {
      throw e;
    } catch (Exception e) {
      if (ResponseStatusException.class.isAssignableFrom(e.getClass())) {
        throw e;
      }
      LOGGER.info("Could not decode bearer token as JWT, assuming it is SIMPLE token");
    }
    return Optional.empty();
  }
}
