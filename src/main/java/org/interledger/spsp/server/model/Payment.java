package org.interledger.spsp.server.model;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.primitives.UnsignedLong;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutablePayment.class)
@JsonDeserialize(as = ImmutablePayment.class)
public interface Payment {

  static ImmutablePayment.Builder builder() {
    return ImmutablePayment.builder();
  }

  UUID paymentId();

  AccountId senderAccountId();

  UnsignedLong originalAmount();

  UnsignedLong amountDelivered();

  UnsignedLong amountSent();

  UnsignedLong amountLeftToSend();

  PaymentPointer destination();

  HermesPaymentTracker.PaymentStatus status();

}
