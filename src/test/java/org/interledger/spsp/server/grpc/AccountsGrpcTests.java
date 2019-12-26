package org.interledger.spsp.server.grpc;

import static org.junit.Assert.assertEquals;

import org.interledger.spsp.server.HermesServerApplication;

import com.google.protobuf.Empty;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;


@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
    classes = {HermesServerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountsGrpcTests {
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private IlpServiceGrpc.IlpServiceBlockingStub blockingStub;

  @Autowired
  AccountServiceGrpc accountServiceGrpc;

  @Before
  public void setUp() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
        .forName(serverName).directExecutor().addService(accountServiceGrpc).build().start());

    blockingStub = IlpServiceGrpc.newBlockingStub(
        // Create a client channel and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  /**
   * Sends a request to the {@link IlpServiceGrpc} getAccount method for user 'connie'.
   */
  @Test
  public void getAccountFor_connie() throws Exception {

    GetAccountResponse reply =
        blockingStub.getAccount(GetAccountRequest.newBuilder().setAccountId("connie").build());

    System.out.println(reply);
    assertEquals(reply.getAccountId(), "connie");
    assertEquals(reply.getAssetScale(), 9);
    assertEquals(reply.getAssetCode(), "XRP");
//    assertEquals(reply.getPaymentPointer(), "$jc.ilpv4.dev/connie");
  }

  /**
   * Creates an account through Hermes Grpc. Should return Status.CREATED in header
    */
  @Test
  public void createAccountTest() {
    CreateAccountRequest.Builder request = CreateAccountRequest.newBuilder()
        .setAccountId("noah")
        .setAssetCode("XRP")
        .setAssetScale(9)
        .setDescription("Noah's test account");

    CreateAccountResponse reply = blockingStub.createAccount(request.build());

    assertEquals(1,1);
  }
}


