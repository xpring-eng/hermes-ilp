package org.interledger.spsp.server.controllers;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.model.ImmutablePaymentRequest;
import org.interledger.spsp.server.model.Payment;
import org.interledger.spsp.server.model.PaymentResponse;
import org.interledger.spsp.server.services.SendMoneyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.UUID;


@RestController
public class PaymentController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final SendMoneyService sendMoneyService;

  public PaymentController(SendMoneyService sendMoneyService) {
    this.sendMoneyService = sendMoneyService;
  }

  /**
   * Sends payment from the given account.
   *
   * @param accountId
   * @param paymentRequest
   * @return payment result
   */
  @RequestMapping(
    value = "/accounts/{accountId}/payments/{paymentId}", method = {RequestMethod.PUT},
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public Payment sendPayment(@PathVariable("accountId") AccountId accountId,
                             @PathVariable("paymentId") UUID paymentId,
                             @RequestBody ImmutablePaymentRequest paymentRequest) {
    try {
      getJwt(); // hack to make sure JWT isn't expired
      return sendMoneyService.sendMoney(accountId,
        getBearerToken(),
        paymentRequest.amount(),
        paymentRequest.destinationPaymentPointer(),
        paymentId);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
