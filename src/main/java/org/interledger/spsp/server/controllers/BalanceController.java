package org.interledger.spsp.server.controllers;

import static org.interledger.spsp.server.services.AuthUtils.getAuthorizationAsBearerToken;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.ConnectorBalanceClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;


@RestController
public class BalanceController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private ConnectorBalanceClient balanceClient;

  public BalanceController(ConnectorBalanceClient balanceClient) {
    this.balanceClient = balanceClient;
  }

  /**
   * Gets the {@link AccountBalanceResponse} for the given {@code accountId}
   *
   * @param authorizationHeader The Authorization header as taken from the incoming {@link HttpServletRequest}.
   * @param accountId           The ILP Connector account identifier for this request.
   *
   * @return balance for account
   */
  @RequestMapping(
    value = "/accounts/{accountId}/balance", method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccountBalanceResponse getBalance(
    @RequestHeader(AUTHORIZATION) Optional<String> authorizationHeader,
    @PathVariable("accountId") String accountId
  ) {
    return balanceClient
      .getBalance("Bearer " + getAuthorizationAsBearerToken(authorizationHeader), AccountId.of(accountId));
  }

}
