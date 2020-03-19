package org.interledger.spsp.server.grpc.auth;

import io.grpc.Metadata;

public interface IlpGrpcMetadataReader {

  /**
   * Obtain an authorization token from the supplied {@code metadata} that can be used as a Bearer Token.
   *
   * @param metadata An instance of {@link Metadata} as supplied by gRPC.
   *
   * @return A {@link String} Bearer token that includes the "Bearer " prefix.
   */
  String authorization(Metadata metadata);
}
