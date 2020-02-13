package org.interledger.spsp.server.config.tracker;

import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;
import org.interledger.spsp.server.services.tracker.InMemoryHermesPaymentTracker;
import org.interledger.spsp.server.services.tracker.RedisHermesPaymentTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuration for Hermes' PaymentTracker, including a Redis based tracker
 */
@Configuration
public class PaymentTrackerConfig {
  public static final String PAYMENT_TRACKING = "PAYMENT_TRACKING";

  public static final String PAYMENT_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME = "paymentTrackingJacksonRedisTemplate";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  protected Environment environment;

  @Autowired
  protected LettuceConnectionFactory lettuceConnectionFactory;

  @Bean(PAYMENT_TRACKING_JACKSON_REDIS_TEMPLATE_BEAN_NAME)
  @Qualifier(PAYMENT_TRACKING)
  protected RedisTemplate<String, ?> paymentTrackingRedisTemplate() {
    final RedisTemplate<String, ?> template = new RedisTemplate<>();

    template.setEnableDefaultSerializer(true);
    template.setDefaultSerializer(new StringRedisSerializer());
    template.setConnectionFactory(lettuceConnectionFactory);

    return template;
  }

  @Bean
  protected HermesPaymentTracker redisPaymentTracker(
    @Qualifier(PAYMENT_TRACKING) RedisTemplate<String, String> stringRedisTemplate,
    @Qualifier(PAYMENT_TRACKING) RedisTemplate<String, ?> jacksonRedisTemplate
  ) {
    try {

      // Try to connect to Redis, but default to InMemoryBalanceTracker if there's no Redis...
      if (stringRedisTemplate.getConnectionFactory().getConnection().ping().equalsIgnoreCase("PONG")) {
        return new RedisHermesPaymentTracker(
          registerPaymentScript(),
          updatePaymentOnCompleteScript(),
          stringRedisTemplate,
          jacksonRedisTemplate
        );
      }
    } catch (RedisConnectionFailureException e) {
      logger.warn(
        "WARNING: Using InMemoryBalanceTracker. Because this configuration is not durable, it should not be used in " +
          "production deployments. Configure RedisBalanceTracker instead. " +
          "HINT: is Redis running on its configured port, by default 6379?"
      );
      // If debug-output is enabled, then emit the stack-trace.
      if (logger.isDebugEnabled()) {
        logger.debug(e.getMessage(), e);
      }
    }

    // What do we want to do here? Should we have an in memory payment tracker?
    return new InMemoryHermesPaymentTracker();
  }

  @Bean
  protected RedisScript<Void> registerPaymentScript() {
    DefaultRedisScript<Void> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("META-INF/scripts/registerPaymentScript.lua"));
    script.setResultType(Void.class);
    return script;
  }

  @Bean
  protected RedisScript<Void> updatePaymentOnCompleteScript() {
    DefaultRedisScript<Void> script = new DefaultRedisScript<>();
    script.setLocation(new ClassPathResource("META-INF/scripts/updatePaymentOnCompleteScript.lua"));
    script.setResultType(Void.class);
    return script;
  }
}
