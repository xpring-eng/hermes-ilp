package org.interledger.spsp.server.grpc;

import static org.junit.Assert.assertEquals;

import org.interledger.spsp.server.HermesServerApplication;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
    classes = {HermesServerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GetAccountGrpcTest {
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  @Autowired
  AccountServiceGrpc accountServiceGrpc;

  /**
   * To test the server, make calls with a real stub using the in-process channel, and verify
   * behaviors or state changes from the client side.
   */
  @Test
  public void getAccountFor_connie() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
        .forName(serverName).directExecutor().addService(accountServiceGrpc).build().start());

    IlpServiceGrpc.IlpServiceBlockingStub blockingStub = IlpServiceGrpc.newBlockingStub(
        // Create a client channel and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


    GetAccountResponse reply =
        blockingStub.getAccount(GetAccountRequest.newBuilder().setAccountId("connie").build());

    System.out.println(reply);
    assertEquals(reply.getAccountId(), "connie");
    assertEquals(reply.getAssetScale(), 9);
    assertEquals(reply.getAssetCode(), "XRP");
    assertEquals(reply.getPaymentPointer(), "$jc.ilpv4.dev/connie");
  }
}


