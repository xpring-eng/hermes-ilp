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

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthUtils.class);

  /**
   * Return an optional bearer token from the incoming {@code request}.
   *
   * @param authorizationHeader A {@link String} representing the Authorization header as taken from an incomoing HTTP
   *                            request.
   *
   * @return An optionally-present bearer token as taken from the `Authorization` header.
   */
  public static String getAuthorizationAsBearerToken(final Optional<String> authorizationHeader) {
    return Objects.requireNonNull(authorizationHeader)
      .filter(authHeader -> authHeader.length() > 7)
      .filter(authHeader -> authHeader.startsWith("Bearer "))
      .map(authHeader -> authHeader.substring(7))
      .orElseThrow(() -> new BadCredentialsException("Requests must have a valid Authorization header"));
  }

  public static Optional<DecodedJWT> getJwt(final Optional<String> authorizationHeader) {
    Objects.requireNonNull(authorizationHeader);
    try {
      final String bearerToken = getAuthorizationAsBearerToken(authorizationHeader);
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
