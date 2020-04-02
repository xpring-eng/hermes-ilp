package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.grpc.auth.IlpGrpcAuthContext;
import org.interledger.spsp.server.grpc.services.AccountRequestResponseConverter;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.services.SendMoneyService;
import org.interledger.spsp.server.util.ExceptionHandlerUtils;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@GRpcService
public class IlpOverHttpGrpcHandler extends IlpOverHttpServiceGrpc.IlpOverHttpServiceImplBase {

  @Autowired
  protected SendMoneyService sendMoneyService;

  @Autowired
  protected IlpGrpcAuthContext ilpGrpcAuthContext;

  @Autowired
  protected ExceptionHandlerUtils exceptionHandlerUtils;

  @Override
  public void sendMoney(SendPaymentRequest request, StreamObserver<SendPaymentResponse> responseObserver) {
    // Send payment using STREAM
    try {
      final BearerToken bearerToken = BearerToken.fromBearerTokenValue(ilpGrpcAuthContext.getAuthorizationHeader());
      final SendMoneyResult result = sendMoneyService.sendMoney(
        AccountId.of(request.getAccountId()),
        Optional.of(bearerToken),
        UnsignedLong.valueOf(request.getAmount()),
        PaymentPointer.of(request.getDestinationPaymentPointer())
      );

      final SendPaymentResponse sendPaymentResponse = AccountRequestResponseConverter
        .sendPaymentResponseFromSendMoneyResult(result);
      responseObserver.onNext(sendPaymentResponse);
      responseObserver.onCompleted();
    } catch (Exception e) {
      exceptionHandlerUtils.handleException(e, responseObserver);
    }
  }
}
