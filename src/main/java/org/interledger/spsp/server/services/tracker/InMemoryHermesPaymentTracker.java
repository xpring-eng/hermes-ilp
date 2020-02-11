package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.google.common.primitives.UnsignedLong;

import java.util.UUID;

public class InMemoryHermesPaymentTracker implements HermesPaymentTracker {

  @Override
  public void registerPayment(UUID paymentId, AccountId senderAccountId, UnsignedLong originalAmount, PaymentPointer destination) {

  }

  @Override
  public void updatePaymentOnComplete(UUID paymentId, UnsignedLong amountSent, UnsignedLong amountDelivered, UnsignedLong amountLeftToSend, PaymentStatus status) {

  }
}
