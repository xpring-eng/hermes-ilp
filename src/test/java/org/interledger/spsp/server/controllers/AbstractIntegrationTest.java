package org.interledger.spsp.server.controllers;

import static org.interledger.spsp.server.config.ilp.IlpOverHttpConfig.SPSP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.TestIlpContainers;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.client.ConnectorTokensClient;
import org.interledger.spsp.server.config.jackson.JacksonConfig;
import org.interledger.spsp.server.model.BearerToken;
import org.interledger.spsp.server.model.BearerTokenHeaderConverter;
import org.interledger.spsp.server.services.SendMoneyService;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractIntegrationTest {

  protected static TestIlpContainers containers;
  @Autowired
  protected AccountController accountController;
  @Autowired
  protected BalanceController balanceController;
  @Autowired
  protected PaymentController paymentController;
  @Autowired
  protected AccountTokenController tokenController;

  @BeforeClass
  public static void startContainers() {
    containers = TestIlpContainers.createContainers();
  }

  @AfterClass
  public static void stopContainers() {
    containers.stop();
  }

  /**
   * Hack to mock out the HttpRequest that the controller uses to get the Authorization header
   *
   * @param bearerToken A {@link BearerToken}.
   * @param callable    to run with mocked credentials
   */
  protected <T> T withAuthToken(BearerToken bearerToken, Callable<T> callable) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(AUTHORIZATION)).thenReturn(bearerToken.value());
    try {
      return callable.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Configuration
  @Import(JacksonConfig.class)
  public abstract static class TestConfig {

    @Bean
    @Primary
    public BearerTokenHeaderConverter bearerTokenHeaderConverter() {
      return new BearerTokenHeaderConverter();
    }
    /**
     * Overrides the adminClient bean for test purposes to connect to our Connector container
     *
     * @return a ConnectorAdminClient that can speak to the test container connector
     */
    @Bean
    @Primary
    public ConnectorAdminClient adminClient() {
      return containers.adminClient();
    }

    @Bean
    @Primary
    public ConnectorRoutesClient routesClient() {
      return containers.routesClient();
    }

    @Bean
    @Primary
    public ConnectorBalanceClient balanceClient() {
      return containers.balanceClient();
    }

    @Bean
    @Primary
    public ConnectorTokensClient tokensClient() {
      return containers.tokensClient();
    }

    @Bean
    @Qualifier(SPSP)
    @Primary
    public HttpUrl spspReceiverUrl() {
      return containers.getSpspBaseUri();
    }

    @Bean
    @Primary
    public SpspClient spspClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
      return new SimpleSpspClient(okHttpClient,
        paymentPointer -> HttpUrl.parse("http://" + paymentPointer.host() + paymentPointer.path()),
        objectMapper);
    }

    @Bean
    @Primary
    public SendMoneyService sendMoneyService(ObjectMapper objectMapper,
      ConnectorAdminClient adminClient,
      OkHttpClient okHttpClient,
      SpspClient spspClient,
      @Qualifier(SPSP) InterledgerAddressPrefix spspAddressPrefix
    ) {
      return new SendMoneyService(
        containers.getNodeBaseUri(), objectMapper, adminClient, okHttpClient, spspClient, spspAddressPrefix
      );
    }
  }

}
