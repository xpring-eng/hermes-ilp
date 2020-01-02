package org.interledger.spsp.server.grpc.services;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.spsp.server.grpc.CreateAccountRequest;
import org.interledger.spsp.server.grpc.CreateAccountResponse;
import org.interledger.spsp.server.grpc.GetAccountResponse;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class RequestResponseConverter {


  public static GetAccountResponse.Builder createGetAccountResponseFromAccountSettings(AccountSettings accountSettings) {

    long maxPacketAmount = accountSettings.maximumPacketAmount().isPresent() ?
      accountSettings.maximumPacketAmount().get().longValue() : 0L;

    // TODO: Maybe there is a way to copy properties to reduce some of this code
    // Convert AccountSettings into CreateAccountResponse
    AccountBalanceSettings accountBalanceSettings = accountSettings.balanceSettings();
    GetAccountResponse.Builder protoResponseBuilder = GetAccountResponse.newBuilder()
      .setAccountRelationship(accountSettings.accountRelationship().toString())
      .setAssetCode(accountSettings.assetCode())
      .setAssetScale(accountSettings.assetScale())
      .setMaximumPacketAmount(maxPacketAmount)
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()))
      .setAccountId(accountSettings.accountId().value())
      .setCreatedAt(accountSettings.createdAt().toString())
      .setModifiedAt(accountSettings.modifiedAt().toString())
      .setDescription(accountSettings.description())
      .setLinkType(accountSettings.linkType().value())
      .setIsInternal(accountSettings.isInternal())
      .setIsConnectionInitiator(accountSettings.isConnectionInitiator())
      .setIlpAddressSegment(accountSettings.ilpAddressSegment())
      .setIsSendRoutes(accountSettings.isSendRoutes())
      .setIsReceiveRoutes(accountSettings.isReceiveRoutes());


    GetAccountResponse.BalanceSettings.Builder balanceSettingsBuilder = GetAccountResponse.BalanceSettings.newBuilder()
      .setSettleTo(accountBalanceSettings.settleTo());
    accountBalanceSettings.minBalance().ifPresent(
      minBalance -> balanceSettingsBuilder.setMinBalance(accountBalanceSettings.minBalance().get())
    );
    accountBalanceSettings.settleThreshold().ifPresent(
      minBalance -> balanceSettingsBuilder.setSettleThreshold(accountBalanceSettings.settleThreshold().get())
    );

    protoResponseBuilder.setBalanceSettings(balanceSettingsBuilder.build());

    accountSettings.rateLimitSettings().maxPacketsPerSecond().ifPresent(
      max -> protoResponseBuilder.setMaximumPacketAmount(accountSettings.rateLimitSettings().maxPacketsPerSecond().get())
    );

    Optional<SettlementEngineDetails> settlementEngineDetails = accountSettings.settlementEngineDetails();
    GetAccountResponse.SettlementEngineDetails.Builder settlementEngineBuilder = GetAccountResponse.SettlementEngineDetails.newBuilder();
    settlementEngineDetails.ifPresent(
      settle -> {
        settlementEngineDetails.get().settlementEngineAccountId().ifPresent(
          accountId -> settlementEngineBuilder.setSettlementEngineAccountId(settlementEngineDetails.get().settlementEngineAccountId().get().value())
        );

        settlementEngineBuilder.setBaseUrl(settlementEngineDetails.get().baseUrl() == null ? null : settlementEngineDetails.get().baseUrl().toString());

        settlementEngineBuilder.putAllCustomSettings(settingsMapToGrpcSettingsMap(settlementEngineDetails.get().customSettings()));
      });

    return protoResponseBuilder
      .setIsParentAccount(accountSettings.isParentAccount())
      .setIsChildAccount(accountSettings.isChildAccount())
      .setIsPeerAccount(accountSettings.isPeerAccount())
      .setIsPeerOrParentAccount(accountSettings.isPeerOrParentAccount())
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()));
  }

  public static CreateAccountResponse.Builder generateCreateAccountResponseFromAccountSettings(AccountSettings accountSettings) {

    long maxPacketAmount = accountSettings.maximumPacketAmount().isPresent() ?
      accountSettings.maximumPacketAmount().get().longValue() : 0L;

    // TODO: Maybe there is a way to copy properties to reduce some of this code
    // Convert AccountSettings into CreateAccountResponse
    AccountBalanceSettings accountBalanceSettings = accountSettings.balanceSettings();
    CreateAccountResponse.Builder protoResponseBuilder = CreateAccountResponse.newBuilder()
      .setAccountRelationship(accountSettings.accountRelationship().toString())
      .setAssetCode(accountSettings.assetCode())
      .setAssetScale(accountSettings.assetScale())
      .setMaximumPacketAmount(maxPacketAmount)
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()))
      .setAccountId(accountSettings.accountId().value())
      .setCreatedAt(accountSettings.createdAt().toString())
      .setModifiedAt(accountSettings.modifiedAt().toString())
      .setDescription(accountSettings.description())
      .setLinkType(accountSettings.linkType().value())
      .setIsInternal(accountSettings.isInternal())
      .setIsConnectionInitiator(accountSettings.isConnectionInitiator())
      .setIlpAddressSegment(accountSettings.ilpAddressSegment())
      .setIsSendRoutes(accountSettings.isSendRoutes())
      .setIsReceiveRoutes(accountSettings.isReceiveRoutes());


    CreateAccountResponse.BalanceSettings.Builder balanceSettingsBuilder = CreateAccountResponse.BalanceSettings.newBuilder()
      .setSettleTo(accountBalanceSettings.settleTo());
    accountBalanceSettings.minBalance().ifPresent(
      minBalance -> balanceSettingsBuilder.setMinBalance(accountBalanceSettings.minBalance().get())
    );
    accountBalanceSettings.settleThreshold().ifPresent(
      minBalance -> balanceSettingsBuilder.setSettleThreshold(accountBalanceSettings.settleThreshold().get())
    );

    protoResponseBuilder.setBalanceSettings(balanceSettingsBuilder.build());

    accountSettings.rateLimitSettings().maxPacketsPerSecond().ifPresent(
      max -> protoResponseBuilder.setMaximumPacketAmount(accountSettings.rateLimitSettings().maxPacketsPerSecond().get())
    );

    Optional<SettlementEngineDetails> settlementEngineDetails = accountSettings.settlementEngineDetails();
    CreateAccountResponse.SettlementEngineDetails.Builder settlementEngineBuilder = CreateAccountResponse.SettlementEngineDetails.newBuilder();
    settlementEngineDetails.ifPresent(
      settle -> {
        settlementEngineDetails.get().settlementEngineAccountId().ifPresent(
          accountId -> settlementEngineBuilder.setSettlementEngineAccountId(settlementEngineDetails.get().settlementEngineAccountId().get().value())
        );

        settlementEngineBuilder.setBaseUrl(settlementEngineDetails.get().baseUrl() == null ? null : settlementEngineDetails.get().baseUrl().toString());

        settlementEngineBuilder.putAllCustomSettings(settingsMapToGrpcSettingsMap(settlementEngineDetails.get().customSettings()));
      });

    return protoResponseBuilder
      .setIsParentAccount(accountSettings.isParentAccount())
      .setIsChildAccount(accountSettings.isChildAccount())
      .setIsPeerAccount(accountSettings.isPeerAccount())
      .setIsPeerOrParentAccount(accountSettings.isPeerOrParentAccount())
      .putAllCustomSettings(settingsMapToGrpcSettingsMap(accountSettings.customSettings()));
  }

  private static Map<String, String> settingsMapToGrpcSettingsMap(Map<String, Object> settingsMap) {
    return settingsMap.entrySet()
      .stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
  }

  public static AccountSettings accountSettingsFromCreateAccountRequest(CreateAccountRequest createAccountRequest) {
    // Derive custom settings (auth) from jwt
    DecodedJWT jwt = JWT.decode(createAccountRequest.getJwt());
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwt.getIssuer());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwt.getAudience().get(0));
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwt.getSubject());

    return AccountSettings.builder()
      .accountId(AccountId.of(createAccountRequest.getAccountId()))
      .assetCode(createAccountRequest.getAssetCode())
      .assetScale(createAccountRequest.getAssetScale())
      .description(createAccountRequest.getDescription())
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .build();
  }
}
