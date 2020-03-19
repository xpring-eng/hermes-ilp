package org.interledger.spsp.server.model;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BearerTokenHeaderConverter implements Converter<String, Optional> {
  @Override
  public Optional convert(@NotNull String s) {
    return Optional.of(BearerToken.fromBearerTokenValue(s));
  }
}
