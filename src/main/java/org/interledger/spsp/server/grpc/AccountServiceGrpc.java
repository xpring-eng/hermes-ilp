package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException;
import org.interledger.spsp.server.grpc.services.AccountsServiceImpl;
import org.interledger.spsp.server.grpc.services.RequestResponseConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@GRpcService
public class AccountServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

//  @Autowired
  protected OkHttpClient okHttpClient;

//  @Autowired
  protected ObjectMapper objectMapper;

//  @Autowired
  protected AccountsServiceImpl accountsService;

  public AccountServiceGrpc(OkHttpClient okHttpClient, ObjectMapper objectMapper, AccountsServiceImpl accountsService) {
    this.okHttpClient = okHttpClient;
    this.objectMapper = objectMapper;
    this.accountsService = accountsService;
  }

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    try {
      AccountSettings accountSettings = accountsService.getAccount(AccountId.of(request.getAccountId()));
      GetAccountResponse.Builder getAccountResponse = RequestResponseConverter.createGetAccountResponseFromAccountSettings(accountSettings);

      responseObserver.onNext(getAccountResponse.build());
      responseObserver.onCompleted();
    } catch (HermesAccountsClientException e) {
      responseObserver.onError(e);
    }
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    try {
      // Convert request to AccountSettings
      AccountSettings requestedAccountSettings = RequestResponseConverter.accountSettingsFromCreateAccountRequest(request);

      // Create account on the connector
      AccountSettings returnedAccountSettings = accountsService.createAccount(requestedAccountSettings);

      // Convert returned AccountSettings into Grpc response object
      final CreateAccountResponse.Builder replyBuilder =
        RequestResponseConverter.generateCreateAccountResponseFromAccountSettings(returnedAccountSettings);

      logger.info("Account created successfully with accountId: " + request.getAccountId());
      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (HermesAccountsClientException e) {
      logger.error("Account creation failed.  Error is: ");
      logger.error(e.getMessage());

      responseObserver.onError(e);
    }
  }



}
