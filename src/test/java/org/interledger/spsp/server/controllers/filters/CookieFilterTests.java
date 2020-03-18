package org.interledger.spsp.server.controllers.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.server.client.AccountBalance;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;
import org.interledger.spsp.server.controllers.AbstractControllerTest;
import org.interledger.spsp.server.controllers.AccountController;
import org.interledger.spsp.server.controllers.BalanceController;
import org.interledger.spsp.server.controllers.PaymentController;
import org.interledger.spsp.server.controllers.filters.CookieFilterTests.CookieFilterConfiguration;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.model.PaymentRequest;
import org.interledger.stream.SendMoneyResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.http.Cookie;

/**
 * Test suite to test {@link CookieAuthenticationFilter}
 */
@RunWith(SpringRunner.class)
@WebMvcTest(
  controllers = {AccountController.class, BalanceController.class, PaymentController.class},
  excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@Import(CookieFilterConfiguration.class)
public class CookieFilterTests extends AbstractControllerTest {

  private static final String JWT_VALUE = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0"
    + ".dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";

  private static final BearerToken BEARER_TOKEN = BearerToken.fromRawToken(JWT_VALUE);
  private static final Optional<BearerToken> OPT_BEARER_TOKEN = Optional.of(BEARER_TOKEN);

  private ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Autowired
  private MockMvc mvc;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateAccountWithCookieAuth() throws Exception {
    Cookie authCookie = new Cookie("jwt", JWT_VALUE);

    AccountSettings accountSettingsMock = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetCode("XRP")
      .assetScale(9)
      .balanceSettings(AccountBalanceSettings.builder().build())
      .settlementEngineDetails(Optional.empty())
      .rateLimitSettings(AccountRateLimitSettings.builder().build())
      .maximumPacketAmount(UnsignedLong.valueOf(1000))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    when(accountService.createAccount(eq(OPT_BEARER_TOKEN), any(Optional.class)))
      .thenReturn(accountSettingsMock);

    this.mvc.perform(post("/accounts")
      .headers(testJsonHeaders())
      .cookie(authCookie)
    )
      .andExpect(status().isOk());

    verify(accountService, times(1)).createAccount(eq(OPT_BEARER_TOKEN), any(Optional.class));
  }

  @Test
  public void testCreateAccountWithHeaderAuth() throws Exception {
    AccountSettings accountSettingsMock = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetCode("XRP")
      .assetScale(9)
      .balanceSettings(AccountBalanceSettings.builder().build())
      .settlementEngineDetails(Optional.empty())
      .rateLimitSettings(AccountRateLimitSettings.builder().build())
      .maximumPacketAmount(UnsignedLong.valueOf(1000))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    when(accountService.createAccount(eq(OPT_BEARER_TOKEN), any(Optional.class)))
      .thenReturn(accountSettingsMock);

    this.mvc.perform(post("/accounts")
      .headers(testJsonHeaders())
      .header(AUTHORIZATION, BEARER_TOKEN.toString())
    )
      .andExpect(status().isOk());

    verify(accountService, times(1)).createAccount(eq(OPT_BEARER_TOKEN), any(Optional.class));
  }

  /**
   * When a client sends a create account request with both an "Authorization" header and a jwt cookie, the
   * CookieAuthenticationFilter should give precedence to the "Authorization" header
   */
  @Test
  public void testCreateAccountWithCookieAndAuthHeader() throws Exception {
    BearerToken headerAuthBearerToken = BearerToken.fromRawToken("shh");
    String cookieAuthBearerTokenValue = JWT_VALUE;
    Cookie authCookie = new Cookie("jwt", cookieAuthBearerTokenValue);

    AccountSettings accountSettingsMock = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .assetCode("XRP")
      .assetScale(9)
      .balanceSettings(AccountBalanceSettings.builder().build())
      .settlementEngineDetails(Optional.empty())
      .rateLimitSettings(AccountRateLimitSettings.builder().build())
      .maximumPacketAmount(UnsignedLong.valueOf(1000))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    when(accountService.createAccount(any(Optional.class), any(Optional.class)))
      .thenReturn(accountSettingsMock);

    this.mvc.perform(post("/accounts")
      .headers(testJsonHeaders())
      .header(AUTHORIZATION, headerAuthBearerToken)
      .cookie(authCookie))
      .andExpect(status().isOk());

    // createAccount should have been called with the header auth, not the cookie auth
    verify(accountService).createAccount(eq(Optional.of(headerAuthBearerToken)), any(Optional.class));
    verifyNoMoreInteractions(accountService);
  }

  @Test
  public void testGetBalanceWithCookieAuth() throws Exception {
    Cookie authCookie = new Cookie("jwt", BEARER_TOKEN.rawToken());

    AccountBalanceResponse accountBalanceResponseMock = AccountBalanceResponse.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of("foo"))
        .clearingBalance(1000)
        .prepaidAmount(10000)
        .build())
      .build();
    when(balanceClient.getBalance(eq(BEARER_TOKEN.toString()), any())).thenReturn(accountBalanceResponseMock);

    this.mvc.perform(get("/accounts/foo/balance")
      .headers(testJsonHeaders())
      .cookie(authCookie)
    )
      .andExpect(status().isOk());

    verify(balanceClient, times(1)).getBalance(eq(BEARER_TOKEN.toString()), any());
  }

  @Test
  public void testGetBalanceWithHeaderAuth() throws Exception {
    BearerToken bearerToken = BearerToken.fromRawToken("fakejwt.asdlfkjasdf.blah");

    AccountBalanceResponse accountBalanceResponseMock = AccountBalanceResponse.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of("foo"))
        .clearingBalance(1000)
        .prepaidAmount(10000)
        .build())
      .build();
    when(balanceClient.getBalance(eq(bearerToken.toString()), any())).thenReturn(accountBalanceResponseMock);

    this.mvc.perform(get("/accounts/foo/balance")
      .headers(testJsonHeaders())
      .header(AUTHORIZATION, bearerToken.toString())
    )
      .andExpect(status().isOk());

    verify(balanceClient, times(1)).getBalance(eq(bearerToken.toString()), any());
  }

  @Test
  public void testSendMoneyWithCookieAuth() throws Exception {
    Cookie authCookie = new Cookie("jwt", JWT_VALUE);

    SendMoneyResult sendMoneyResultMock = SendMoneyResult.builder()
      .originalAmount(UnsignedLong.valueOf(10))
      .amountDelivered(UnsignedLong.valueOf(10))
      .amountSent(UnsignedLong.valueOf(10))
      .amountLeftToSend(UnsignedLong.valueOf(10))
      .numFulfilledPackets(10)
      .numRejectPackets(10)
      .sendMoneyDuration(Duration.ZERO)
      .successfulPayment(true)
      .build();

    when(sendMoneyService.sendMoney(any(),
      eq(BEARER_TOKEN),
      any(),
      any())).thenReturn(sendMoneyResultMock);

    PaymentRequest request = PaymentRequest.builder()
      .amount(UnsignedLong.valueOf(10))
      .destinationPaymentPointer("$foo.bar/baz")
      .build();
    this.mvc.perform(post("/accounts/foo/pay")
      .headers(testJsonHeaders())
      .cookie(authCookie)
      .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isOk());

    verify(sendMoneyService, times(1)).sendMoney(any(),
      eq(BEARER_TOKEN),
      any(),
      any());
  }

  @Test
  public void testSendMoneyWithHeaderAuth() throws Exception {
    SendMoneyResult sendMoneyResultMock = SendMoneyResult.builder()
      .originalAmount(UnsignedLong.valueOf(10))
      .amountDelivered(UnsignedLong.valueOf(10))
      .amountSent(UnsignedLong.valueOf(10))
      .amountLeftToSend(UnsignedLong.valueOf(10))
      .numFulfilledPackets(10)
      .numRejectPackets(10)
      .sendMoneyDuration(Duration.ZERO)
      .successfulPayment(true)
      .build();

    when(sendMoneyService.sendMoney(any(),
      eq(BEARER_TOKEN),
      any(),
      any())).thenReturn(sendMoneyResultMock);

    PaymentRequest request = PaymentRequest.builder()
      .amount(UnsignedLong.valueOf(10))
      .destinationPaymentPointer("$foo.bar/baz")
      .build();
    this.mvc.perform(post("/accounts/foo/pay")
      .headers(testJsonHeaders())
      .header(AUTHORIZATION, BEARER_TOKEN.value())
      .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isOk());

    verify(sendMoneyService, times(1)).sendMoney(any(),
      eq(BEARER_TOKEN),
      any(),
      any());
  }

  static class CookieFilterConfiguration {

    @Bean
    public BuildProperties buildProperties() {
      return new BuildProperties(new Properties());
    }
  }
}
