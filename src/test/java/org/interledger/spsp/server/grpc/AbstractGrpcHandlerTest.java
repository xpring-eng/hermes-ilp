package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.mockito.Mockito.mock;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.spsp.server.AbstractIntegrationTest;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcMetadataReader;

import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

public abstract class AbstractGrpcHandlerTest extends AbstractIntegrationTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Autowired
  protected ConnectorAdminClient adminClient;

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Autowired
  IlpGrpcMetadataReader ilpGrpcMetadataReader;

  @Before
  public void setUp() throws IOException {
    super.setUp();
    registerGrpc();
  }

  public abstract void registerGrpc() throws IOException;
}
