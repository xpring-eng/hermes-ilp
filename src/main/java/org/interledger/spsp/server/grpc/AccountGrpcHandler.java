package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountAlreadyExistsProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountException;
import org.interledger.spsp.server.grpc.services.AccountsServiceImpl;
import org.interledger.spsp.server.grpc.services.AccountRequestResponseConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@GRpcService
public class AccountGrpcHandler extends AccountServiceGrpc.AccountServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

//  @Autowired
  protected OkHttpClient okHttpClient;

//  @Autowired
  protected ObjectMapper objectMapper;

//  @Autowired
  protected AccountsServiceImpl accountsService;

  public AccountGrpcHandler(OkHttpClient okHttpClient, ObjectMapper objectMapper, AccountsServiceImpl accountsService) {
    this.okHttpClient = okHttpClient;
    this.objectMapper = objectMapper;
    this.accountsService = accountsService;
  }

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    Status grpcStatus = Status.OK;

    try {
      AccountSettings accountSettings = accountsService.getAccount(AccountId.of(request.getAccountId()));
      GetAccountResponse.Builder getAccountResponse = AccountRequestResponseConverter.createGetAccountResponseFromAccountSettings(accountSettings);

      responseObserver.onNext(getAccountResponse.build());
      responseObserver.onCompleted();
    } catch (AccountNotFoundProblem e) {
      grpcStatus = Status.NOT_FOUND;
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException)  {
        grpcStatus = Status.ABORTED;
      } else {
        grpcStatus = Status.INTERNAL;
      }
    }

    responseObserver.onError(new StatusRuntimeException(grpcStatus));
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    Status grpcStatus = Status.OK;
    try {
      // Convert request to AccountSettings
      AccountSettings requestedAccountSettings = AccountRequestResponseConverter.accountSettingsFromCreateAccountRequest(request);

      // Create account on the connector
      AccountSettings returnedAccountSettings = accountsService.createAccount(requestedAccountSettings);

      // Convert returned AccountSettings into Grpc response object
      final CreateAccountResponse.Builder replyBuilder =
        AccountRequestResponseConverter.generateCreateAccountResponseFromAccountSettings(returnedAccountSettings);

      logger.info("Account created successfully with accountId: " + request.getAccountId());
      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (HermesAccountException e) {
      grpcStatus = Status.INVALID_ARGUMENT.withCause(e);
    } catch (AccountAlreadyExistsProblem e) {
      grpcStatus = Status.ALREADY_EXISTS.withCause(e);
    }
    responseObserver.onError(new StatusRuntimeException(grpcStatus));
  }



}
