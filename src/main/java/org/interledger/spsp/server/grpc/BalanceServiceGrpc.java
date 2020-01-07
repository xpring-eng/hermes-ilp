package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.ConnectorBalanceClient;

import feign.FeignException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@GRpcService
public class BalanceServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {
  Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected ConnectorBalanceClient balanceClient;

  @Autowired
  protected OkHttpClient okHttpClient;

  @Override
  public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
    try {
      balanceClient.getBalance("Bearer " + request.getJwt(), AccountId.of(request.getAccountId()))
        .ifPresent(balanceResponse -> {
          final GetBalanceResponse reply = GetBalanceResponse.newBuilder()
            .setAssetScale(balanceResponse.assetScale())
            .setAssetCode(balanceResponse.assetCode())
            .setNetBalance(balanceResponse.accountBalance().netBalance().longValue())
            .setPrepaidAmount(balanceResponse.accountBalance().prepaidAmount())
            .setClearingBalance(balanceResponse.accountBalance().clearingBalance())
            .setAccountId(balanceResponse.accountBalance().accountId().value())
            .build();

          logger.info("Balance retrieved successfully.");
          logger.info(reply.toString());

          responseObserver.onNext(reply);
          responseObserver.onCompleted();
        });
    } catch (FeignException e) {
      Status exceptionStatus;
      switch (e.status()) {
        case 401:
          exceptionStatus = Status.PERMISSION_DENIED;
          break;
        case 404:
          exceptionStatus = Status.NOT_FOUND;
          break;
        default:
          exceptionStatus = Status.INTERNAL;
          break;
      }
      responseObserver.onError(new StatusRuntimeException(exceptionStatus));
    }
  }

}
