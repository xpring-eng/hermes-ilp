package org.interledger.spsp.server.model;

import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

/**
 * Sent by a client to Hermes to initiate a payment
 */
@Value.Immutable
@JsonDeserialize(as = ImmutablePaymentRequest.class)
@JsonSerialize(as = ImmutablePaymentRequest.class)
public interface PaymentRequest {

  static ImmutablePaymentRequest.Builder builder() {
    return ImmutablePaymentRequest.builder();
  }

  /**
   *
   * @return Payment pointer of the receiver
   */
  PaymentPointer destinationPaymentPointer();

  /**
   *
   * @return An {@link UnsignedLong} representing the amount to send to the receiver,
   *          denominated in the sender's account denomination
   */
  UnsignedLong amount();

}
