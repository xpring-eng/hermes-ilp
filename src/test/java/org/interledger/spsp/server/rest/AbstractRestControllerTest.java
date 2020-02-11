package org.interledger.spsp.server.rest;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.spsp.server.AbstractIntegrationTest;
import org.interledger.spsp.server.client.ConnectorRoutesClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class AbstractRestControllerTest extends AbstractIntegrationTest {
  public static class TestConfig {

    /**
     * Overrides the adminClient bean for test purposes to connect to our Connector container
     *
     * @return a ConnectorAdminClient that can speak to the test container connector
     */
    @Bean
    @Primary
    public ConnectorAdminClient adminClient() {
      return ConnectorAdminClient.construct(getInterledgerBaseUri(), template -> {
        template.header(AUTHORIZATION, "Basic " + ADMIN_AUTH_TOKEN);
      });
    }

    @Bean
    @Primary
    public ConnectorRoutesClient routesClient() {
      return ConnectorRoutesClient.construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
      });
    }
  }
}
