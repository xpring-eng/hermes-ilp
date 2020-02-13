package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.Payment;

import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class RedisHermesPaymentTracker implements HermesPaymentTracker {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final RedisScript<Void> registerPaymentScript;
  private final RedisScript<Void> updatePaymentOnCompleteScript;
  private final RedisTemplate<String, String> stringRedisTemplate;
  private final RedisTemplate<String, ?> jacksonRedisTemplate;

  public RedisHermesPaymentTracker(RedisScript<Void> registerPaymentScript,
                                   RedisScript<Void> updatePaymentOnCompleteScript,
                                   RedisTemplate<String, String> stringRedisTemplate,
                                   RedisTemplate<String, ?> jacksonRedisTemplate) {

    this.registerPaymentScript = Objects.requireNonNull(registerPaymentScript);
    this.updatePaymentOnCompleteScript = Objects.requireNonNull(updatePaymentOnCompleteScript);
    this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate);
    this.jacksonRedisTemplate = Objects.requireNonNull(jacksonRedisTemplate);
  }

  @Override
  public Payment payment(UUID paymentId) {
    Objects.requireNonNull(paymentId);

    final BoundHashOperations<String, String, String> result =
      stringRedisTemplate.boundHashOps(toRedisPaymentKey(paymentId));

    if (Objects.nonNull(result.entries()) && result.entries().isEmpty()) {
      throw new HermesPaymentTrackerException("Payment does not exist! paymentId=" + paymentId);
    }

    return Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(AccountId.of(result.get("sender_account_id")))
      .originalAmount(toUnsignedLong(result.get("original_amount")))
      .amountSent(toUnsignedLong(result.get("amount_sent")))
      .amountDelivered(toUnsignedLong(result.get("amount_delivered")))
      .amountLeftToSend(toUnsignedLong(result.get("amount_left_to_send")))
      .destination(PaymentPointer.of(result.get("destination")))
      .status(PaymentStatus.valueOf(result.get("status")))
      .build();
  }

  @Override
  public void registerPayment(final UUID paymentId,
                              final AccountId senderAccountId,
                              final UnsignedLong originalAmount,
                              final PaymentPointer destination) throws HermesPaymentTrackerException {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(senderAccountId, "senderAccountId must not be null");
    Objects.requireNonNull(originalAmount, "originalAmount must not be null");
    Objects.requireNonNull(destination, "destination must not be null");

    Preconditions.checkArgument(originalAmount.longValue() >= 0, String.format("originalAmount `%s` cannot be negative!", originalAmount));

    try {
      stringRedisTemplate.execute(
        registerPaymentScript,
        Collections.singletonList(toRedisPaymentKey(paymentId)),
        // Arg1: senderAccountId
        senderAccountId.value() + "",
        // Arg2: originalAmount : amount to send
        originalAmount.longValue() + "",
        // Arg3: amountLeftToSend : initially originalAmount
        originalAmount.longValue() + "",
        // Arg3: destination payment pointer,
        destination.toString()
      );

      logger.debug("Registered pending payment. paymentId={} senderId={} amount={} destination={}",
        paymentId, senderAccountId, originalAmount, destination
      );
    } catch (Exception e) {
      final String errorMessage = String.format("Error registering payment in Redis. paymentId=%s senderId=%s amount=%s destination=%s",
        paymentId, senderAccountId, originalAmount, destination
        );
      throw new HermesPaymentTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updatePaymentOnComplete(UUID paymentId,
                                      UnsignedLong amountSent,
                                      UnsignedLong amountDelivered,
                                      UnsignedLong amountLeftToSend,
                                      PaymentStatus status) throws HermesPaymentTrackerException {
    Objects.requireNonNull(paymentId, "paymentId must not be null");
    Objects.requireNonNull(amountSent, "amountSent must not be null");
    Objects.requireNonNull(amountDelivered, "amountDelivered must not be null");
    Objects.requireNonNull(status, "status must not be null");

    // Make sure the payment exists. This will throw an exception if it doesnt exist
    this.payment(paymentId);

    try {
      stringRedisTemplate.execute(
        updatePaymentOnCompleteScript,
        Collections.singletonList(toRedisPaymentKey(paymentId)),
        // Arg1 : Amount sent
        amountSent.longValue() + "",
        // Arg2 : Amount delivered
        amountDelivered.longValue() + "",
        // Arg3 : Amount left to send
        amountLeftToSend.longValue() + "",
        // Arg4 : Resulting status of payment
        status.toString()
      );

      logger.debug("Updated payment. paymentId={} amountSent={} amountDelivered={} amountLeftToSend={} status={}",
        paymentId, amountSent, amountDelivered, amountLeftToSend, status);
    } catch (Exception e) {
      final String errorMessage =
        String.format("Error updating payment in Redis. paymentId=%s amountSent=%s amountDelivered=%s amountLeftToSend=%s status=%s",
          paymentId, amountSent, amountDelivered, amountLeftToSend, status
        );
      throw new HermesPaymentTrackerException(errorMessage, e);
    }
  }

  @Override
  public void updatePaymentOnError(UUID paymentId) {
    Objects.requireNonNull(paymentId, "paymentId must not be null");

    updatePaymentOnComplete(paymentId, UnsignedLong.ZERO, UnsignedLong.ZERO, UnsignedLong.ZERO, PaymentStatus.FAILED);
  }

  private String toRedisPaymentKey(final UUID paymentId) {
    return "payments:" + paymentId.toString();
  }

  private UnsignedLong toUnsignedLong(String numAsString) {
    if (numAsString == null || numAsString.isEmpty()) {
      return UnsignedLong.ZERO;
    }

    return UnsignedLong.valueOf(numAsString);
  }
}
