package org.interledger.spsp.server.grpc;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountBalance;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcMetadataReader;
import org.interledger.spsp.server.grpc.utils.InterceptedService;
import org.interledger.spsp.server.services.NewAccountService;
import org.interledger.spsp.server.services.SendMoneyService;
import org.interledger.stream.SendMoneyResult;

import com.google.common.net.HttpHeaders;
import com.google.common.primitives.UnsignedLong;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})

public class AuthContextInterceptorTests {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  private AccountServiceGrpc.AccountServiceBlockingStub accountServiceBlockingStub;
  private BalanceServiceGrpc.BalanceServiceBlockingStub balanceServiceBlockingStub;
  private IlpOverHttpServiceGrpc.IlpOverHttpServiceBlockingStub paymentServiceBlockingStub;

  @Autowired
  AccountGrpcHandler accountGrpcHandler;

  @Autowired
  BalanceGrpcHandler balanceGrpcHandler;

  @Autowired
  IlpOverHttpGrpcHandler ilpOverHttpGrpcHandler;

  @MockBean
  private NewAccountService accountService;

  @MockBean
  private ConnectorBalanceClient balanceClient;

  @MockBean
  private SendMoneyService sendMoneyService;

  @Autowired
  private IlpGrpcMetadataReader ilpGrpcMetadataReader;

  private ClientInterceptor mockClientInterceptor;

  private String jwt;

  @Before
  public void setUp() throws IOException {
    initMocks(this);

    // Just a sample RS256 JWT.  We don't do any signature validation in these tests, but the JWT still needs to be
    // decoded.
    jwt = "eyJraWQiOiJrZXkxIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYifQ.eyJhdWQiOiJiYXIiLCJzdWIiOiJmb28iLCJpc3MiOiJodHRwOi8vaG9zdC50ZXN0Y29udGFpbmVycy5pbnRlcm5hbDozMjQ1Ni8iLCJleHAiOjI0ODI2Nzg5MTJ9.dXLB6f4Kw-VuY0-gbD50bsk9yHrvP50fUpcbnGFsPfyEXugCMT0FdzuftmvhkVz3DRSLeeIX2_vVpG8_18tHK-lqAvZDU-eX5hiJdNCOrfp9epM6Fh6ZcpOJWLC5E1WonDU8S7FrpXnVmN9iuBZH-4Z5gmd65P_xqcPRECgseyg2Hr4XcGg7zQ95tKzNx0KnQfIuHiKLcDFXUWKwMxMhoiNoWpxuH-g_vbaUE0bpoIUSbLHkpoEKpc9RrY2SLOXo1oOSCGgoRZg7p9AN_I1iG4-60nfRH4tQNDMdDxkuZcqjQ6SQfJ9jufwvMLhirUGPplvaXf3DtqpN03RkOpkR_A";

    // This mock ClientInterceptor will add a COOKIE header with a jwt to the grpc Metadata.
    // This mocks the behavior of the Envoy proxy when receiving an HTTP request with cookies
    mockClientInterceptor = mock(ClientInterceptor.class, delegatesTo(
      new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
          return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
              headers.put(Metadata.Key.of(HttpHeaders.COOKIE, Metadata.ASCII_STRING_MARSHALLER),
                "jwt=" + jwt);

              super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {}, headers);
            }
          };
        }
      }
    ));

    registerGrpc();
  }


  @Test
  public void testCreateAccountWithCookieAuth() {
    CreateAccountRequest request = CreateAccountRequest.newBuilder()
      .build();

    AccountSettings accountSettingsMock = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetCode("XRP")
      .assetScale(9)
      .balanceSettings(AccountBalanceSettings.builder().build())
      .settlementEngineDetails(Optional.empty())
      .rateLimitSettings(AccountRateLimitSettings.builder().build())
      .maximumPacketAmount(UnsignedLong.valueOf(1000))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    when(accountService.createAccount(eq(Optional.of(jwt)), any(Optional.class)))
      .thenReturn(accountSettingsMock);
    CreateAccountResponse reply = accountServiceBlockingStub.createAccount(request);

    verify(accountService, times(1)).createAccount(eq(Optional.of(jwt)), any(Optional.class));
  }

  @Test
  public void testGetBalanceWithCookieAuth() {
    GetBalanceRequest request = GetBalanceRequest.newBuilder()
      .setAccountId("foo")
      .build();

    AccountBalanceResponse accountBalanceResponseMock = AccountBalanceResponse.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of("foo"))
        .clearingBalance(1000)
        .prepaidAmount(10000)
        .build())
      .build();
    when(balanceClient.getBalance(eq(jwt), any())).thenReturn(accountBalanceResponseMock);

    GetBalanceResponse response = balanceServiceBlockingStub.getBalance(request);
    verify(balanceClient, times(1)).getBalance(eq(jwt), any());
  }

  @Test
  public void testSendMoneyWithCookieAuth() throws Exception {

    SendPaymentRequest sendMoneyRequest = SendPaymentRequest.newBuilder()
      .setAccountId("foo")
      .setAmount(1000)
      .setDestinationPaymentPointer("$foo.baz/bar")
      .build();

    SendMoneyResult sendMoneyResultMock = SendMoneyResult.builder()
      .originalAmount(UnsignedLong.valueOf(10))
      .amountDelivered(UnsignedLong.valueOf(10))
      .amountSent(UnsignedLong.valueOf(10))
      .amountLeftToSend(UnsignedLong.valueOf(10))
      .numFulfilledPackets(10)
      .numRejectPackets(10)
      .sendMoneyDuration(Duration.ZERO)
      .successfulPayment(true)
      .build();

    when(sendMoneyService.sendMoney(any(),
      eq(jwt),
      any(),
      any())).thenReturn(sendMoneyResultMock);

    SendPaymentResponse response = paymentServiceBlockingStub.sendMoney(sendMoneyRequest);
    verify(sendMoneyService, times(1)).sendMoney(any(), eq(jwt), any(), any());
  }

  private void registerGrpc() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
      .forName(serverName)
      .directExecutor()
      .addService(InterceptedService.of(accountGrpcHandler, ilpGrpcMetadataReader))
      .addService(InterceptedService.of(balanceGrpcHandler, ilpGrpcMetadataReader))
      .addService(InterceptedService.of(ilpOverHttpGrpcHandler, ilpGrpcMetadataReader))
      .build()
      .start()
    );

    accountServiceBlockingStub = AccountServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));

    balanceServiceBlockingStub = BalanceServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));

    paymentServiceBlockingStub = IlpOverHttpServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup
        .register(InProcessChannelBuilder
          .forName(serverName)
          .directExecutor()
          .intercept(mockClientInterceptor)
          .build()));
  }
}
