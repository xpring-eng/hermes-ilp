package org.interledger.spsp.server;

import static org.interledger.spsp.server.config.ilp.IlpOverHttpConfig.SPSP;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.controllers.AccountController;
import org.interledger.spsp.server.controllers.BalanceController;
import org.interledger.spsp.server.controllers.PaymentController;
import org.interledger.spsp.server.services.SendMoneyService;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Properties;

public abstract class AbstractIntegrationTest {

  protected static TestIlpContainers containers;
  @Autowired
  protected AccountController accountController;
  @Autowired
  protected BalanceController balanceController;
  @Autowired
  protected PaymentController paymentController;

  @BeforeClass
  public static void startContainers() {
    containers = TestIlpContainers.createContainers();
  }

  @AfterClass
  public static void stopContainers() {
    containers.stop();
  }

  public abstract static class TestConfig {

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
      SpspClient spspClient) {
      return new SendMoneyService(containers.getNodeBaseUri(), objectMapper, adminClient, okHttpClient, spspClient);
    }
  }

}
