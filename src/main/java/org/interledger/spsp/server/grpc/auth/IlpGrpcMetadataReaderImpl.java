package org.interledger.spsp.server.grpc.auth;

import static org.interledger.spsp.server.services.AuthUtils.JWT_COOKIE_NAME;

import org.interledger.spsp.server.model.BearerToken;

import com.google.common.net.HttpHeaders;
import io.grpc.Metadata;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class IlpGrpcMetadataReaderImpl implements IlpGrpcMetadataReader {

  @Override
  public String authorization(Metadata metadata) {
    Optional<String> cookieHeaders =
      Optional.ofNullable(metadata.get(Metadata.Key.of(HttpHeaders.COOKIE, Metadata.ASCII_STRING_MARSHALLER)));

    Optional<String> authorizationHeader =
      Optional.ofNullable(metadata.get(Metadata.Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER)));

    // If the "Authorization" header exists, use it. Otherwise, see if we can find a jwt Cookie. If that doesn't exist,
    // return null so the AccountGrpcHandler can generate credentials
    return authorizationHeader.orElseGet(() -> cookieHeaders
      .map(c -> {
        // There are cookies
        return Arrays.stream(c.split(";")) // c will be one string with cookies delimited by ";"
          .map(HttpCookie::parse) // This yields a list of HttpCookies
          .flatMap(Collection::stream) // Just flatten the list of lists
          .filter($ -> $.getName().equals(JWT_COOKIE_NAME)) // JWT cookie exists?
          .findFirst()
          .map(HttpCookie::getValue) // if JWT cookie exists, return the value
          .orElse(null);
      })
      // Append the "Bearer " prefix because the JWT coming from a Cookie has no such prefix.
      .map(BearerToken::fromRawToken)
      .map(BearerToken::value)
      // No cookies and no Authorization header, so just return null. This will signal to AccountGrpcHandler to
      // generate new credentials
      .orElse(null));
  }
}
