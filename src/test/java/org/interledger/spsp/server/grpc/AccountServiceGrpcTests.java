package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import okhttp3.Headers;
import okhttp3.Request;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import javax.annotation.PostConstruct;


@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
    classes = {HermesServerApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountServiceGrpcTests {
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";
  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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
   * Gets account for foo.  Should succeed on {@link org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException}
   */
  @Test
  public void getAccountFooFailsAccountNotFound() {
/*    expectedException.expect(HermesAccountsClientException.class);
    expectedException.expectCause(is(new StatusRuntimeException(Status.UNKNOWN)));*/

    expectedException.expect(StatusRuntimeException.class);
    AccountId accountId = AccountId.of("foo");
    /*expectedException.expectCause(is(new HermesAccountsClientException("Unable to get account information from connector for accountId = " + accountId,
      new AccountNotFoundProblem(accountId), accountId)));*/

    GetAccountResponse reply =
      blockingStub.getAccount(GetAccountRequest.newBuilder().setAccountId("foo").build());

  }

  /**
   * Creates an account through Hermes Grpc
    */
  // TODO: Spin up a connector locally
  @Test
  public void createAccountTest() {
    String accountID = "AccountServiceGRPCTest";
    String accountDescription = "Noah's test account";
    Map<String, String> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://xpringsandbox.auth0.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "0r1frZ59eylDsMH3acVeSJD5KI6puEho");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, "github|4440345");

    CreateAccountResponse expected = CreateAccountResponse.newBuilder()
      .setAccountRelationship("CHILD")
      .setAssetCode("XRP")
      .setAssetScale(9)
      .putAllCustomSettings(customSettings)
      .setAccountId(accountID)
      .setDescription(accountDescription)
      .setLinkType(IlpOverHttpLink.LINK_TYPE_STRING)
      .setIsConnectionInitiator(true)
      .setIlpAddressSegment(accountID)
      .setBalanceSettings(CreateAccountResponse.BalanceSettings.newBuilder().build())
      .setIsChildAccount(true)
      .build();


    String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ik1rTTJPRGMxUVRKR05qSXlRelJFUmtKQk5qRTNNMFpCTkRsRFJEQkVSVFF3UWpWRk5VSkNNdyJ9.eyJuaWNrbmFtZSI6Im5oYXJ0bmVyIiwibmFtZSI6Im5oYXJ0bmVyQGdtYWlsLmNvbSIsInBpY3R1cmUiOiJodHRwczovL2F2YXRhcnMwLmdpdGh1YnVzZXJjb250ZW50LmNvbS91LzQ0NDAzNDU_dj00IiwidXBkYXRlZF9hdCI6IjIwMTktMTItMjdUMTg6NTI6MDMuMTg1WiIsImVtYWlsIjoibmhhcnRuZXJAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImlzcyI6Imh0dHBzOi8veHByaW5nc2FuZGJveC5hdXRoMC5jb20vIiwic3ViIjoiZ2l0aHVifDQ0NDAzNDUiLCJhdWQiOiIwcjFmclo1OWV5bERzTUgzYWNWZVNKRDVLSTZwdUVobyIsImlhdCI6MTU3NzQ3MjczMywiZXhwIjoxNTc3NDc2MzMzfQ.Kzddgj_Zd4Ib7jq4avLCigzIxJJjuh8NZII0wKfT4lA1XEPc_HAUEhNCTRe--CHpaCOo6HLs4YQdTcC_0flWLXX7_Uz5zVCS9KVX__g3BJRvU_pZjzt51iBkA8zKKzmxMAd9w2g0Lq8SanqMDZq7TfNXb85ZFDBPAjPDLV42sgkWy0i0RADddEadmACuvnp_GC91RDLIMOPY8BCDF8JBB2RRZ_CsPRLlpMlGaQYAgs9fbquq6qPwHXoFxHVfsGg_xYi4W8uAc_J1qWxOPDUEWKf_HLAu0a-0_kbufn1xXqkHowHaR4VA90ljabFUR92ioWXYUlwvPaFKqzalkeDdpQ";
    CreateAccountRequest.Builder request = CreateAccountRequest.newBuilder()
      .setAccountId(accountID)
      .setAssetCode("XRP")
      .setAssetScale(9)
      .setDescription(accountDescription)
      .setJwt(jwt);

    CreateAccountResponse reply = blockingStub.createAccount(request.build());
    System.out.println(reply);

    // Get rid of phantom created account
    deleteAccountByID(accountID);

    Assertions.assertThat(expected)
      .usingRecursiveComparison()
      .ignoringFields("createdAt_", "memoizedHashCode", "modifiedAt_")
      .isEqualTo(reply);
  }

  // Just need this so we don't create a bunch of orphan accounts on the dev connector
  private void deleteAccountByID(String accountId) {
    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
        .add(CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .add(ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI + "/" + URLEncoder.encode(accountId);

    Request deleteRequest = new Request.Builder()
        .headers(httpRequestHeaders)
        .url(requestUrl)
        .delete()
        .build();

    try {
      accountServiceGrpc.okHttpClient.newCall(deleteRequest).execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}


