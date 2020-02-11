package org.interledger.spsp.server.services.tracker;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Objects;
import java.util.UUID;

public class RedisHermesPaymentTracker implements HermesPaymentTracker {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final RedisScript<Long> registerPaymentScript;
  private final RedisScript<String> updatePaymentOnCompleteScript;
  private final RedisTemplate<String, String> stringRedisTemplate;
  private final RedisTemplate<String, ?> jacksonRedisTemplate;

  public RedisHermesPaymentTracker(RedisScript<Long> registerPaymentScript,
                                   RedisScript<String> updatePaymentOnCompleteScript,
                                   RedisTemplate<String, String> stringRedisTemplate,
                                   RedisTemplate<String, ?> jacksonRedisTemplate) {

    this.registerPaymentScript = Objects.requireNonNull(registerPaymentScript);
    this.updatePaymentOnCompleteScript = Objects.requireNonNull(updatePaymentOnCompleteScript);
    this.stringRedisTemplate = Objects.requireNonNull(stringRedisTemplate);
    this.jacksonRedisTemplate = Objects.requireNonNull(jacksonRedisTemplate);
  }

  @Override
  public void registerPayment(UUID paymentId,
                              AccountId senderAccountId,
                              UnsignedLong originalAmount,
                              PaymentPointer destination) {

  }

  @Override
  public void updatePaymentOnComplete(UUID paymentId,
                                      UnsignedLong amountSent,
                                      UnsignedLong amountDelivered,
                                      UnsignedLong amountLeftToSend,
                                      PaymentStatus status) {

  }
}
