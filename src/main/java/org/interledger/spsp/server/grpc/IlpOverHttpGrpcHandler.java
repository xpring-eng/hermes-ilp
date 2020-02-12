package org.interledger.spsp.server.grpc;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.grpc.auth.IlpGrpcAuthContext;
import org.interledger.spsp.server.grpc.services.AccountRequestResponseConverter;
import org.interledger.spsp.server.model.Payment;
import org.interledger.spsp.server.services.PaymentService;
import org.interledger.spsp.server.services.tracker.HermesPaymentTrackerException;

import com.google.common.primitives.UnsignedLong;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@GRpcService
public class IlpOverHttpGrpcHandler extends IlpOverHttpServiceGrpc.IlpOverHttpServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected PaymentService paymentService;

  @Autowired
  protected IlpGrpcAuthContext ilpGrpcAuthContext;

  @Override
  public void sendMoney(SendPaymentRequest request, StreamObserver<SendPaymentResponse> responseObserver) {
    // Send payment using STREAM
    Payment result;
    try {
      String jwt = ilpGrpcAuthContext.getAuthorizationHeader();
      result = paymentService.sendMoney(AccountId.of(request.getAccountId()),
        jwt.substring("Bearer ".length()),
        UnsignedLong.valueOf(request.getAmount()),
        PaymentPointer.of(request.getDestinationPaymentPointer()),
        UUID.fromString(request.getPaymentId()));
    } catch (HermesPaymentTrackerException e) {
      responseObserver.onError(e);
      return;
    }

    SendPaymentResponse sendPaymentResponse = AccountRequestResponseConverter.sendPaymentResponseFromPayment(result);
    responseObserver.onNext(sendPaymentResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void getPayment(GetPaymentRequest request, StreamObserver<GetPaymentResponse> responseObserver) {
    Payment result;
    try {
      result = paymentService.getPayment(UUID.fromString(request.getPaymentId()));
    } catch (HermesPaymentTrackerException e) {
      responseObserver.onError(e);
      return;
    }

    GetPaymentResponse getPaymentResponse = AccountRequestResponseConverter.getPaymentResponseFromPayment(result);
    responseObserver.onNext(getPaymentResponse);
    responseObserver.onCompleted();
  }
}
