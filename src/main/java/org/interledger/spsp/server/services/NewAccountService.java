package org.interledger.spsp.server.services;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.connector.routing.StaticRoute;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.grpc.CreateAccountRequest;
import org.interledger.spsp.server.grpc.services.AccountRequestResponseConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class NewAccountService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ConnectorAdminClient adminClient;

  private final ConnectorRoutesClient connectorRoutesClient;

  private final OutgoingLinkSettings spspLinkSettings;

  private final InterledgerAddressPrefix spspAddressPrefix;

  public NewAccountService(ConnectorAdminClient adminClient, ConnectorRoutesClient connectorRoutesClient, OutgoingLinkSettings spspLinkSettings, InterledgerAddressPrefix spspAddressPrefix) {
    this.adminClient = adminClient;
    this.connectorRoutesClient = connectorRoutesClient;
    this.spspLinkSettings = spspLinkSettings;
    this.spspAddressPrefix = spspAddressPrefix;
  }

  public AccountSettings createAccount(CreateAccountRequest request) {
    // Convert request to AccountSettings
    AccountSettings requestedAccountSettings =
      AccountRequestResponseConverter.accountSettingsFromCreateAccountRequest(request, spspLinkSettings);

    // Create account on the connector
    AccountSettings returnedAccountSettings = adminClient.createAccount(requestedAccountSettings);

    logger.info("Account created successfully with accountId: " + request.getAccountId());

    try {
      InterledgerAddressPrefix routePrefix = spspAddressPrefix.with(returnedAccountSettings.accountId().value());
      connectorRoutesClient.createStaticRoute(
        routePrefix.getValue(),
        StaticRoute.builder()
          .routePrefix(routePrefix)
          .nextHopAccountId(returnedAccountSettings.accountId())
          .build()
      );
    } catch (Exception e) {
      logger.warn("Failed to create route", e);
    }

    return returnedAccountSettings;
  }

  public AccountSettings createRainmaker() {
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN, "password");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.toString());

    // Convert request to AccountSettings
    AccountSettings requestedAccountSettings = AccountSettings.builder()
      .accountId(AccountId.of("rainmaker"))
      .accountRelationship(AccountRelationship.CHILD)
      .customSettings(customSettings)
      .assetScale(9)
      .assetCode("XRP")
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .build();

    // Create account on the connector
    AccountSettings returnedAccountSettings = adminClient.createAccount(requestedAccountSettings);

    logger.info("Account created successfully with accountId: " + requestedAccountSettings.accountId());

    try {
      InterledgerAddressPrefix routePrefix = spspAddressPrefix.with(returnedAccountSettings.accountId().value());
      connectorRoutesClient.createStaticRoute(
        routePrefix.getValue(),
        StaticRoute.builder()
          .routePrefix(routePrefix)
          .nextHopAccountId(returnedAccountSettings.accountId())
          .build()
      );
    } catch (Exception e) {
      logger.warn("Failed to create route", e);
    }

    return returnedAccountSettings;
  }


}
