package org.interledger.spsp.server.client;


import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.optionals.OptionalDecoder;
import okhttp3.HttpUrl;
import org.zalando.problem.ThrowableProblem;

import java.util.List;
import java.util.Objects;

public interface ConnectorTokensClient {
  String ACCEPT = "Accept:";
  String APPLICATION_JSON = "application/json";

  /**
   * Static constructor to build a new instance of this Connector Balance client.
   *
   * @param connectorHttpUrl The {@link HttpUrl} of the Connector.
   * @return A {@link ConnectorTokensClient}.
   */
  static ConnectorTokensClient construct(final HttpUrl connectorHttpUrl) {
    Objects.requireNonNull(connectorHttpUrl);
    final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    return Feign.builder()
      .encoder(new JacksonEncoder(objectMapper))
      .decode404()
      .decoder(new OptionalDecoder(new JacksonDecoder(objectMapper)))
      .target(ConnectorTokensClient.class, connectorHttpUrl.toString());
  }

  @RequestLine("POST /accounts/{accountId}/tokens")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    "Authorization: {authorizationHeader}"
  })
  CreateAccessTokenResponse createToken(@Param("authorizationHeader") String authorizationHeader,
                                        @Param("accountId") AccountId accountId) throws ThrowableProblem;


  @RequestLine("DELETE /accounts/{accountId}/tokens")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    "Authorization: {authorizationHeader}"
  })
  void deleteTokens(@Param("authorizationHeader") String authorizationHeader,
                    @Param("accountId") AccountId accountId) throws ThrowableProblem;

  @RequestLine("GET /accounts/{accountId}/tokens")
  @Headers( {
    ACCEPT + APPLICATION_JSON,
    "Authorization: {authorizationHeader}"
  })
  List<AccessToken> getTokens(@Param("authorizationHeader") String authorizationHeader,
                              @Param("accountId") AccountId accountId) throws ThrowableProblem;

}
