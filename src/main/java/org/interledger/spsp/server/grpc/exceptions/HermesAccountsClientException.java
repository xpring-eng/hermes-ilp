package org.interledger.spsp.server.grpc.exceptions;

import org.interledger.connector.accounts.AccountId;

import java.util.Objects;

public class HermesAccountsClientException extends RuntimeException {

  private AccountId accountId;

  public HermesAccountsClientException(String message, AccountId accountId) {
    super(message);
    this.accountId = Objects.requireNonNull(accountId);
  }

  public HermesAccountsClientException(String message, Throwable e, AccountId accountId) {
    super(message, e);
    this.accountId = Objects.requireNonNull(accountId);
  }

  public AccountId getAccountId() {
    return accountId;
  }

  @Override
  public String getMessage() {
    return super.getMessage();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
