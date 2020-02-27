package org.interledger.spsp.server.controllers.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;
import javax.servlet.http.Cookie;

/**
 * Test suite to test {@link CookieAuthenticationFilter}
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {AccountController.class, BalanceController.class, PaymentController.class},
  excludeAutoConfiguration = {SecurityAutoConfiguration.class})
public class CookieFilterTests extends AbstractControllerTest {

  @Autowired
  ApplicationContext applicationContext;

  @Autowired
  private MockMvc mvc;

  ObjectMapper objectMapper = ObjectMapperFactory.create();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCreateAccountWithCookieAuth() throws Exception {

    String authToken = "fakejwt.asdlfkjasdf.blah";
    Cookie authCookie = new Cookie("jwt", authToken);

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

    when(accountService.createAccount(eq(Optional.of("Bearer " + authToken)), any(Optional.class)))
      .thenReturn(accountSettingsMock);

    this.mvc.perform(post("/accounts")
      .headers(testJsonHeaders())
      .cookie(authCookie)
    )
      .andExpect(status().isOk());

    verify(accountService, times(1)).createAccount(eq(Optional.of("Bearer " + authToken)), any(Optional.class));
  }

  @Test
  public void testCreateAccountWithHeaderAuth() throws Exception {
    String authToken = "fakejwt.asdlfkjasdf.blah";

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

    when(accountService.createAccount(eq(Optional.of("Bearer " + authToken)), any(Optional.class)))
      .thenReturn(accountSettingsMock);

    this.mvc.perform(post("/accounts")
      .headers(testJsonHeaders())
      .header("Authorization", "Bearer " + authToken)
    )
      .andExpect(status().isOk());

    verify(accountService, times(1)).createAccount(eq(Optional.of("Bearer " + authToken)), any(Optional.class));
  }

  @Test
  public void testGetBalanceWithCookieAuth() throws Exception {
    String authToken = "fakejwt.asdlfkjasdf.blah";
    Cookie authCookie = new Cookie("jwt", authToken);

    AccountBalanceResponse accountBalanceResponseMock = AccountBalanceResponse.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of("foo"))
        .clearingBalance(1000)
        .prepaidAmount(10000)
        .build())
      .build();
    when(balanceClient.getBalance(eq("Bearer " + authToken), any())).thenReturn(accountBalanceResponseMock);

    this.mvc.perform(get("/accounts/foo/balance")
      .headers(testJsonHeaders())
      .cookie(authCookie)
    )
    .andExpect(status().isOk());

    verify(balanceClient, times(1)).getBalance(eq("Bearer " + authToken), any());
  }

  @Test
  public void testGetBalanceWithHeaderAuth() throws Exception {
    String authToken = "fakejwt.asdlfkjasdf.blah";

    AccountBalanceResponse accountBalanceResponseMock = AccountBalanceResponse.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of("foo"))
        .clearingBalance(1000)
        .prepaidAmount(10000)
        .build())
      .build();
    when(balanceClient.getBalance(eq("Bearer " + authToken), any())).thenReturn(accountBalanceResponseMock);

    this.mvc.perform(get("/accounts/foo/balance")
      .headers(testJsonHeaders())
      .header("Authorization", "Bearer " + authToken)
    )
      .andExpect(status().isOk());

    verify(balanceClient, times(1)).getBalance(eq("Bearer " + authToken), any());
  }

  @Test
  public void testSendMoneyWithCookieAuth() throws Exception {
    String authToken = "fakejwt";
    Cookie authCookie = new Cookie("jwt", authToken);

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
      eq("Bearer " + authToken),
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
      eq("Bearer " + authToken),
      any(),
      any());
  }

  @Test
  public void testSendMoneyWithHeaderAuth() throws Exception {
    String authToken = "fakejwt";

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
      eq("Bearer " + authToken),
      any(),
      any())).thenReturn(sendMoneyResultMock);

    PaymentRequest request = PaymentRequest.builder()
      .amount(UnsignedLong.valueOf(10))
      .destinationPaymentPointer("$foo.bar/baz")
      .build();
    this.mvc.perform(post("/accounts/foo/pay")
      .headers(testJsonHeaders())
      .header("Authorization", "Bearer " + authToken)
      .content(objectMapper.writeValueAsString(request))
    )
      .andExpect(status().isOk());

    verify(sendMoneyService, times(1)).sendMoney(any(),
      eq("Bearer " + authToken),
      any(),
      any());
  }
}
