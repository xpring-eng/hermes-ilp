package org.interledger.spsp.server.services.tracker;

import static org.mockito.Mockito.mock;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.crypto.Decryptor;
import org.interledger.spsp.server.config.tracker.PaymentTrackerConfig;
import org.interledger.spsp.server.config.tracker.RedisConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import redis.embedded.RedisServerBuilder;

public abstract class AbstractRedisPaymentTrackerTest {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractRedisPaymentTrackerTest.class);
  protected static final int REDIS_PORT = 6379;

  protected static final AccountId ACCOUNT_ID = AccountId.of("1");

  protected static final boolean PRODUCES_NO_ERROR = false;
  protected static final boolean PRODUCES_ERROR = true;

  private static redis.embedded.RedisServer redisServer;

  @BeforeClass
  public static void startRedisServer() {
    try {
      RedisClient redisClient = RedisClient
        .create("redis://password@localhost:6379/");
      StatefulRedisConnection<String, String> connection
        = redisClient.connect();
      LOGGER.debug("Pinging Redis to check if its up...");
      connection.sync().ping();
      LOGGER.debug("Redis is running on port {}", REDIS_PORT);
    } catch (Exception e) {
      LOGGER.debug("Redis was NOT running on port {}. Using in-memory version instead.", REDIS_PORT);
      redisServer = new RedisServerBuilder().port(REDIS_PORT)
        //.setting("maxheap 128M")
        .build();
      redisServer.start();
    }
  }

  @AfterClass
  public static void stopRedisServer() {
    if (redisServer != null && redisServer.isActive()) {
      redisServer.stop();
    }
  }

  protected abstract RedisTemplate getRedisTemplate();

  @Configuration
  @Import({RedisConfig.class, PaymentTrackerConfig.class})
  static class Config {

    // For testing purposes, Redis is not secured with a password, so this implementation can be a no-op.
    @Bean
    Decryptor decryptor() {
      return (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[0];
    }


    @Bean
    ObjectMapper objectMapper() {
      return ObjectMapperFactory.create();
    }
  }
}
