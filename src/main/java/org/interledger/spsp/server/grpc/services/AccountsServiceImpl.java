package org.interledger.spsp.server.grpc.services;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import org.interledger.connector.accounts.AccountAlreadyExistsProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.spsp.server.grpc.config.AccountsServiceConfig;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;

@EnableConfigurationProperties(AccountsServiceConfig.class)
public class AccountsServiceImpl implements AccountsService {
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${interledger.connector.connector-url}")
  private String CONNECTOR_URL;

  private static final String ACCOUNT_URI = "/accounts";

  @Value("${interledger.connector.admin-key}")
  private String SENDER_PASS_KEY;

  private static final String BASIC = "Basic ";

  protected ObjectMapper objectMapper;

  protected OkHttpClient okHttpClient;


  public AccountsServiceImpl() {
  }

  public AccountsServiceImpl(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
    this.objectMapper = objectMapper;
    this.okHttpClient = okHttpClient;
  }

  @Override
  public AccountSettings getAccount(AccountId accountId) {

      Request getAccountRequest = this.constructNewGetAccountRequest(accountId);

      try {
        Response response = okHttpClient.newCall(getAccountRequest).execute();

        if (!response.isSuccessful()) {
          String errorMessage = "Unable to get account information from connector for accountId = " + accountId;
          // Account not found
          if (response.code() == 404) {
            throw new AccountNotFoundProblem(accountId);
          } else {
            throw new HermesAccountException(errorMessage, accountId);
          }
        }

        String responseBodyString = response.body().string();
        final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);

        return accountSettingsResponse;
      } catch (JsonProcessingException e) {
        // This should never really happen, assuming we don't have invalid data on the connector
        throw new HermesAccountException("Unable to deserialize response from connector into AccountSettings. AccountId = " + accountId, e, accountId);
      } catch (IOException e) {
        // Will happen if there is a network
        throw new HermesAccountException("OkHttpClient error.  Unable to get account for " + accountId, e, accountId);
      }
  }

  private Request constructNewGetAccountRequest(AccountId accountId) {

    final Headers httpRequestHeaders = new Headers.Builder()
      .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
      .build();

    String requestUrl = CONNECTOR_URL + ACCOUNT_URI + "/" + accountId;

    return new Request.Builder()
      .headers(httpRequestHeaders)
      .url(requestUrl)
      .get()
      .build();
  }

  @Override
  public AccountSettings createAccount(AccountSettings accountSettings) {

    AccountId accountId = accountSettings.accountId();

    try {
      Request createAccountRequest = constructNewCreateAccountRequest(accountSettings);
      Response response = okHttpClient.newCall(createAccountRequest).execute();

      if (!response.isSuccessful()) {
        // Account already exists
        if (response.code() == 409) {
          throw new AccountAlreadyExistsProblem(accountId);
        }
      }

      String responseBodyString = response.body().string();
      return objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);
    } catch (JsonProcessingException e) {
      throw new HermesAccountException(
        "Unable to deserialize response from connector into AccountSettings. AccountId = " + accountId, e, accountId);
    } catch (IOException e) {
      throw new HermesAccountException(
        "Problem constructing OkHttpRequest for getAccount for accoundId " + accountId, e, accountId);
    }
  }

  public Request constructNewCreateAccountRequest(AccountSettings accountSettings) throws JsonProcessingException {
    final Headers httpRequestHeaders = new Headers.Builder()
      .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
      .add(CONTENT_TYPE, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
      .add(ACCEPT, org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
      .build();

    String requestUrl = CONNECTOR_URL + ACCOUNT_URI;

    // Had to bring in AccountIdModule to objectMapper from connector-jackson for this serialization to work
    String bodyJson = objectMapper.writeValueAsString(accountSettings);

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
