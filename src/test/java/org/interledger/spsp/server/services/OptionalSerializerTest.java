package org.interledger.spsp.server.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;
import org.interledger.spsp.server.model.BearerToken;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Optional;

public class OptionalSerializerTest {

  private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();

  @Test
  public void serializeOptionalBearerToken() throws JsonProcessingException {
    Optional<BearerToken> optionalBearerToken = Optional.of(BearerToken.fromBearerTokenValue("Bearer password"));
    String json = objectMapper.writeValueAsString(optionalBearerToken);
    TypeReference<Optional<BearerToken>> typeReference = new TypeReference<Optional<BearerToken>>() {};

    Optional<BearerToken> actual = objectMapper.readValue(json, typeReference);
    assertThat(actual).isEqualTo(optionalBearerToken);
//    assertThat(json).isEqualTo("Bearer password");
  }
}
