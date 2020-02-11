package org.interledger.spsp.server.services.tracker;

public class HermesPaymentTrackerException extends RuntimeException {
  public HermesPaymentTrackerException() {
  }

  public HermesPaymentTrackerException(String message) {
    super(message);
  }

  public HermesPaymentTrackerException(String message, Throwable cause) {
    super(message, cause);
  }

  public HermesPaymentTrackerException(Throwable cause) {
    super(cause);
  }

  public HermesPaymentTrackerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
