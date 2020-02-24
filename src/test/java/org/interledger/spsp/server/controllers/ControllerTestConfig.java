package org.interledger.spsp.server.controllers;

import okhttp3.HttpUrl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ControllerTestConfig {

  @Bean
  @Primary
  public HttpUrl spspReceiverUrl() {
    return HttpUrl.parse("http://example.com");
  }
}
