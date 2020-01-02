package org.interledger.spsp.server.grpc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties("interledger.connector")
public class AccountsServiceConfig {

  private String CONNECTOR_URL;

  private String ACCOUNT_URI = "/accounts";

  private String SENDER_PASS_KEY;

  private String BASIC = "Basic ";
}
