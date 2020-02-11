package org.interledger.spsp.server.model;

import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonDeserialize(as = ImmutablePaymentResponse.class)
@JsonSerialize(as = ImmutablePaymentResponse.class)
public interface PaymentResponse {
  static ImmutablePaymentResponse.Builder builder() {
    return ImmutablePaymentResponse.builder();
  }

  UUID paymentId();
  UnsignedLong originalAmount();
  UnsignedLong amountDelivered();
  UnsignedLong amountSent();
  UnsignedLong amountLeftToSend();
  PaymentPointer destination();
  HermesPaymentTracker.PaymentStatus status();

}
