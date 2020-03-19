package org.interledger.spsp.server.client;

import feign.Param;

import java.util.Objects;
import java.util.Optional;

public class OptionalBearerTokenExpander implements Param.Expander {

  @Override
  public String expand(Object value) {
      if (value != null && Optional.class.isAssignableFrom(value.getClass())) {
        Optional<?> optionalToken = (Optional) value;
        return optionalToken.map(Object::toString).orElse("");
      } else {
        return "";
      }
  }
}
