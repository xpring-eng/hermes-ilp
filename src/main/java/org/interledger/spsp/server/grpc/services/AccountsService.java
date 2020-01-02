package org.interledger.spsp.server.grpc.services;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.spsp.server.grpc.CreateAccountRequest;
import org.interledger.spsp.server.grpc.config.AccountsServiceConfig;
import org.interledger.spsp.server.grpc.exceptions.HermesAccountsClientException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@EnableConfigurationProperties(AccountsServiceConfig.class)
public class AccountsService {

  @Value("${interledger.connector.connector-url}")
  private String CONNECTOR_URL;

  private static final String ACCOUNT_URI = "/accounts";

  @Value("${interledger.connector.admin-key}")
  private String SENDER_PASS_KEY;

  private static final String BASIC = "Basic ";

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected OkHttpClient okHttpClient;

  @Autowired
  protected RequestResponseConverter requestResponseConverter;

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

    String requestUrl = CONNECTOR_URL + ACCOUNT_URI + "/" + accountId;

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

    String requestUrl = CONNECTOR_URL + ACCOUNT_URI;

    // Had to bring in AccountIdModule to objectMapper from connector-jackson for this serialization to work
    String bodyJson = objectMapper.writeValueAsString(requestResponseConverter.accountSettingsFromCreateAccountRequest(request));

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
