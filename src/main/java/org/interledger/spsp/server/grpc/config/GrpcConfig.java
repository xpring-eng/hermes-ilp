package org.interledger.spsp.server.grpc.config;

import org.interledger.spsp.server.grpc.services.AccountsService;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

  @Bean
  public AccountsService accountsService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    return new AccountsService(objectMapper, okHttpClient);
  }
}
