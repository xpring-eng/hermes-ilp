package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException;
import org.interledger.spsp.server.grpc.services.AccountsService;
import org.interledger.spsp.server.grpc.services.RequestResponseConverter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
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

  @Autowired
  protected AccountsService accountsService;

  @Autowired
  protected RequestResponseConverter converter;

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    try {
      AccountSettings accountSettings = accountsService.getAccount(AccountId.of(request.getAccountId()));
      GetAccountResponse.Builder getAccountResponse = converter.createGetAccountResponseFromAccountSettings(accountSettings);

      responseObserver.onNext(getAccountResponse.build());
      responseObserver.onCompleted();
    } catch (HermesAccountsClientException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    try {
      Request createAccountRequest = accountsService.constructNewCreateAccountRequest(request);
      Response response = okHttpClient.newCall(createAccountRequest).execute();

      String responseBodyString = response.body().string();
      final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);

      final CreateAccountResponse.Builder replyBuilder = converter.generateCreateAccountResponseFromAccountSettings(accountSettingsResponse);

      logger.info("Account created successfully with accountId: " + request.getAccountId());

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      logger.error("Account creation failed.  Error is: ");
      logger.error(e.getMessage());

      responseObserver.onError(e);
    }
  }



}
