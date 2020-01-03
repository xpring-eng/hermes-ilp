package org.interledger.spsp.server.grpc.services;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountException;

/**
 *  Helper service that interacts with a connector to get and create accounts
 */
public interface AccountsService {

  /**
   * Gets account settings from a connector for a given accountId
   * @param accountId
   * @return Account settings from connector
   *
   * @throws {@link HermesAccountException}
   */
  AccountSettings getAccount(AccountId accountId);

  /**
   * Creates an account on a connector with given settings, which do not have to be fully populated
   * @param accountSettings
   * @return Account settings as returned by the connector after creating an account
   *
   * @throws {@link HermesAccountException}
   */
  AccountSettings createAccount(AccountSettings accountSettings);
}
