package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.link.LinkType;
import org.interledger.link.LoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.BearerTokenSupplier;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.Int64Value;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

      final CreateAccountResponse.Builder replyBuilder = generateCreateAccountResponseFromAccountSettings(accountSettingsResponse);

      logger.info("Account created successfully with accountId: " + request.getAccountId());

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      logger.error("Account creation failed.  Error is: ");
      logger.error(e.getMessage());

      responseObserver.onError(e);
    }
  }

  private CreateAccountResponse.Builder generateCreateAccountResponseFromAccountSettings(AccountSettings accountSettings) {

    long maxPacketAmount = accountSettings.maximumPacketAmount().isPresent() ?
        accountSettings.maximumPacketAmount().get().longValue() : 0L;

    // TODO: Maybe there is a way to copy properties to reduce some of this code
    // Convert AccountSettings into CreateAccountResponse
    AccountBalanceSettings accountBalanceSettings = accountSettings.balanceSettings();
    CreateAccountResponse.Builder protoResponseBuilder = CreateAccountResponse.newBuilder()
      .setAccountRelationship(accountSettings.accountRelationship().toString())
      .setAssetCode(accountSettings.assetCode())
      .setAssetScale(accountSettings.assetScale())
      .setMaximumPacketAmount(maxPacketAmount)
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()))
      .setAccountId(accountSettings.accountId().value())
      .setCreatedAt(accountSettings.createdAt().toString())
      .setModifiedAt(accountSettings.modifiedAt().toString())
      .setDescription(accountSettings.description())
      .setLinkType(accountSettings.linkType().value())
      .setIsInternal(accountSettings.isInternal())
      .setIsConnectionInitiator(accountSettings.isConnectionInitiator())
      .setIlpAddressSegment(accountSettings.ilpAddressSegment())
      .setIsSendRoutes(accountSettings.isSendRoutes())
      .setIsReceiveRoutes(accountSettings.isReceiveRoutes());


    CreateAccountResponse.BalanceSettings.Builder balanceSettingsBuilder = CreateAccountResponse.BalanceSettings.newBuilder()
      .setSettleTo(accountBalanceSettings.settleTo());
    accountBalanceSettings.minBalance().ifPresent(
      minBalance -> balanceSettingsBuilder.setMinBalance(accountBalanceSettings.minBalance().get())
    );
    accountBalanceSettings.settleThreshold().ifPresent(
      minBalance -> balanceSettingsBuilder.setSettleThreshold(accountBalanceSettings.settleThreshold().get())
    );

    protoResponseBuilder.setBalanceSettings(balanceSettingsBuilder.build());

    accountSettings.rateLimitSettings().maxPacketsPerSecond().ifPresent(
      max -> protoResponseBuilder.setMaximumPacketAmount(accountSettings.rateLimitSettings().maxPacketsPerSecond().get())
    );

    Optional<SettlementEngineDetails> settlementEngineDetails = accountSettings.settlementEngineDetails();
    CreateAccountResponse.SettlementEngineDetails.Builder settlementEngineBuilder = CreateAccountResponse.SettlementEngineDetails.newBuilder();
    settlementEngineDetails.ifPresent(
      settle -> {
        settlementEngineDetails.get().settlementEngineAccountId().ifPresent(
          accountId -> settlementEngineBuilder.setSettlementEngineAccountId(settlementEngineDetails.get().settlementEngineAccountId().get().value())
        );

        settlementEngineBuilder.setBaseUrl(settlementEngineDetails.get().baseUrl() == null ? null : settlementEngineDetails.get().baseUrl().toString());

        settlementEngineBuilder.putAllCustomSettings(settingsMapToGrpcSettingsMap(settlementEngineDetails.get().customSettings()));
      });

    return protoResponseBuilder
      .setIsParentAccount(accountSettings.isParentAccount())
      .setIsChildAccount(accountSettings.isChildAccount())
      .setIsPeerAccount(accountSettings.isPeerAccount())
      .setIsPeerOrParentAccount(accountSettings.isPeerOrParentAccount())
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()));
  }

  private Map<String, String> settingsMapToGrpcSettingsMap(Map<String, Object> settingsMap) {
    return settingsMap.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
  }

  private Request constructNewCreateAccountRequest(CreateAccountRequest request) throws JsonProcessingException {
    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
        .add(CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .add(ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
        .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI;

    DecodedJWT jwt = JWT.decode(request.getJwt());
    Map<String, Object> customSettings = new HashMap<>();
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_URL, "https://someurl.com"); // Do we need this for RS256?
//    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, request.getAccountId());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, "some shared secret");
    customSettings.put("ilpOverHttp.incoming.token_subject", request.getAccountId()); // FIXME use above line from new quilt
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256); // FIXME use JWT_RS_256 from new quilt
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwt.getAudience());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwt.getIssuer());

    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://someurl.com"); // Do we need this for RS256?
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, "some shared secret");
    customSettings.put("ilpOverHttp.outgoing.token_subject", request.getAccountId()); // FIXME use above line from new quilt
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256); // FIXME use JWT_RS_256 from new quilt
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, jwt.getAudience());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, jwt.getIssuer());

    // Had to bring in AccountIdModule to objectMapper from connector-jackson for this serialization to work
    String bodyJson = objectMapper.writeValueAsString(AccountSettings.builder()
      .accountId(AccountId.of(request.getAccountId()))
      .assetCode(request.getAssetCode())
      .assetScale(request.getAssetScale())
      .description(request.getDescription())
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
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
