package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;
import org.interledger.stream.SendMoneyResult;

import com.google.common.primitives.UnsignedLong;

import java.util.UUID;

/**
 * Service responsible for keeping track of {@link Payment}s that come through Hermes
 * in order to enable idempotent payment requests
 */
public interface HermesPaymentTracker {

  /**
   * Retrieve the {@link Payment} associated with paymentId
   *
   * @param paymentId : {@link UUID} of the payment
   * @return a {@link Payment} containing details about the status of the payment
   */
  Payment payment(final UUID paymentId);

  /**
   * Preemptively save a payment before initiating a STREAM payment so that
   * clients can retrieve the payment by paymentId later on.
   *
   * @param paymentId : {@link UUID} of the payment
   * @param senderAccountId : {@link AccountId} of the sender
   * @param originalAmount : Amount the sender requested to send denominated in their asset scale
   * @param destination : {@link PaymentPointer} of the receiver
   *
   * @throws HermesPaymentTrackerException
   */
  void registerPayment(final UUID paymentId,
                       final AccountId senderAccountId,
                       final UnsignedLong originalAmount,
                       final PaymentPointer destination)
    throws HermesPaymentTrackerException;

  /**
   * After a payment is complete (in this implementation, when {@link org.interledger.stream.sender.SimpleStreamSender#sendMoney}
   * returns), the payment associated with the given paymentId will get updated with metadata
   * from the {@link org.interledger.stream.SendMoneyResult} that is returned.
   *
   * Status will be set to SUCCESSFUL if {@link SendMoneyResult#successfulPayment()} is true, otherwise it will be set to
   * FAILED
   *
   * @param paymentId : {@link UUID} of the payment to be updated
   * @param amountSent : The actual amount, in the senders units, that was sent to the receiver. In the case, of a timeout or rejected
   *                   packets this amount may be less than the requested amount to be sent.
   * @param amountDelivered : The actual amount, in the receivers units, that was delivered to the receiver. Any currency conversion and/or
   *                        connector fees may cause this to be different than the amount sent.
   * @param amountLeftToSend : The actual amount, in the senders units, that is still left to send. If the payment was successful, this amount
   *                         will be 0. If there was an issue sending payment (repeated rejections or timeouts), this will be the amount that
   *                         could not be sent or which may have been sent but we failed to receive the fulfillment packet (network issue).
   * @param status : Status of the payment
   *
   * @throws HermesPaymentTrackerException
   */
  void updatePaymentOnComplete(UUID paymentId,
                               final UnsignedLong amountSent,
                               final UnsignedLong amountDelivered,
                               final UnsignedLong amountLeftToSend,
                               PaymentStatus status)
    throws HermesPaymentTrackerException;

  /**
   * If an exception occurs during a payment, this method will update the payment associated with paymentId.
   * Because {@link org.interledger.stream.sender.SimpleStreamSender} is fairly opaque, and because there is no way
   * currently to reconcile what occurred within the ILP network, all resulting payment data will be set to 0
   *
   * @param paymentId : {@link UUID} associated with the payment that failed
   */
  void updatePaymentOnError(UUID paymentId);

  /**
   * Enum defining different statuses a payment can have while being tracked
   */
  enum PaymentStatus {
    PENDING,
    SUCCESSFUL,
    FAILED
  }
}
