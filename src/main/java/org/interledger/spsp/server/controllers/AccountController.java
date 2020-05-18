package org.interledger.spsp.server.controllers;

import static org.interledger.spsp.server.services.HermesUtils.paymentPointerFromSpspUrl;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.payments.ListStreamPaymentsResponse;
import org.interledger.spsp.server.client.AccountSettingsResponse;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.model.CreateAccountRestRequest;
import org.interledger.spsp.server.services.NewAccountService;

import okhttp3.HttpUrl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Objects;
import java.util.Optional;

@Controller
public class AccountController {

  private final NewAccountService newAccountService;

  private final ConnectorAdminClient adminClient;

  private final HttpUrl spspReceiverUrl;

  public AccountController(
    NewAccountService newAccountService,
    ConnectorAdminClient adminClient,
    HttpUrl spspReceiverUrl
  ) {
    this.newAccountService = Objects.requireNonNull(newAccountService);
    this.adminClient = Objects.requireNonNull(adminClient);
    this.spspReceiverUrl = Objects.requireNonNull(spspReceiverUrl);
  }

  @RequestMapping(
    value = "/accounts", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  AccountSettingsResponse createAccount(
    @RequestHeader(AUTHORIZATION) Optional<BearerToken> authToken,
    @RequestBody Optional<CreateAccountRestRequest> createAccountRequest
  ) {
    // Give a choice of passing in a JWT or simple auth token, or having Hermes generate a Simple token
    AccountSettings accountSettings = newAccountService.createAccount(authToken, createAccountRequest);

    // Add a payment pointer to the response
    return AccountSettingsResponse.builder()
      .from(accountSettings)
      .paymentPointer(paymentPointerFromSpspUrl(spspReceiverUrl, accountSettings.accountId()))
      .build();
  }

  @RequestMapping(
    value = "/accounts/{accountId}",
    method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  AccountSettingsResponse getAccount(@PathVariable("accountId") AccountId accountId) {
    AccountSettings accountSettings = adminClient.findAccount(accountId.value())
      .orElseThrow(() -> new AccountNotFoundProblem(AccountId.of(accountId.value())));

    // Add a payment pointer to the response
    return AccountSettingsResponse.builder()
      .from(accountSettings)
      .paymentPointer(paymentPointerFromSpspUrl(spspReceiverUrl, accountSettings.accountId()))
      .build();
  }

  @RequestMapping(
    value = "/accounts/{accountId}/payments",
    method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public @ResponseBody
  ListStreamPaymentsResponse getAccountPayments(@PathVariable("accountId") AccountId accountId) {
    return adminClient.findAccountPayments(accountId.value());
  }

  @RequestMapping(
    value = "/accounts/rainmaker", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccountSettings createRainmaker() {
    return newAccountService.createRainmaker();
  }
}
