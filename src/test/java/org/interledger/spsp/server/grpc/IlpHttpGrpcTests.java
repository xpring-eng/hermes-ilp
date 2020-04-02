package org.interledger.spsp.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.controllers.AbstractIntegrationTest;
import org.interledger.spsp.server.grpc.auth.IlpCallCredentials;
import org.interledger.spsp.server.grpc.auth.IlpGrpcMetadataReader;
import org.interledger.spsp.server.grpc.utils.InterceptedService;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.services.NewAccountService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, IlpHttpGrpcTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class IlpHttpGrpcTests extends AbstractIntegrationTest {

  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Autowired
  IlpOverHttpGrpcHandler ilpOverHttpGrpcHandler;
  @Autowired
  IlpGrpcMetadataReader ilpGrpcMetadataReader;
  private Logger logger = LoggerFactory.getLogger(this.getClass());
  private IlpOverHttpServiceGrpc.IlpOverHttpServiceBlockingStub blockingStub;
  @Autowired
  private NewAccountService newAccountService;

  @Autowired
  private ConnectorBalanceClient balanceClient;

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Autowired
  private OutgoingLinkSettings outgoingLinkSettings;

  @Before
  public void startUp() throws IOException {
    createTestAccount(AccountId.of("alice"));
    createTestAccount(AccountId.of("bob"));
    registerGrpc();
  }

  public void registerGrpc() throws IOException {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(
      InProcessServerBuilder
        .forName(serverName)
        .directExecutor()
        .addService(InterceptedService.of(ilpOverHttpGrpcHandler, ilpGrpcMetadataReader))
        .build()
        .start()
    );

    blockingStub = IlpOverHttpServiceGrpc.newBlockingStub(
      // Create a client channel and register for automatic graceful shutdown.
      grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));
  }

  private void createTestAccount(AccountId jwtSubject) {
    JwtAuthSettings jwtAuthSettings = containers.defaultJwtAuthSettings(jwtSubject);
    createTestAccount(jwtAuthSettings);
  }

  private void createTestAccount(JwtAuthSettings jwtAuthSettings) {
    // Set up auth settings to use JWT_RS_256
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwtAuthSettings.tokenIssuer().get().toString());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwtAuthSettings.tokenAudience().get());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwtAuthSettings.tokenSubject());
    customSettings.putAll(outgoingLinkSettings.toCustomSettingsMap()
      .entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))
    );

    try {
      this.newAccountService.createAccount(
        AccountSettings.builder()
          .accountId(AccountId.of(jwtAuthSettings.tokenSubject()))
          .assetCode("XRP")
          .assetScale(9)
          .linkType(IlpOverHttpLink.LINK_TYPE)
          .accountRelationship(AccountRelationship.CHILD)
          .customSettings(customSettings)
          .build()
      );
    } catch (FeignException e) {
      if (e.status() != 409) {
        throw e;
      } else {
        logger.warn(
          "Hermes account already exists. If you want to update the account, delete it and try again with new settings.");
      }
    }
  }

  @Test
  public void sendMoneyTest() throws JsonProcessingException {
    int sendAmount = 10000;
    BearerToken aliceJwt = containers.createJwt(AccountId.of("alice"), 10);

    SendPaymentRequest sendMoneyRequest = SendPaymentRequest.newBuilder()
      .setAccountId("alice")
      .setAmount(sendAmount)
      .setDestinationPaymentPointer(containers.paymentPointerBase() + "/bob")
      .build();

    SendPaymentResponse response = blockingStub
      .withCallCredentials(IlpCallCredentials.build(aliceJwt))
      .sendMoney(sendMoneyRequest);
    if (!response.getSuccessfulPayment()) {
      fail();
    }
    logger.info("Payment sent successfully!  Payment Response: ");
    logger.info(response.toString());

    SendPaymentResponse expected = SendPaymentResponse.newBuilder()
      .setAmountSent(sendAmount)
      .setOriginalAmount(sendAmount)
      .setAmountDelivered(sendAmount)
      .setAmountSent(sendAmount)
      .setSuccessfulPayment(true)
      .build();

    assertThat(response).isEqualToComparingFieldByField(expected);

    BearerToken bobJwt = containers.createJwt(AccountId.of("bob"), 10);
    AccountBalanceResponse aliceBalance = balanceClient.getBalance(Optional.of(aliceJwt), AccountId.of("alice"));
    AccountBalanceResponse bobBalance = balanceClient.getBalance(Optional.of(bobJwt), AccountId.of("bob"));

    logger.info("Alice's balance after sending payment: ");
    logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aliceBalance));

    logger.info("Bob's balance after receiving payment: ");
    logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(bobBalance));

    assertThat(aliceBalance.accountBalance().netBalance())
      .isEqualTo(bobBalance.accountBalance().netBalance().negate());
    assertThat(aliceBalance.accountBalance().netBalance()).isEqualTo(BigInteger.valueOf(-sendAmount));
  }

  @Configuration
  public static class TestConfig extends AbstractIntegrationTest.TestConfig {

  }
}


