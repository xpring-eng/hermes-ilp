package org.interledger.spsp.server.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.UUID;

/**
 * Represents a payment sent through Hermes, which Hermes tracks via {@link HermesPaymentTracker}
 */
@Value.Immutable
@JsonSerialize(as = ImmutablePayment.class)
@JsonDeserialize(as = ImmutablePayment.class)
public interface Payment {

  static ImmutablePayment.Builder builder() {
    return ImmutablePayment.builder();
  }

  /**
   * Clients are required to provide a paymentId in the form of a {@link UUID}
   * as a unique identifier of their payment. If this task were left up to Hermes,
   * there is a chance the client could disconnect from Hermes while the payment is being registered,
   * and the client would never get a paymentId back.
   *
   * @return Unique identifier of this payment
   */
  UUID paymentId();

  /**
   *
   * @return {@link AccountId} of the sender on the connector
   */
  AccountId senderAccountId();

  /**
   * The original amount that was requested to be sent.
   *
   * @return An {@link UnsignedLong} representing the original amount to be sent in a given STREAM payment.
   */
  UnsignedLong originalAmount();

  /**
   * The actual amount, in the receivers units, that was delivered to the receiver. Any currency conversion and/or
   * connector fees may cause this to be different than the amount sent.
   *
   * Defaults to {@link UnsignedLong#ZERO}, as no amount has been delivered upon initial storage of the payment
   *
   * @return An {@link UnsignedLong} representing the amount delivered.
   */
  @Value.Default
  default UnsignedLong amountDelivered() {
    return UnsignedLong.ZERO;
  };

  /**
   * The actual amount, in the senders units, that was sent to the receiver. In the case, of a timeout or rejected
   * packets this amount may be less than the requested amount to be sent.
   *
   * @return An {@link UnsignedLong} representing the amount sent.
   */
  @Value.Default
  default UnsignedLong amountSent() {
    return UnsignedLong.ZERO;
  };

  /**
   * The actual amount, in the senders units, that is still left to send. If the payment was successful, this amount
   * will be 0. If there was an issue sending payment (repeated rejections or timeouts), this will be the amount that
   * could not be sent or which may have been sent but we failed to receive the fulfillment packet (network issue).
   *
   * @return An {@link UnsignedLong} representing the amount left to send.
   */
  @Value.Default
  default UnsignedLong amountLeftToSend() {
    return this.originalAmount();
  };

  /**
   *
   * @return Payment pointer of the receiver of this payment
   */
  PaymentPointer destination();

  /**
   * Status will be PENDING when Hermes receives a payment request, SUCCESSFUL if a successful payment was sent,
   * or FAILED if the payment completed but was unsuccessful or if an exception occurred during the send
   *
   * @return the current status of the payment, as seen from Hermes' point of view
   */
  @Value.Default
  default HermesPaymentTracker.PaymentStatus status() {
    return HermesPaymentTracker.PaymentStatus.PENDING;
  };

}
