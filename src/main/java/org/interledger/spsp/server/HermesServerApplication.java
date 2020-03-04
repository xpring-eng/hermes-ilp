package org.interledger.spsp.server;

import org.interledger.spsp.server.config.ilp.IlpOverHttpConfig;
import org.interledger.spsp.server.config.web.SpringSpspServerWebMvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class) // Excluded for `problems` support
@Import({SpringSpspServerWebMvc.class, IlpOverHttpConfig.class})
public class HermesServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(HermesServerApplication.class, args);
  }

}
