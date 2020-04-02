package org.interledger.spsp.server.util;

import org.interledger.connector.accounts.AccountNotFoundProblem;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ResponseObserver;
import feign.FeignException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.zalando.problem.Problem;
import org.zalando.problem.ThrowableProblem;

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

  /**
   * Handle an exception by mapping it to something that gRPC can understand.
   *
   * @param exception        An {@link Exception} thrown by any code.
   * @param responseObserver A {@link ResponseObserver} that can return a response back to a gRPC client.
   */
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
    } else if (NullPointerException.class.isAssignableFrom(exception.getClass())) {
      logger.info(exception.getMessage(), exception);
      responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT));
    }
    //else if (InterruptedException.class.isAssignableFrom(exception.getClass())) {
    //else if (ExecutionException.class.isAssignableFrom(exception.getClass())) {
    else {
      logger.error(exception.getMessage(), exception);
      responseObserver.onError(new StatusRuntimeException(Status.INTERNAL));
    }
  }

  /**
   * Map a {@link FeignException} to a {@link Problem}.
   *
   * @param feignException A {@link FeignException} to map from.
   *
   * @return A {@link ThrowableProblem} to emit to the client.
   */
  public ThrowableProblem mapToProblem(final FeignException feignException) {
    try {
      return objectMapper.readValue(new String(feignException.content()), ThrowableProblem.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
