package org.interledger.spsp.server.model;


import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BearerTokenHeaderConverter implements Converter<String, Optional> {

  @Override
  public Optional convert(final String s) {
    return Optional.of(BearerToken.fromBearerTokenValue(s));
  }
}
