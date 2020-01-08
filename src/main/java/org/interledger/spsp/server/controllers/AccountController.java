package org.interledger.spsp.server.controllers;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.server.grpc.CreateAccountRequest;
import org.interledger.spsp.server.services.NewAccountService;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.zalando.problem.spring.common.MediaTypes;


@RestController
public class AccountController extends AbstractController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final NewAccountService newAccountService;

  public AccountController(NewAccountService newAccountService) {
    this.newAccountService = newAccountService;
  }

  @RequestMapping(
    value = "/accounts", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccountSettings createAccount() {
    // FIXME get accountId, assetCode, scale, etc from request object
    DecodedJWT jwt = getJwt();
    Claim nickname = jwt.getClaim("nickname");
    try {
      return newAccountService.createAccount(CreateAccountRequest.newBuilder()
        .setJwt(getBearerToken())
        .setAssetScale(9)
        .setAccountId(nickname.asString())
        .setAssetCode("XRP")
        .setDescription("account")
        .build());
    }
    catch (FeignException e) {
      throw new ResponseStatusException(HttpStatus.valueOf(e.status()), e.contentUTF8());
    }
  }

  @RequestMapping(
    value = "/accounts/rainmaker", method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public AccountSettings createRainmaker() {
    try {
      return newAccountService.createRainmaker();
    }
    catch (FeignException e) {
      throw new ResponseStatusException(HttpStatus.valueOf(e.status()), e.contentUTF8());
    }
  }

}
