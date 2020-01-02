package org.interledger.spsp.server.grpc.services;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.server.grpc.CreateAccountRequest;
import org.interledger.spsp.server.grpc.GetAccountResponse;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountsService {

  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected OkHttpClient okHttpClient;

  public AccountSettings getAccount(AccountId accountId) {


      Request getAccountRequest = this.constructNewGetAccountRequest(accountId);

      try {
        Response response = okHttpClient.newCall(getAccountRequest).execute();

        if (!response.isSuccessful()) {
          String errorMessage = "Unable to get account information from connector for accountId = " + accountId;
          throw new HermesAccountsClientException(errorMessage, accountId);
        }

        String responseBodyString = response.body().string();
        final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);

        return accountSettingsResponse;
      } catch (HermesAccountsClientException e) {
        throw new HermesAccountsClientException(e.getMessage(), e, accountId);
      } catch (JsonProcessingException e) {
        throw new HermesAccountsClientException("Unable to deserialize response from connector into AccountSettings. AccountId = " + accountId, e, accountId);
      } catch (IOException e) {
        throw new HermesAccountsClientException("Problem constructing OkHttpRequest for getAccount for accoundId " + accountId, e, accountId);
      }
  }

  private Request constructNewGetAccountRequest(AccountId accountId) {

    final Headers httpRequestHeaders = new Headers.Builder()
      .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
      .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI + "/" + accountId;

    return new Request.Builder()
      .headers(httpRequestHeaders)
      .url(requestUrl)
      .get()
      .build();
  }

  public Request constructNewCreateAccountRequest(CreateAccountRequest request) throws JsonProcessingException {
    final Headers httpRequestHeaders = new Headers.Builder()
      .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
      .add(CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
      .add(ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
      .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI;

    DecodedJWT jwt = JWT.decode(request.getJwt());
    Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_RS_256);
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, jwt.getIssuer());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, jwt.getAudience().get(0));
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, jwt.getSubject());

    // Had to bring in AccountIdModule to objectMapper from connector-jackson for this serialization to work
    String bodyJson = objectMapper.writeValueAsString(AccountSettings.builder()
      .accountId(AccountId.of(request.getAccountId()))
      .assetCode(request.getAssetCode())
      .assetScale(request.getAssetScale())
      .description(request.getDescription())
      .accountRelationship(AccountRelationship.CHILD)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .customSettings(customSettings)
      .build());

    RequestBody body = RequestBody.create(
      bodyJson,
      okhttp3.MediaType.parse(org.springframework.http.MediaType.APPLICATION_JSON_VALUE));

    return new Request.Builder()
      .headers(httpRequestHeaders)
      .url(requestUrl)
      .post(body)
      .build();
  }

}
