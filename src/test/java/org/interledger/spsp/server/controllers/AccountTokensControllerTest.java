package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.CreateAccessTokenResponse;
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
   * Test that accounts created with a fully populated {@link CreateAccountRestRequest}
   * won't have anything overwritten
   */
  @Test
  public void testCRUD() {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder("USD", 6)
      .accountId("foo")
      .build();

    accountController.createAccount(Optional.of("Bearer password"), Optional.of(request));

    CreateAccessTokenResponse accessTokenResponse =
      withAuthToken("password", () -> tokenController.createToken(request.accountId()));

    AccountBalanceResponse balanceResponse =
      withAuthToken(accessTokenResponse.rawToken(), () -> balanceController.getBalance(request.accountId()));

    assertThat(withAuthToken("password", () -> tokenController.getTokens(request.accountId())))
      .hasSize(1);

    assertThat(balanceResponse.accountBalance()).isNotNull();

    withAuthToken("password", () -> {
      tokenController.deleteTokens(request.accountId());
      assertThat(tokenController.getTokens(request.accountId())).isEmpty();
      return null;
    });
  }

  @Component
  public static class TestConfig extends AbstractIntegrationTest.TestConfig {};

}
