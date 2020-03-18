package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.CreateAccessTokenResponse;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.model.CreateAccountRestRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, AccountTokensControllerTest.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class AccountTokensControllerTest extends AbstractIntegrationTest {

  /**
   * Test that accounts created with a fully populated {@link CreateAccountRestRequest} won't have anything overwritten
   */
  @Test
  public void testCRUD() {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder("USD", 6)
      .accountId("foo")
      .build();

    final String tokenValue = "password";
    final BearerToken authorizationHeader = BearerToken.fromRawToken(tokenValue);
    accountController.createAccount(Optional.of(authorizationHeader.value()), Optional.of(request));

    CreateAccessTokenResponse accessTokenResponse =
      withAuthToken(tokenValue, () -> tokenController.createToken(authorizationHeader.value(), request.accountId()));

    AccountBalanceResponse balanceResponse =
      withAuthToken(accessTokenResponse.rawToken(), () -> balanceController.getBalance(
        authorizationHeader.value(),
        request.accountId())
      );

    assertThat(withAuthToken(tokenValue, () -> tokenController.getTokens(
      authorizationHeader.value(), request.accountId()))
    ).hasSize(1);

    assertThat(balanceResponse.accountBalance()).isNotNull();

    withAuthToken(tokenValue, () -> {
      tokenController.deleteTokens(authorizationHeader.value(), request.accountId());
      assertThat(tokenController.getTokens(authorizationHeader.value(), request.accountId())).isEmpty();
      return null;
    });
  }

  @Component
  public static class TestConfig extends AbstractIntegrationTest.TestConfig {

  }

}
