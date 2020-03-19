package org.interledger.spsp.server;

import org.interledger.spsp.server.config.ilp.IlpOverHttpConfig;
import org.interledger.spsp.server.config.web.SpringSpspServerWebMvc;
import org.interledger.spsp.server.util.ExceptionHandlerUtils;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class) // Excluded for `problems` support
@Import({SpringSpspServerWebMvc.class, IlpOverHttpConfig.class})
public class HermesServerApplication {

  @Autowired
  private Environment env;

  @Autowired
  private BuildProperties buildProperties;

  public static void main(String[] args) {
    SpringApplication.run(HermesServerApplication.class, args);
  }

  @Bean
  public ExceptionHandlerUtils exceptionHandlerUtils(final ObjectMapper objectMapper) {
    return new ExceptionHandlerUtils(objectMapper);
  }

  /**
   * <p>Initialize the connector after constructing it.</p>
   */
  @PostConstruct
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private void init() {
    // Configure Sentry
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.putProperty("name", buildProperties.getName());
    context.putProperty("group", buildProperties.getGroup());
    context.putProperty("artifact", buildProperties.getArtifact());
    context.putProperty("release", buildProperties.getVersion());
    context.putProperty("spring-profiles", Arrays.stream(env.getActiveProfiles()).collect(Collectors.joining(" ")));
  }

}
