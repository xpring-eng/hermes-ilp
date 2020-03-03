package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.AbstractIntegrationTest;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountSettingsResponse;
import org.interledger.spsp.server.client.CreateAccountRestRequest;

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
  classes = {HermesServerApplication.class, AccountsRestControllerUnitTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class AccountsRestControllerUnitTests extends AbstractIntegrationTest {

  /**
   * Test that accounts created with a fully populated {@link CreateAccountRestRequest}
   * won't have anything overwritten
   */
  @Test
  public void testCreateAccountWithTokenAndFullRequest() {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder("USD", 6)
      .accountId("foo")
      .build();

    AccountSettingsResponse response = accountController.createAccount(Optional.of("Bearer password"), Optional.of(request));
    assertThat(response.accountId()).isEqualTo(AccountId.of("foo"));
    assertThat(response.assetCode()).isEqualTo("USD");
    assertThat(response.assetScale()).isEqualTo(6);
    assertThat(response.linkType().value()).isEqualTo(IlpOverHttpLink.LINK_TYPE_STRING);
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).isEqualTo("password");
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/foo"));
  }

  /**
   * Test that we can create an account with a specified token but default account settings
   */
  @Test
  public void testCreateAccountWithTokenButNoRequest() {
    AccountSettingsResponse response = accountController.createAccount(Optional.of("Bearer password"), Optional.empty());
    assertThat(response.accountId().value()).startsWith("user_");
    assertThat(response.assetCode()).isEqualTo("XRP");
    assertThat(response.assetScale()).isEqualTo(9);
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/" + response.accountId()));
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().isEqualTo("password");
  }

  /**
   * Test that we can pass nothing and create a default account with a generated auth token
   */
  @Test
  public void testCreateAccountWithNoTokenAndNoRequest() {
    AccountSettingsResponse response = accountController.createAccount(Optional.empty(), Optional.empty());
    assertThat(response.accountId().value()).startsWith("user_");
    assertThat(response.assetCode()).isEqualTo("XRP");
    assertThat(response.assetScale()).isEqualTo(9);
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/" + response.accountId()));
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().doesNotStartWith("enc:jks");
  }

  @Test
  public void testCreateAccountWithOnlyAssetDetails() {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder("USD", 2).build();

    AccountSettingsResponse response = accountController.createAccount(Optional.empty(), Optional.of(request));
    assertThat(response.accountId().value()).startsWith("user_");
    assertThat(response.assetCode()).isEqualTo("USD");
    assertThat(response.assetScale()).isEqualTo(2);
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/" + response.accountId()));
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).asString().doesNotStartWith("enc:jks");
  }

  /**
   * Test that we can still create an account using a JWT and full account settings.
   * Tests compatibility with the xpring ilp wallet
   */
  @Test
  public void testCreateAccountWithJwtAndFullRequest() {
    String accountID = "AccountServiceGRPCTest";
    String accountDescription = "Noah's test account";

    JwtAuthSettings jwtAuthSettings = containers.defaultJwtAuthSettings(accountID);
    String jwt = containers.createJwt(accountID);

    CreateAccountRestRequest request = CreateAccountRestRequest.builder("XRP", 9)
      .accountId(accountID)
      .description(accountDescription)
      .build();

    AccountSettingsResponse createdAccountSettings = accountController.createAccount(Optional.of("Bearer " + jwt), Optional.of(request));
    assertThat(createdAccountSettings.paymentPointer()).isEqualTo(PaymentPointer.of(containers.paymentPointerBase() + "/" + createdAccountSettings.accountId()));
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER)).isEqualTo(jwtAuthSettings.tokenIssuer().get().toString());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE)).isEqualTo(jwtAuthSettings.tokenAudience().get());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT)).isEqualTo(jwtAuthSettings.tokenSubject());
  }

  @Component
  public static class TestConfig extends AbstractIntegrationTest.TestConfig {};

}
