package org.interledger.spsp.server.grpc.auth;

import com.google.common.net.HttpHeaders;
import io.grpc.Metadata;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class IlpGrpcMetadataReaderImpl implements IlpGrpcMetadataReader {

  public static final String JWT_COOKIE_NAME = "jwt";

  @Override
  public String authorization(Metadata metadata) {
    Optional<String> cookieHeaders =
      Optional.ofNullable(metadata.get(Metadata.Key.of(HttpHeaders.COOKIE, Metadata.ASCII_STRING_MARSHALLER)));


    return cookieHeaders
      .map(c -> {
        // There are cookies
        return Arrays.stream(c.split(";")) // c will be one string with cookies delimited by ";"
          .map(HttpCookie::parse) // This yields a list of HttpCookies
          .flatMap(Collection::stream) // Just flatten the list of lists
          .filter($ -> $.getName().equals(JWT_COOKIE_NAME)) // JWT cookie exists?
          .findFirst()
          .map(jwtToken -> { return "Bearer " + jwtToken.getValue(); }) // if JWT cookie exists, return "Bearer " + the value
          .orElse(metadata.get(Metadata.Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER))); // Otherwise get jwt from auth header
      })
      // No cookies, so try to get it from the auth header
      .orElse(metadata.get(Metadata.Key.of(HttpHeaders.AUTHORIZATION, Metadata.ASCII_STRING_MARSHALLER)));
  }
}
