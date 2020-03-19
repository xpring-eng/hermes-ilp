package org.interledger.spsp.server.grpc.auth;

import org.interledger.spsp.server.model.BearerToken;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class IlpCallCredentials extends CallCredentials {
  private static Logger LOGGER = LoggerFactory.getLogger(IlpCallCredentials.class);

  private final BearerToken jwtToken;

  private IlpCallCredentials(final BearerToken jwtToken) {
    this.jwtToken = jwtToken;
  }

  public static IlpCallCredentials build(BearerToken token) {
    return new IlpCallCredentials(token);
  }

  @Override
  public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
    try {
      applyToken(applier, jwtToken);
    } catch (RuntimeException e) {
      applyFailure(applier, e);
    }
  }

  @Override
  public void thisUsesUnstableApi() {}

  protected void applyToken(MetadataApplier applier, BearerToken jwtToken) {
    Metadata metadata = new Metadata();
    metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), jwtToken.toString());
    applier.apply(metadata);
  }

  protected void applyFailure(MetadataApplier applier, Throwable e) {
    String msg = "An exception when obtaining JWT token";
    LOGGER.error(msg, e);
    applier.fail(Status.UNAUTHENTICATED.withDescription(msg).withCause(e));
  }
}
