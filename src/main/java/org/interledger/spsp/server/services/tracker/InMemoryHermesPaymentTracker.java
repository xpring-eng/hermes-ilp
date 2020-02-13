package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class InMemoryHermesPaymentTracker implements HermesPaymentTracker {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private Map<UUID, Payment> payments;

  public InMemoryHermesPaymentTracker() {
    this.payments = new HashMap<>();
  }

  @Override
  public Payment payment(UUID paymentId) throws HermesPaymentTrackerException {
    if (payments.containsKey(paymentId)) {
      return payments.get(paymentId);
    }
    throw new HermesPaymentTrackerException("Payment does not exist! paymentId=" + paymentId);
  }

  @Override
  public void registerPayment(UUID paymentId, AccountId senderAccountId, UnsignedLong originalAmount, PaymentPointer destination) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(senderAccountId, "senderAccountId must not be null");
    Objects.requireNonNull(originalAmount, "originalAmount must not be null");
    Objects.requireNonNull(destination, "destination must not be null");

    Payment payment = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(senderAccountId)
      .originalAmount(originalAmount)
      .destination(destination)
      .build();

    payments.put(paymentId, payment);
  }

  @Override
  public void updatePaymentOnComplete(UUID paymentId, UnsignedLong amountSent, UnsignedLong amountDelivered, UnsignedLong amountLeftToSend, PaymentStatus status) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(amountSent, "amountSent must not be null");
    Objects.requireNonNull(amountDelivered, "amountDelivered must not be null");
    Objects.requireNonNull(status, "status must not be null");


    if (!payments.containsKey(paymentId)) {
      throw new HermesPaymentTrackerException("Payment does not exist! paymentId=" + paymentId);
    }

    Payment existingPayment = payments.get(paymentId);
    Payment updatedPayment = Payment.builder()
      .from(existingPayment)
      .amountSent(amountSent)
      .amountDelivered(amountDelivered)
      .amountLeftToSend(Objects.isNull(amountLeftToSend) ? existingPayment.originalAmount() : amountLeftToSend)
      .status(status)
      .build();

    payments.replace(paymentId, updatedPayment);
  }

  @Override
  public void updatePaymentOnError(UUID paymentId) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");

    this.updatePaymentOnComplete(paymentId, UnsignedLong.ZERO, UnsignedLong.ZERO, null, PaymentStatus.FAILED);
  }
}
