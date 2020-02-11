package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import com.google.common.primitives.UnsignedLong;

import java.util.UUID;

public interface HermesPaymentTracker {

  Payment payment(final UUID paymentId);

  void registerPayment(final UUID paymentId,
                       final AccountId senderAccountId,
                       final UnsignedLong originalAmount,
                       final PaymentPointer destination)
    throws HermesPaymentTrackerException;

  void updatePaymentOnComplete(UUID paymentId,
                               final UnsignedLong amountSent,
                               final UnsignedLong amountDelivered,
                               final UnsignedLong amountLeftToSend,
                               PaymentStatus status)
    throws HermesPaymentTrackerException;

  void updatePaymentOnError(UUID paymentId);

  enum PaymentStatus {
    PENDING,
    SUCCESSFUL,
    FAILED
  }
}
