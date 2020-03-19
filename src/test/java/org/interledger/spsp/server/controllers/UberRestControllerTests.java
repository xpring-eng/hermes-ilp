package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountSettingsResponse;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.model.CreateAccountRestRequest;
import org.interledger.spsp.server.model.PaymentRequest;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.Random;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, UberRestControllerTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class UberRestControllerTests extends AbstractIntegrationTest {

  @Test
  public void testPayScenarioWithSimpleAuth() {
    runFooPaysBarScenario(
      IlpOverHttpLinkSettings.AuthType.SIMPLE,
      BearerToken.fromRawToken("fooAuthToken"),
      BearerToken.fromRawToken("barAuthToken")
    );
  }

  @Test
  public void testPayScenarioWithJwtAuth() {
    runFooPaysBarScenario(
      IlpOverHttpLinkSettings.AuthType.JWT_RS_256,
      containers.createJwt(AccountId.of("foo")),
      containers.createJwt(AccountId.of("bar"))
    );
  }

  /**
   * Runs a scenario where Foo and Bar accounts are created, the balances of each are checked, Foo pays Bar, and then
   * the balances are checked again to validate payment occurred.
   *
   * @param fooToken auth token for Foo
   * @param barToken auth token for Bar
   */
  private void runFooPaysBarScenario(IlpOverHttpLinkSettings.AuthType authType, BearerToken fooToken,
    BearerToken barToken) {
    Random random = new Random();
    CreateAccountRestRequest foo = CreateAccountRestRequest.builder("XRP", 6)
      .accountId(AccountId.of("foo" + random.nextInt()))
      .build();

    CreateAccountRestRequest bar = CreateAccountRestRequest.builder("XRP", 6)
      .accountId(AccountId.of("bar" + random.nextInt()))
      .build();

    final Optional<BearerToken> fooAuthorizationHeader = Optional.of(fooToken);
    final Optional<BearerToken> barAuthorizationHeader = Optional.of(barToken);

    AccountSettingsResponse fooResponse = accountController.createAccount(fooAuthorizationHeader, Optional.of(foo));
    assertThat(fooResponse.accountId()).isEqualTo(foo.accountId());
    assertThat(fooResponse.assetCode()).isEqualTo("XRP");
    assertThat(fooResponse.assetScale()).isEqualTo(6);
    assertThat(fooResponse.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE))
      .isEqualTo(authType.toString());
    assertThat(fooResponse.paymentPointer())
      .isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/" + foo.accountId()));

    AccountSettingsResponse barResponse = accountController.createAccount(barAuthorizationHeader, Optional.of(bar));

    withAuthToken(fooToken, () ->
      assertThat(
        balanceController.getBalance(fooAuthorizationHeader, foo.accountId()).accountBalance().clearingBalance())
        .isEqualTo(0));

    withAuthToken(barToken, () ->
      assertThat(
        balanceController.getBalance(barAuthorizationHeader, bar.accountId()).accountBalance()
          .clearingBalance())
        .isEqualTo(0));

    withAuthToken(fooToken, () ->
      paymentController.sendPayment(fooAuthorizationHeader.get(), foo.accountId(), PaymentRequest.builder()
        .destinationPaymentPointer(barResponse.paymentPointer().toString())
        .amount(UnsignedLong.ONE)
        .build()
      )
    );

    withAuthToken(fooToken, () ->
      assertThat(
        balanceController.getBalance(fooAuthorizationHeader, foo.accountId()).accountBalance()
          .clearingBalance())
        .isEqualTo(-1));

    withAuthToken(barToken, () ->
      assertThat(
        balanceController.getBalance(barAuthorizationHeader, bar.accountId()).accountBalance()
          .clearingBalance())
        .isEqualTo(1));
  }

  @Configuration
  public static class TestConfig extends AbstractIntegrationTest.TestConfig {

  }

}
