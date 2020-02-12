package org.interledger.spsp.server.rest;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.JwtAuthSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.AbstractIntegrationTest;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.AccountSettingsResponse;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.controllers.AccountController;
import org.interledger.spsp.server.model.CreateAccountRestRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.Optional;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, AccountsRestControllerTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class AccountsRestControllerTests extends AbstractIntegrationTest {
  @Autowired
  private AccountController accountController;

  /**
   * Test that accounts created with a fully populated {@link org.interledger.spsp.server.model.CreateAccountRestRequest}
   * won't have anything overwritten
   */
  @Test
  public void testCreateAccountWithTokenAndFullRequest() {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder()
      .accountId("foo")
      .assetCode("USD")
      .assetScale(6)
      .build();

    AccountSettingsResponse response = accountController.createAccount(Optional.of("Bearer password"), Optional.of(request));
    assertThat(response.accountId()).isEqualTo(AccountId.of("foo"));
    assertThat(response.assetCode()).isEqualTo("USD");
    assertThat(response.assetScale()).isEqualTo(6);
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());
    assertThat(response.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN)).isEqualTo("password");
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(paymentPointerBase + "/foo"));
  }

  /**
   * Test that we can create an account with a specified token but default account settings
   */
  @Test
  public void testCreateAccountWithTokenButNoRequest() {
    AccountSettingsResponse response = accountController.createAccount(Optional.of("password"), Optional.empty());
    assertThat(response.accountId().value()).startsWith("user_");
    assertThat(response.assetCode()).isEqualTo("XRP");
    assertThat(response.assetScale()).isEqualTo(9);
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(paymentPointerBase + "/" + response.accountId()));
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
    assertThat(response.paymentPointer()).isEqualTo(PaymentPointer.of(paymentPointerBase + "/" + response.accountId()));
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

    JwtAuthSettings jwtAuthSettings = defaultAuthSettings(issuer);
    String jwt = jwtServer.createJwt(jwtAuthSettings, Instant.now().plusSeconds(10));

    CreateAccountRestRequest request = CreateAccountRestRequest.builder()
      .accountId(accountID)
      .assetCode("XRP")
      .assetScale(9)
      .description(accountDescription)
      .build();

    AccountSettingsResponse createdAccountSettings = accountController.createAccount(Optional.of(jwt), Optional.of(request));
    assertThat(createdAccountSettings.paymentPointer()).isEqualTo(PaymentPointer.of(paymentPointerBase + "/" + createdAccountSettings.accountId()));
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE)).isEqualTo(IlpOverHttpLinkSettings.AuthType.JWT_RS_256.toString());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER)).isEqualTo(jwtAuthSettings.tokenIssuer().get().toString());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE)).isEqualTo(jwtAuthSettings.tokenAudience().get());
    assertThat(createdAccountSettings.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT)).isEqualTo(jwtAuthSettings.tokenSubject());
  }

  public static class TestConfig {

    /**
     * Overrides the adminClient bean for test purposes to connect to our Connector container
     *
     * @return a ConnectorAdminClient that can speak to the test container connector
     */
    @Bean
    @Primary
    public ConnectorAdminClient adminClient() {
      return ConnectorAdminClient.construct(getInterledgerBaseUri(), template -> {
        template.header(AUTHORIZATION, "Basic " + ADMIN_AUTH_TOKEN);
      });
    }

    @Bean
    @Primary
    public ConnectorRoutesClient routesClient() {
      return ConnectorRoutesClient.construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
      });
    }
  }
}
