package org.interledger.spsp.server.services.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

public class InMemoryHermesPaymentTrackerTests {

  private HermesPaymentTracker hermesPaymentTracker;

  @Before
  public void setUp() {
    hermesPaymentTracker = new InMemoryHermesPaymentTracker();
  }

  @Test
  public void registerPayment() {
    UUID paymentId = UUID.randomUUID();
    Payment expected = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(UnsignedLong.valueOf(1000))
      .destination(PaymentPointer.of("$bar"))
      .build();
    registerPayment(expected);
  }

  private void registerPayment(Payment payment) {
    hermesPaymentTracker.registerPayment(payment.paymentId(),
      payment.senderAccountId(),
      payment.originalAmount(),
      payment.destination());

    Payment result = hermesPaymentTracker.payment(payment.paymentId());

    assertThat(result).isEqualToComparingFieldByField(payment);
  }

  @Test
  public void updatePaymentOnSuccessfulComplete() {
    UUID paymentId = UUID.randomUUID();
    UnsignedLong originalAmount = UnsignedLong.valueOf(1000);
    Payment registeredPayment = Payment.builder()
      .paymentId(paymentId)
      .status(HermesPaymentTracker.PaymentStatus.PENDING)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .destination(PaymentPointer.of("$bar"))
      .build();
    registerPayment(registeredPayment);

    Payment expected = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .amountSent(originalAmount)
      .amountDelivered(originalAmount)
      .amountLeftToSend(UnsignedLong.ZERO)
      .destination(PaymentPointer.of("$bar"))
      .status(HermesPaymentTracker.PaymentStatus.SUCCESSFUL)
      .build();

    hermesPaymentTracker.updatePaymentOnComplete(paymentId,
      originalAmount,
      originalAmount,
      UnsignedLong.ZERO,
      HermesPaymentTracker.PaymentStatus.SUCCESSFUL);

    Payment result = hermesPaymentTracker.payment(paymentId);
    assertThat(result).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void updatePaymentOnFailedComplete() {
    UUID paymentId = UUID.randomUUID();
    UnsignedLong originalAmount = UnsignedLong.valueOf(1000);
    Payment registeredPayment = Payment.builder()
      .paymentId(paymentId)
      .status(HermesPaymentTracker.PaymentStatus.PENDING)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .destination(PaymentPointer.of("$bar"))
      .build();
    registerPayment(registeredPayment);

    Payment expected = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .amountSent(UnsignedLong.ZERO)
      .amountDelivered(UnsignedLong.ZERO)
      .amountLeftToSend(originalAmount)
      .destination(PaymentPointer.of("$bar"))
      .status(HermesPaymentTracker.PaymentStatus.FAILED)
      .build();

    hermesPaymentTracker.updatePaymentOnComplete(paymentId,
      UnsignedLong.ZERO,
      UnsignedLong.ZERO,
      originalAmount,
      HermesPaymentTracker.PaymentStatus.FAILED);

    Payment result = hermesPaymentTracker.payment(paymentId);
    assertThat(result).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void updatePaymentOnError() {
    UUID paymentId = UUID.randomUUID();
    UnsignedLong originalAmount = UnsignedLong.valueOf(1000);
    Payment registeredPayment = Payment.builder()
      .paymentId(paymentId)
      .status(HermesPaymentTracker.PaymentStatus.PENDING)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .destination(PaymentPointer.of("$bar"))
      .build();
    registerPayment(registeredPayment);

    Payment expected = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(AccountId.of("foo"))
      .originalAmount(originalAmount)
      .amountSent(UnsignedLong.ZERO)
      .amountDelivered(UnsignedLong.ZERO)
      .amountLeftToSend(originalAmount)
      .destination(PaymentPointer.of("$bar"))
      .status(HermesPaymentTracker.PaymentStatus.FAILED)
      .build();

    hermesPaymentTracker.updatePaymentOnError(paymentId);
    Payment result = hermesPaymentTracker.payment(paymentId);
    assertThat(result).isEqualToComparingFieldByField(expected);
  }
}
