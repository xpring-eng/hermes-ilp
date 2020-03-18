package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.grpc.auth.IlpGrpcAuthContext;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.util.ExceptionHandlerUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

@GRpcService
public class BalanceGrpcHandler extends BalanceServiceGrpc.BalanceServiceImplBase {

  @Autowired
  protected ConnectorBalanceClient balanceClient;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  protected IlpGrpcAuthContext ilpGrpcAuthContext;
  @Autowired
  protected ExceptionHandlerUtils exceptionHandlerUtils;

  @Override
  public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
    try {
      BearerToken bearerToken = BearerToken.fromBearerTokenValue(ilpGrpcAuthContext.getAuthorizationHeader());
      AccountBalanceResponse balanceResponse = balanceClient
        .getBalance(bearerToken.value(), AccountId.of(request.getAccountId()));

      final GetBalanceResponse reply = GetBalanceResponse.newBuilder()
        .setAssetScale(balanceResponse.assetScale())
        .setAssetCode(balanceResponse.assetCode())
        .setNetBalance(balanceResponse.accountBalance().netBalance().longValue())
        .setPrepaidAmount(balanceResponse.accountBalance().prepaidAmount())
        .setClearingBalance(balanceResponse.accountBalance().clearingBalance())
        .setAccountId(balanceResponse.accountBalance().accountId().value())
        .build();

      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    } catch (Exception e) {
      exceptionHandlerUtils.handleException(e, responseObserver);
    }
  }

}
