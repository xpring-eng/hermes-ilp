package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.google.common.primitives.UnsignedLong;

import java.util.UUID;

public interface HermesPaymentTracker {

  void registerPayment(UUID paymentId,
                       AccountId senderAccountId,
                       UnsignedLong originalAmount,
                       PaymentPointer destination);

  void updatePaymentOnComplete(UUID paymentId,
                               UnsignedLong amountSent,
                               UnsignedLong amountDelivered,
                               UnsignedLong amountLeftToSend,
                               PaymentStatus status);

  enum PaymentStatus {
    PENDING,
    COMPLETE,
    FAILED
  }
}
