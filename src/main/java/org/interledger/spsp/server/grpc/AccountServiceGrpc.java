package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.auth.BearerTokenSupplier;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

@GRpcService
public class AccountServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";

  @Autowired
  protected OkHttpClient okHttpClient;

  @Autowired
  protected ObjectMapper objectMapper;

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {

    try {
      Request getAccountRequest = this.constructNewGetAccountRequest(request);
      Response response = okHttpClient.newCall(getAccountRequest).execute();

      String responseBodyString = response.body().string();
      final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);

      final GetAccountResponse.Builder replyBuilder = GetAccountResponse.newBuilder()
          .setAccountId(accountSettingsResponse.accountId().value())
          .setAssetCode(accountSettingsResponse.assetCode())
          .setAssetScale(accountSettingsResponse.assetScale());

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    try {
      Request createAccountRequest = this.constructNewCreateAccountRequest(request);
      Response response = okHttpClient.newCall(createAccountRequest).execute();

      String responseBodyString = response.body().string();
      final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);


      SettlementEngineDetails settlementEngineDetails = accountSettingsResponse.settlementEngineDetails().isPresent() ?
          accountSettingsResponse.settlementEngineDetails().get() : null;

      long maxPacketAmount = accountSettingsResponse.maximumPacketAmount().isPresent() ?
          accountSettingsResponse.maximumPacketAmount().get().longValue() : 0L;

      // TODO: Maybe there is a way to copy properties to reduce some of this code
      // Convert AccountSettings into CreateAccountResponse
      final CreateAccountResponse.Builder replyBuilder = CreateAccountResponse.newBuilder()
          .setAccountRelationship(accountSettingsResponse.accountRelationship().toString())
          .setAssetCode(accountSettingsResponse.assetCode())
          .setAssetScale(accountSettingsResponse.assetScale())
          .setMaximumPacketAmount(maxPacketAmount)
          // TODO: Figure out types for settings map fields
//          .putAllCustomSettings(accountSettingsResponse.customSettings())
          .setAccountId(accountSettingsResponse.accountId().value())
          .setCreatedAt(accountSettingsResponse.createdAt().toString())
          .setModifiedAt(accountSettingsResponse.modifiedAt().toString())
          .setDescription(accountSettingsResponse.description())
          .setLinkType(accountSettingsResponse.linkType().value())
          .setIsInternal(accountSettingsResponse.isInternal())
          .setIsConnectionInitiator(accountSettingsResponse.isConnectionInitiator())
          .setIlpAddressSegment(accountSettingsResponse.ilpAddressSegment())
          .setIsSendRoutes(accountSettingsResponse.isSendRoutes())
          .setIsReceiveRoutes(accountSettingsResponse.isReceiveRoutes())
//          .putAllBalanceSettings(accountSettingsResponse.balanceSettings())
//          .putAllRateLimitSettings(accountSettingsResponse.rateLimitSettings())
          // TODO: What should we do about this being null. Cant set CreateAccountResponse.SettlementEngineDetails to null
          .setSettlementEngineDetails(settlementEngineDetails == null ?
              CreateAccountResponse.SettlementEngineDetails.newBuilder()
                  .setSettlementEngineAccountId("")
                  .setBaseUrl("") :
              CreateAccountResponse.SettlementEngineDetails.newBuilder()
                .setSettlementEngineAccountId(settlementEngineDetails.settlementEngineAccountId().isPresent() ?
                    settlementEngineDetails.settlementEngineAccountId().get().value() : null)
                .setBaseUrl(settlementEngineDetails.baseUrl().toString())
//                .putAllCustomSettings(settlementEngineDetails.customSettings())
          )
          .setIsParentAccount(accountSettingsResponse.isParentAccount())
          .setIsChildAccount(accountSettingsResponse.isChildAccount())
          .setIsPeerAccount(accountSettingsResponse.isPeerAccount())
          .setIsPeerOrParentAccount(accountSettingsResponse.isPeerOrParentAccount());

      logger.info("Account created successfully with accountId: " + request.getAccountId());

      responseObserver.onNext(replyBuilder
          .setCreateStatus(HttpStatus.CREATED.value()).build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      logger.error("Account creation failed.  Error is: ");
      logger.error(e.getMessage());

      responseObserver.onError(e);
    }
  }

  private Request constructNewCreateAccountRequest(CreateAccountRequest request) throws JsonProcessingException {
    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
        .add(CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .add(ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI;

    // Had to bring in AccountIdModule to objectMapper from connector-jackson for this serialization to work
    String bodyJson = objectMapper.writeValueAsString(AccountSettings.builder()
        .accountId(AccountId.of(request.getAccountId()))
        .assetCode(request.getAssetCode())
        .assetScale(request.getAssetScale())
        .description(request.getDescription())
        .accountRelationship(AccountRelationship.CHILD)
        .linkType(LoopbackLink.LINK_TYPE)
        .build());

    RequestBody body = RequestBody.create(
        bodyJson,
        okhttp3.MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE));

    return new Request.Builder()
        .headers(httpRequestHeaders)
        .url(requestUrl)
        .post(body)
        .build();
  }

  private Request constructNewGetAccountRequest(GetAccountRequest request) {

    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
        .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI + "/" + request.getAccountId();

    return new Request.Builder()
        .headers(httpRequestHeaders)
        .url(requestUrl)
        .get()
        .build();
  }

}
