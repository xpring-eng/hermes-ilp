package org.interledger.spsp.server.controllers;

import org.interledger.quilt.jackson.InterledgerModule;
import org.interledger.quilt.jackson.conditions.Encoding;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.InvalidReceiverClientException;
import org.interledger.spsp.client.SpspClientException;
import org.interledger.spsp.client.rust.InterledgerRustNodeClient;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableMap;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.Objects;

/**
 * @see "https://github.com/hyperledger/quilt/issues/380"
 * @see "https://github.com/hyperledger/quilt/issues/381"
 * @deprecated This should be removed once `https://github.com/hyperledger/quilt/issues/380` is fixed. That issue will
 *   allow Quilt to provide a generic SPSP client that can be used in this test, meaning this class can be deleted.
 */
@Deprecated
public class SpspClient extends InterledgerRustNodeClient {

  private final OkHttpClient httpClient;
  private final String authToken;
  private final ObjectMapper objectMapper;

  public SpspClient(OkHttpClient okHttpClient, String authToken, String baseUri) {
    super(okHttpClient, authToken, baseUri);
    this.authToken = Objects.requireNonNull(authToken);
    this.httpClient = okHttpClient;
    this.objectMapper = mapper();
  }

  public SpspClient(
    OkHttpClient okHttpClient, String authToken, String baseUri, PaymentPointerResolver paymentPointerResolver
  ) {
    super(okHttpClient, authToken, baseUri, paymentPointerResolver);
    this.authToken = Objects.requireNonNull(authToken);
    this.httpClient = Objects.requireNonNull(okHttpClient);
    this.objectMapper = mapper();
  }

  private static ObjectMapper mapper() {
    final ObjectMapper objectMapper = JsonMapper.builder()
      .serializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(JsonWriteFeature.WRITE_NUMBERS_AS_STRINGS, false)
      .build()
      .registerModule(new Jdk8Module())
      .registerModule(new InterledgerModule(Encoding.BASE64));
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    objectMapper.configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true);
    return objectMapper;
  }

  @Override
  public StreamConnectionDetails getStreamConnectionDetails(PaymentPointer paymentPointer)
    throws InvalidReceiverClientException {
    return super.getStreamConnectionDetails(paymentPointer);
  }

  public StreamConnectionDetails getStreamConnectionDetails(
    final HttpUrl spspUrl
  ) throws InvalidReceiverClientException {
    Objects.requireNonNull(spspUrl);
    return execute(
      requestBuilder().url(spspUrl).headers(Headers.of(
        ImmutableMap.of("Authorization", "Bearer " + authToken, "Accept", ACCEPT_SPSP_JSON)))
        .get().build(),
      StreamConnectionDetails.class);
  }

  private <T> T execute(Request request, Class<T> clazz) throws SpspClientException {
    try (Response response = httpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        if (response.code() == 404) {
          throw new InvalidReceiverClientException(request.url().toString());
        }
        throw new SpspClientException("Received non-successful HTTP response code " + response.code()
          + " calling " + request.url());
      }
      return objectMapper.readValue(response.body().string(), clazz);
    } catch (IOException e) {
      throw new SpspClientException("IOException failure calling " + request.url(), e);
    }
  }

  private Request.Builder requestBuilder() {
    return new Request.Builder()
      .headers(Headers.of(ImmutableMap.of("Authorization", "Bearer " + authToken)));
  }
}
