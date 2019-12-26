package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.link.http.auth.BearerTokenSupplier;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

@GRpcService
public class AccountServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {

  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts/{accountId}";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";

  @Autowired
  protected OkHttpClient okHttpClient;

  @Autowired
  protected ObjectMapper objectMapper;

  @Override
  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    Request getBalanceRequest = this.constructNewGetAccountRequest(request);
    try {
      Response response = okHttpClient.newCall(getBalanceRequest).execute();

      String responseBodyString = response.body().string();
      final AccountSettings accountSettingsResponse = objectMapper.readValue(responseBodyString, ImmutableAccountSettings.class);

      final GetAccountResponse.Builder replyBuilder = GetAccountResponse.newBuilder()
          .setAccountId(accountSettingsResponse.accountId().value())
          .setAssetCode(accountSettingsResponse.assetCode())
          .setAssetScale(accountSettingsResponse.assetScale());

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
    super.createAccount(request, responseObserver);
  }

  private Request constructNewGetAccountRequest(GetAccountRequest request) {

    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BASIC + SENDER_PASS_KEY)
        .build();

    String requestUrl = TESTNET_URI + ACCOUNT_URI;
    requestUrl = requestUrl.replace("{accountId}", request.getAccountId());

    return new Request.Builder()
        .headers(httpRequestHeaders)
        .url(requestUrl)
        .get()
        .build();
  }

}
