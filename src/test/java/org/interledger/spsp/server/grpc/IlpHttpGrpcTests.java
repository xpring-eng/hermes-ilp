package org.interledger.spsp.server.grpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.interledger.spsp.server.HermesServerApplication;

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
public class IlpHttpGrpcTests {
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";
  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts";
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private IlpOverHttpServiceGrpc.IlpOverHttpServiceBlockingStub blockingStub;

  @Autowired
  IlpOverHttpGrpcHandler ilpOverHttpGrpcHandler;

  @Before
  public void setUp() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
        .forName(serverName).directExecutor().addService(ilpOverHttpGrpcHandler).build().start());

    blockingStub = IlpOverHttpServiceGrpc.newBlockingStub(
        // Create a client channel and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  @Test
  public void sendMoneyTest() {
    String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1rTTJPRGMxUVRKR05qSXlRelJFUmtKQk5qRTNNMFpCTkRsRFJEQkVSVFF3UWpWRk5VSkNNdyJ9.eyJuaWNrbmFtZSI6Im5oYXJ0bmVyIiwibmFtZSI6Im5oYXJ0bmVyQGdtYWlsLmNvbSIsInBpY3R1cmUiOiJodHRwczovL2F2YXRhcnMwLmdpdGh1YnVzZXJjb250ZW50LmNvbS91LzQ0NDAzNDU_dj00IiwidXBkYXRlZF9hdCI6IjIwMTktMTItMjdUMTg6NTI6MDMuMTg1WiIsImVtYWlsIjoibmhhcnRuZXJAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8veHByaW5nc2FuZGJveC5hdXRoMC5jb20vIiwic3ViIjoiZ2l0aHVifDQ0NDAzNDUiLCJhdWQiOiIwcjFmclo1OWV5bERzTUgzYWNWZVNKRDVLSTZwdUVobyIsImlhdCI6MTU3NzQ3MjczMywiZXhwIjoxNTc3NDc2MzMzfQ.Kzddgj_Zd4Ib7jq4avLCigzIxJJjuh8NZII0wKfT4lA1XEPc_HAUEhNCTRe--CHpaCOo6HLs4YQdTcC_0flWLXX7_Uz5zVCS9KVX__g3BJRvU_pZjzt51iBkA8zKKzmxMAd9w2g0Lq8SanqMDZq7TfNXb85ZFDBPAjPDLV42sgkWy0i0RADddEadmACuvnp_GC91RDLIMOPY8BCDF8JBB2RRZ_CsPRLlpMlGaQYAgs9fbquq6qPwHXoFxHVfsGg_xYi4W8uAc_J1qWxOPDUEWKf_HLAu0a-0_kbufn1xXqkHowHaR4VA90ljabFUR92ioWXYUlwvPaFKqzalkeDdpQ";

    SendPaymentRequest sendMoneyRequest = SendPaymentRequest.newBuilder()
      .setAccountId("alice")
      .setAmount(10000)
      .setDestinationPaymentPointer("$spsp-dev.interledger.ml/bob")
      .setJwt(jwt)
      .build();

    SendPaymentResponse response = blockingStub.sendMoney(sendMoneyRequest);
    System.out.println(response);
  }


}


