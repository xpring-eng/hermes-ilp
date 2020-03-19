package org.interledger.spsp.server.grpc;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;
import org.interledger.spsp.server.util.ExceptionHandlerUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.gax.rpc.ResponseObserver;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.testcontainers.shaded.org.apache.commons.lang.ObjectUtils;

import java.util.Map;

public class ExceptionHandlerUtilsTest {

  /**
   * Will be removed once the Connector responds properly with a 401 exception (currently it reports a 404).
   *
   * @see "https://github.com/interledger4j/ilpv4-connector/issues/622"
   */
  @Deprecated
  private static final String NOT_FOUND_MESSAGE = "Account not found for principal:";

  @Mock
  private StreamObserver responseObserverMock;

  private ObjectMapper objectMapper;
  private ExceptionHandlerUtils exceptionHandlerUtils;
  private Request request;

  @Before
  public void setUp() {
    initMocks(this);
    this.objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
    this.exceptionHandlerUtils = new ExceptionHandlerUtils(this.objectMapper);
    request = Request.create(mock(Request.HttpMethod.class), "https://ughfeign.com", mock(Map.class), null, mock(RequestTemplate.class));
  }

  @Test
  public void feignException401Unauthenticated() {

    Response response = Response.builder()
      .body("{\"detail\": \"thou shall not pass!\"}".getBytes())
      .status(401)
      .request(request)
      .build();
    FeignException exception = FeignException.errorStatus("doSomething", response);

    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.UNAUTHENTICATED);
  }

  @Test
  public void feignException401AccountNotFound() {
    Response response = Response.builder()
      .body(String.format("{\"detail\": \"%s\"}", NOT_FOUND_MESSAGE).getBytes())
      .status(401)
      .request(request)
      .build();
    FeignException exception = FeignException.errorStatus("doSomething", response);
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.NOT_FOUND);
  }

  @Test
  public void feignException404() {
    Response response = Response.builder()
      .body(String.format("{\"detail\": \"%s\"}", NOT_FOUND_MESSAGE).getBytes())
      .status(404)
      .request(request)
      .build();
    FeignException exception = FeignException.errorStatus("doSomething", response);
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.NOT_FOUND);
  }

  @Test
  public void feignException409() {
    Response response = Response.builder()
      .body(String.format("{\"detail\": \"%s\"}", NOT_FOUND_MESSAGE).getBytes())
      .status(409)
      .request(request)
      .build();
    FeignException exception = FeignException.errorStatus("doSomething", response);
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.ALREADY_EXISTS);
  }

  @Test
  public void feignExceptionOther() {
    Response response = Response.builder()
      .body(String.format("{\"detail\": \"%s\"}", NOT_FOUND_MESSAGE).getBytes())
      .status(500)
      .request(request)
      .build();
    FeignException exception = FeignException.errorStatus("doSomething", response);
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.INTERNAL);
  }

  @Test
  public void badCredentialsException() {
    BadCredentialsException exception = new BadCredentialsException("thow shall not pass");
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.UNAUTHENTICATED);
  }

  @Test
  public void accountNotFoundProblem() {
    AccountNotFoundProblem exception = new AccountNotFoundProblem(AccountId.of("foo"));
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.NOT_FOUND);
  }

  @Test
  public void nullPointerException() {
    NullPointerException exception = new NullPointerException();
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.INVALID_ARGUMENT);
  }

  @Test
  public void unspecifiedException() {
    Exception exception = new Exception();
    exceptionHandlerUtils.handleException(exception, responseObserverMock);
    verifyStatusRuntimeException(Status.INTERNAL);
  }

  private void verifyStatusRuntimeException(Status expectedStatus) {
    verify(responseObserverMock, times(1))
      .onError(argThat(new StatusRuntimeExceptionMatcher(new StatusRuntimeException(expectedStatus))));
    verifyNoMoreInteractions(responseObserverMock);
  }

  private class StatusRuntimeExceptionMatcher implements ArgumentMatcher<StatusRuntimeException> {
    private StatusRuntimeException left;

    public StatusRuntimeExceptionMatcher(StatusRuntimeException left) {
      this.left = left;
    }

    @Override
    public boolean matches(StatusRuntimeException right) {
      return left.getStatus().equals(right.getStatus());
    }
  }
}
