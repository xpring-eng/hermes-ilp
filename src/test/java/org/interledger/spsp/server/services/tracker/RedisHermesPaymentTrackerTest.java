package org.interledger.spsp.server.services.tracker;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {AbstractRedisPaymentTrackerTest.Config.class})
public class RedisHermesPaymentTrackerTest extends AbstractRedisPaymentTrackerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Autowired
  private RedisHermesPaymentTracker paymentTracker;

  @Autowired
  private RedisTemplate<String, String> redisTemplate;

  @Override
  protected RedisTemplate getRedisTemplate() {
    return this.redisTemplate;
  }

  @Test
  public void registerPayment() {
    final UUID paymentId = UUID.randomUUID();
    registerPayment(paymentId);
  }

  private Payment registerPayment(UUID paymentId) {
    final AccountId senderAccountId = ACCOUNT_ID;
    final UnsignedLong originalAmount = UnsignedLong.valueOf(1000L);
    final PaymentPointer destination = PaymentPointer.of("$money.dev/foo");

    paymentTracker.registerPayment(paymentId, senderAccountId, originalAmount, destination);
    final Payment registeredPayment = paymentTracker.payment(paymentId);
    assertThat(registeredPayment.paymentId()).isEqualTo(paymentId);
    assertThat(registeredPayment.senderAccountId()).isEqualTo(senderAccountId);
    assertThat(registeredPayment.originalAmount()).isEqualTo(originalAmount);
    assertThat(registeredPayment.destination()).isEqualTo(destination);
    assertThat(registeredPayment.status()).isEqualTo(HermesPaymentTracker.PaymentStatus.PENDING);
    assertThat(registeredPayment.amountDelivered().longValue()).isEqualTo(UnsignedLong.ZERO.longValue());
    assertThat(registeredPayment.amountSent().longValue()).isEqualTo(UnsignedLong.ZERO.longValue());
    assertThat(registeredPayment.amountLeftToSend()).isEqualTo(originalAmount);

    return registeredPayment;
  }

  @Test
  public void updatePaymentOnComplete() {
    final UUID paymentId = UUID.randomUUID();
    final Payment registeredPayment = registerPayment(paymentId);

    UnsignedLong amountSent = UnsignedLong.valueOf(1000L);
    UnsignedLong amountDelivered = UnsignedLong.valueOf(1000L);
    UnsignedLong amountLeftToSend = UnsignedLong.valueOf(0L);
    HermesPaymentTracker.PaymentStatus status = HermesPaymentTracker.PaymentStatus.SUCCESSFUL;
    paymentTracker.updatePaymentOnComplete(paymentId, amountSent, amountDelivered, amountLeftToSend, status);

    final Payment updatedPayment = paymentTracker.payment(paymentId);
    assertThat(updatedPayment.paymentId()).isEqualTo(paymentId);
    assertThat(updatedPayment.senderAccountId()).isEqualTo(registeredPayment.senderAccountId());
    assertThat(updatedPayment.originalAmount()).isEqualTo(registeredPayment.originalAmount());
    assertThat(updatedPayment.destination()).isEqualTo(registeredPayment.destination());
    assertThat(updatedPayment.status()).isEqualTo(HermesPaymentTracker.PaymentStatus.SUCCESSFUL);
    assertThat(updatedPayment.amountDelivered()).isEqualTo(amountDelivered);
    assertThat(updatedPayment.amountSent()).isEqualTo(amountSent);
    assertThat(updatedPayment.amountLeftToSend()).isEqualTo(amountLeftToSend);
  }

  @Test
  public void getPaymentFailsPaymentDoesntExist() {
    expectedException.expect(HermesPaymentTrackerException.class);
    paymentTracker.payment(UUID.randomUUID());
  }
}
