package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import java.util.UUID;

public interface HermesPaymentTracker {

  Payment payment(final UUID paymentId);

  void registerPayment(final UUID paymentId,
                       final AccountId senderAccountId,
                       final long originalAmount,
                       final PaymentPointer destination)
    throws HermesPaymentTrackerException;

  void updatePaymentOnComplete(UUID paymentId,
                               final long amountSent,
                               final long amountDelivered,
                               final long amountLeftToSend,
                               PaymentStatus status)
    throws HermesPaymentTrackerException;

  enum PaymentStatus {
    PENDING,
    SUCCESSFUL,
    FAILED
  }
}
