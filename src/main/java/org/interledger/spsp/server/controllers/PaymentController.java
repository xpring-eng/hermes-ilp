package org.interledger.spsp.server.controllers;

import static org.interledger.spsp.server.services.AuthUtils.getAuthorizationAsBearerToken;
import static org.interledger.spsp.server.services.AuthUtils.getJwt;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.model.ImmutablePaymentRequest;
import org.interledger.spsp.server.model.PaymentRequest;
import org.interledger.spsp.server.model.PaymentResponse;
import org.interledger.spsp.server.services.SendMoneyService;
import org.interledger.stream.SendMoneyResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;


@RestController
public class PaymentController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final SendMoneyService sendMoneyService;

  public PaymentController(SendMoneyService sendMoneyService) {
    this.sendMoneyService = sendMoneyService;
  }

  /**
   * Sends payment from the given account.
   *
   * @param authorizationHeader The Authorization header as taken from the incoming {@link HttpServletRequest}.
   * @param accountId           The ILP Connector account identifier for this request.
   * @param paymentRequest      A {@link PaymentRequest}.
   *
   * @return payment result
   */
  @RequestMapping(
    value = "/accounts/{accountId}/pay", method = {RequestMethod.POST},
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public PaymentResponse sendPayment(
    @RequestHeader(AUTHORIZATION) Optional<String> authorizationHeader,
    @PathVariable("accountId") String accountId,
    @RequestBody ImmutablePaymentRequest paymentRequest
  ) {
    try {
      getJwt(authorizationHeader); // hack to make sure JWT isn't expired
      SendMoneyResult result = sendMoneyService.sendMoney(AccountId.of(accountId),
        getAuthorizationAsBearerToken(authorizationHeader),
        paymentRequest.amount(),
        PaymentPointer.of(paymentRequest.destinationPaymentPointer()));

      return PaymentResponse.builder()
        .amountDelivered(result.amountDelivered())
        .amountSent(result.amountSent())
        .originalAmount(result.originalAmount())
        .successfulPayment(result.successfulPayment())
        .build();
    } catch (ExecutionException | InterruptedException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), e);
    }
  }

}
