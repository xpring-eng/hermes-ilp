package org.interledger.spsp.server.controllers;

import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.server.client.ConnectorBalanceClient;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.client.ConnectorTokensClient;
import org.interledger.spsp.server.services.GimmeMoneyService;
import org.interledger.spsp.server.services.NewAccountService;
import org.interledger.spsp.server.services.SendMoneyService;

import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.servlet.mvc.AbstractController;

/**
 * An abstract super class for all Controller tests that inherit {@link AbstractController}.
 */
@ContextConfiguration(classes = {
  ControllerTestConfig.class // For custom Beans.
})
@ComponentScan(basePackages = "org.interledger.spsp.server.controllers",
  excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
    value = AbstractIntegrationTest.TestConfig.class)})
public abstract class AbstractControllerTest {

  @MockBean
  protected NewAccountService accountService;
  @MockBean
  protected ConnectorAdminClient adminClient;
  @Autowired
  protected HttpUrl spspReceiverUrl;
  @MockBean
  protected ConnectorBalanceClient balanceClient;
  @MockBean
  protected ConnectorTokensClient tokensClient;
  @MockBean
  protected GimmeMoneyService gimmeMoneyService;
  @MockBean
  protected SendMoneyService sendMoneyService;
  @MockBean
  protected ConnectorRoutesClient connectorRoutesClient;
  @MockBean
  protected OutgoingLinkSettings spspLinkSettings;
  @MockBean
  protected InterledgerAddressPrefix spspAddressPrefix;

  /**
   * Construct an instance of {@link HttpHeaders} that contains everything needed to make a valid request to the Hermes
   * API endpoint for a JSON payload.
   *
   * @return An instance of {@link HttpHeaders}.
   */
  protected HttpHeaders testJsonHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

}
