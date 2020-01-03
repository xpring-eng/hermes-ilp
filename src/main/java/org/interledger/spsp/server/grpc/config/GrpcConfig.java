package org.interledger.spsp.server.grpc.config;

import org.interledger.spsp.server.grpc.services.AccountsServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

  @Bean
  public AccountsServiceImpl accountsService(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    return new AccountsServiceImpl(objectMapper, okHttpClient);
  }
}
