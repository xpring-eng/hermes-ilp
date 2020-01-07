package org.interledger.spsp.server.grpc.config;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.grpc.services.AccountsServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

  @Bean
  public AccountsServiceImpl accountsService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    return new AccountsServiceImpl(objectMapper, okHttpClient);
  }

  @Bean
  public ConnectorBalanceClient balanceClient(@Value("${interledger.connector.connector-url}") String connectorHttpUrl) {
    return ConnectorBalanceClient.construct(HttpUrl.parse(connectorHttpUrl));
  }

  @Bean
  public ConnectorAdminClient adminClient(@Value("${interledger.connector.connector-url}") String connectorHttpUrl/*,
                                          @Value("${interledger.connector.admin-key}") String adminKey*/) {
    return ConnectorAdminClient.construct(HttpUrl.parse(connectorHttpUrl), template -> {
      template.header("Authorization", "YWRtaW46cGFzc3dvcmQ=");
    });
  }

}
