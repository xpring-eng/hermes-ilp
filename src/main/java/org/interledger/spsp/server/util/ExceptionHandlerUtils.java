package org.interledger.spsp.server.util;

import org.interledger.connector.accounts.AccountNotFoundProblem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExceptionHandlerUtils {

  /**
   * Will be removed once the Connector responds properly with a 401 exception (currently it reports a 404).
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/622"
   */
  @Deprecated
  private static final String NOT_FOUND_MESSAGE = "Account not found for principal:";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ObjectMapper objectMapper;

  public ExceptionHandlerUtils(final ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  public void handleException(final Exception exception, final StreamObserver<?> responseObserver) {
    Objects.requireNonNull(exception);
    Objects.requireNonNull(responseObserver);

    if (FeignException.class.isAssignableFrom(exception.getClass())) {
      Status exceptionStatus;
      final FeignException feignException = (FeignException) exception;
      switch (feignException.status()) {
        case 401:
          try {
            Map<String, String> exceptionBody = objectMapper
              .readValue(new String(feignException.content()), HashMap.class);

            exceptionStatus = exceptionBody.getOrDefault("detail", "")
              .contains(NOT_FOUND_MESSAGE) ? Status.NOT_FOUND : Status.UNAUTHENTICATED;
          } catch (JsonProcessingException ex) {
            exceptionStatus = Status.INTERNAL;
          }
          logger.warn(feignException.getMessage(), feignException);
          break;
        case 404:
          exceptionStatus = Status.NOT_FOUND;
          logger.info(feignException.getMessage(), feignException);
          break;
        case 409:
          exceptionStatus = Status.ALREADY_EXISTS;
          logger.info(feignException.getMessage(), feignException);
          break;
        default:
          exceptionStatus = Status.INTERNAL;
          logger.error(feignException.getMessage(), feignException);
          break;
      }
      responseObserver.onError(new StatusRuntimeException(exceptionStatus));
    } else if (BadCredentialsException.class.isAssignableFrom(exception.getClass())) {
      logger.warn(exception.getMessage(), exception);
      responseObserver.onError(new StatusRuntimeException(Status.UNAUTHENTICATED));
    } else if (AccountNotFoundProblem.class.isAssignableFrom(exception.getClass())) {
      logger.info(exception.getMessage(), exception);
      responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
    }
    //else if (InterruptedException.class.isAssignableFrom(exception.getClass())) {
    //else if (ExecutionException.class.isAssignableFrom(exception.getClass())) {
    else {
      logger.error(exception.getMessage(), exception);
      responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
    }
  }
}
