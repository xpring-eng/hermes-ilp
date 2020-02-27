package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcAuthContext;
import org.interledger.spsp.server.grpc.services.AccountRequestResponseConverter;
import org.interledger.spsp.server.model.CreateAccountRestRequest;
import org.interledger.spsp.server.model.ImmutableCreateAccountRestRequest;
import org.interledger.spsp.server.services.NewAccountService;

import feign.FeignException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import okhttp3.HttpUrl;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@GRpcService
public class AccountGrpcHandler extends AccountServiceGrpc.AccountServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected ConnectorAdminClient adminClient;

  @Autowired
  protected NewAccountService newAccountService;

  @Autowired
  protected ConnectorRoutesClient routesClient;

  @Autowired
  protected HttpUrl spspReceiverUrl;

  @Autowired
  protected IlpGrpcAuthContext ilpGrpcAuthContext;

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    Status grpcStatus;
    try {
      GetAccountResponse accountResponse = adminClient.findAccount(request.getAccountId())
        .map(account -> AccountRequestResponseConverter.createGetAccountResponseFromAccountSettings(account, spspReceiverUrl))
        .orElseThrow(() -> new AccountNotFoundProblem(AccountId.of(request.getAccountId())));

      responseObserver.onNext(accountResponse);
      responseObserver.onCompleted();
      return;
    } catch (FeignException e) {
      if (e.status() == 401) {
        grpcStatus = Status.PERMISSION_DENIED;
      } else {
        grpcStatus = Status.INTERNAL;
      }
    } catch (AccountNotFoundProblem e) {
      grpcStatus = Status.NOT_FOUND;
    }

    responseObserver.onError(new StatusRuntimeException(grpcStatus));
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    Status grpcStatus;
    try {
      // Create account on the connector
      Optional<CreateAccountRestRequest> convertedRequest = createAccountRestRequestFromGrpc(request);
      AccountSettings returnedAccountSettings = newAccountService
        .createAccount(Optional.ofNullable(ilpGrpcAuthContext.getAuthorizationHeader()), convertedRequest);

      // Convert returned AccountSettings into Grpc response object
      final CreateAccountResponse.Builder replyBuilder =
        AccountRequestResponseConverter.generateCreateAccountResponseFromAccountSettings(returnedAccountSettings, spspReceiverUrl);

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
      return;
    } catch (FeignException e) {
      logger.error("Error creating account: " + e);
      switch (e.status()) {
        case 401:
          grpcStatus = Status.PERMISSION_DENIED;
          break;
        case 409:
          grpcStatus = Status.ALREADY_EXISTS;
          break;
        default:
          grpcStatus = Status.INTERNAL;
      }
    }

    responseObserver.onError(new StatusRuntimeException(grpcStatus));
  }

  /**
   * Convert a {@link CreateAccountRequest} into a {@link CreateAccountRestRequest} so that our service level code
   * only has one execution path.
   *
   * This method will also validate that the required parameters (assetCode and assetScale) are set if any other fields are set.
   *
   * If no other fields are set, we can assume that the requester wanted Hermes to generate a default account for them (similar to not
   * passing a body in the REST request equivalent). GRPC generated objects are never null, but rather use default values for fields that are
   * left blank.
   *
   * If there is at least one field set in the GRPC object, we must validate the assetCode and assetScale are specified.
   *
   * @param request
   * @return An {@link Optional<CreateAccountRestRequest>}. If the grpc request was empty/defaulted, return {@link Optional#empty()}
   *        otherwise try to convert the given request to a rest request.
   */
  private Optional<CreateAccountRestRequest> createAccountRestRequestFromGrpc(CreateAccountRequest request) {
    String assetCode = request.getAssetCode().isEmpty() ? null : request.getAssetCode();
    Integer assetScale = request.getAssetScale() == 0 ? null : request.getAssetScale();

    if (!requestIsEmpty(request)) {
      ImmutableCreateAccountRestRequest.Builder requestBuilder = CreateAccountRestRequest.builder(assetCode, assetScale);
      if (!request.getAccountId().isEmpty()) {
        requestBuilder.accountId(request.getAccountId());
      }

      if (!request.getDescription().isEmpty()) {
        requestBuilder.description(request.getDescription());
      }

      return Optional.of(requestBuilder.build());
    }

    return Optional.empty();
  }

  /**
   * If all the fields in the GRPC CreateAccountRequest are empty or the protobuf defaults,
   * we can assume the requester wants Hermes to generate an account for them.
   * @param request
   * @return
   */
  private boolean requestIsEmpty(CreateAccountRequest request) {
    if (!request.getAccountId().isEmpty()) {
      return false;
    }
    if (!request.getAssetCode().isEmpty()) {
      return false;
    }
    if (request.getAssetScale() != 0) {
      return false;
    }
    if (!request.getDescription().isEmpty()) {
      return false;
    }

    return true;
  }
}
