package org.interledger.spsp.server.controllers;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.AccessToken;
import org.interledger.spsp.server.client.ConnectorTokensClient;
import org.interledger.spsp.server.client.CreateAccessTokenResponse;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.List;


@RestController
public class AccountTokenController extends AbstractController {

  private ConnectorTokensClient tokensClient;

  public AccountTokenController(ConnectorTokensClient tokensClient) {
    this.tokensClient = tokensClient;
  }

  /**
   * Creates new access token for an account
   *
   * @param accountId
   *
   * @return created token details
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public CreateAccessTokenResponse createToken(@PathVariable("accountId") String accountId) {
    return tokensClient.createToken("Bearer " + getAuthorization(), AccountId.of(accountId));
  }

  /**
   * Gets existing access tokens on an account
   *
   * @param accountId
   *
   * @return list of tokens
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.GET},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public List<AccessToken> getTokens(@PathVariable("accountId") String accountId) {
    return tokensClient.getTokens("Bearer " + getAuthorization(), AccountId.of(accountId));
  }

  /**
   * Deletes all tokens on an account
   *
   * @param accountId
   */
  @RequestMapping(
    value = "/accounts/{accountId}/tokens", method = {RequestMethod.DELETE},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void deleteTokens(@PathVariable("accountId") String accountId) {
    tokensClient.deleteTokens("Bearer " + getAuthorization(), AccountId.of(accountId));
  }

}
