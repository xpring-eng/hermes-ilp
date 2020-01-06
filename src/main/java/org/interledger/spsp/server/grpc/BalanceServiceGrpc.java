package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.link.http.IlpOverHttpConstants.BEARER;
import static org.interledger.spsp.server.config.ilp.IlpOverHttpConfig.ILP_OVER_HTTP;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.BearerTokenSupplier;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.server.client.ConnectorAccountBalance;
import org.interledger.spsp.server.client.ConnectorBalanceClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.utils.URIBuilder;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@GRpcService
public class BalanceServiceGrpc extends IlpServiceGrpc.IlpServiceImplBase {

  @Autowired
  protected ConnectorBalanceClient balanceClient;

  @Autowired
  protected OkHttpClient okHttpClient;

  @Override
  public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
    balanceClient.getBalance(AccountId.of(request.getAccountId()), request.getJwt())
      .ifPresent(balanceResponse -> {
        final GetBalanceResponse.Builder replyBuilder = GetBalanceResponse.newBuilder()
          .setAssetScale(balanceResponse.assetScale())
          .setAssetCode(balanceResponse.assetCode())
          .setNetBalance(balanceResponse.netBalance().longValue())
          .setPrepaidAmount(balanceResponse.prepaidAmount().longValue())
          .setClearingBalance(balanceResponse.clearingBalance().longValue());

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
      });
   responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
  }

}
