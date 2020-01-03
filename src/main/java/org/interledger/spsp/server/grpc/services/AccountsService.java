package org.interledger.spsp.server.grpc.services;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;

/**
 *
 */
public interface AccountsService {

  /**
   * Gets account settings from a connector for a given accountId
   * @param accountId
   * @return Account settings from connector
   *
   * @throws {@link org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException}
   */
  AccountSettings getAccount(AccountId accountId);

  /**
   * Creates an account on a connector with given settings, which do not have to be fully populated
   * @param accountSettings
   * @return Account settings as returned by the connector after creating an account
   *
   * @throws {@link org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException}
   */
  AccountSettings createAccount(AccountSettings accountSettings);
}
