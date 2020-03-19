package org.interledger.spsp.server.controllers;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.AccessToken;
import org.interledger.spsp.server.client.ConnectorTokensClient;
import org.interledger.spsp.server.client.CreateAccessTokenResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.List;

import javax.servlet.http.HttpServletRequest;


@RestController
public class AccountTokenController {

  private ConnectorTokensClient tokensClient;

  public AccountTokenController(ConnectorTokensClient tokensClient) {
    this.tokensClient = tokensClient;
  }

  /**
   * Creates new access token for an account
   *
   * @param authorizationHeader The Authorization header as taken from the incoming {@link HttpServletRequest}.
   * @param accountId           The ILP Connector account identifier for this request.
   *
   * @return created token details
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public CreateAccessTokenResponse createToken(
    @RequestHeader(AUTHORIZATION) String authorizationHeader,
    @PathVariable("accountId") String accountId
  ) {
    return tokensClient.createToken(authorizationHeader, AccountId.of(accountId));
  }

  /**
   * Gets existing access tokens on an account
   *
   * @param authorizationHeader The Authorization header as taken from the incoming {@link HttpServletRequest}.
   * @param accountId           The ILP Connector account identifier for this request.
   *
   * @return list of tokens
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public List<AccessToken> getTokens(
    @RequestHeader(AUTHORIZATION) String authorizationHeader,
    @PathVariable("accountId") String accountId
  ) {
    return tokensClient.getTokens(authorizationHeader, AccountId.of(accountId));
  }

  /**
   * Deletes all tokens on an account
   *
   * @param authorizationHeader The Authorization header as taken from the incoming {@link HttpServletRequest}.
   * @param accountId           The ILP Connector account identifier for this request.
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.DELETE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void deleteTokens(
    @RequestHeader(AUTHORIZATION) String authorizationHeader,
    @PathVariable("accountId") String accountId
  ) {
    tokensClient.deleteTokens(authorizationHeader, AccountId.of(accountId));
  }

}
