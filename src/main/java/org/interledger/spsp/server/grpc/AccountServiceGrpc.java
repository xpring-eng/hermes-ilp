package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;

import org.interledger.link.http.auth.BearerTokenSupplier;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;

import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@GRpcService
public class AccountServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {

  private static final String TESTNET_URI = "https://rs3.xpring.dev";
  private static final String ACCOUNT_URI = "/accounts/{accountId}";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "w6uwvg42ogktl";

  @Autowired
  protected OkHttpClient okHttpClient;

  public void getAccount(GetAccountRequest request, StreamObserver<GetAccountResponse> responseObserver) {
    Request getBalanceRequest = this.constructNewGetAccountRequest(request);
    try {
      Response response = okHttpClient.newCall(getBalanceRequest).execute();

      final GetAccountResponse.Builder replyBuilder = GetAccountResponse.newBuilder();
      JsonFormat.parser().ignoringUnknownFields().merge(response.body().string(), replyBuilder);

      responseObserver.onNext(replyBuilder.build());
      responseObserver.onCompleted();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Request constructNewGetAccountRequest(GetAccountRequest request) {
    BearerTokenSupplier tokenSupplier = new SimpleBearerTokenSupplier(request.getAccountId() + ":" + SENDER_PASS_KEY);

    final Headers httpRequestHeaders = new Headers.Builder()
        .add(AUTHORIZATION, BEARER + tokenSupplier.get())
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
