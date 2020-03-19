package org.interledger.spsp.server.services;

import org.interledger.connector.accounts.AccountId;
import org.interledger.spsp.server.model.CreateAccountRestRequest;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

/**
 * A utility service for account creation, with methods for generating default accounts as well as simple auth tokens
 * and accountIds.
 */
@Service
public class AccountGeneratorService {

  /**
   * Generates a random alphanumeric string of length 13 to be used as credentials
   *
   * TODO: Use a library that generates more secure tokens
   *
   * @return Random alphanumeric simple auth token with 13 characters
   *
   * @deprecated This functionality should be controlled by the Connector so it can ensure the quality of its auth
   *   tokens.
   */
  @Deprecated
  public static String generateSimpleAuthCredentials() {
    return RandomStringUtils.randomAlphanumeric(13);
  }

  /**
   * Generates a {@link CreateAccountRestRequest} if none is given
   *
   * @return a {@link CreateAccountRestRequest} with a generated accountId
   */
  public static CreateAccountRestRequest newDefaultCreateAccountRequest() {
    return CreateAccountRestRequest.builder("XRP", 9).build();
  }

  /**
   * Generates an account ID with format user_{random 8 alphanumeric characters}
   *
   * @return A String representing a generated account ID
   */
  public static AccountId generateAccountId() {
    return AccountId.of("user_" + RandomStringUtils.randomAlphanumeric(8));
  }


}
