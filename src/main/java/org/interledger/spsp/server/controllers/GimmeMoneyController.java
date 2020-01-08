package org.interledger.spsp.server.controllers;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.services.GimmeMoneyService;

import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.zalando.problem.spring.common.MediaTypes;


/**
 *
 */
@RestController
public class GimmeMoneyController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final GimmeMoneyService gimmeMoneyService;

  public GimmeMoneyController(GimmeMoneyService gimmeMoneyService) {
    this.gimmeMoneyService = gimmeMoneyService;
  }

  /**
   * Gets the {@link AccountBalanceResponse} for the given {@code accountId}
   *
   * @param accountId
   * @return balance for account
   */
  @RequestMapping(
    value = "/accounts/{accountId}/money", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public UnsignedLong getBalance(@PathVariable("accountId") String accountId) {
    try {
      UnsignedLong amount = UnsignedLong.valueOf(100000);
      return gimmeMoneyService.gimmeMoney(AccountId.of(accountId), amount);
    }
    catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
