package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static okhttp3.CookieJar.NO_COOKIES;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.Link;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.link.http.auth.SimpleBearerTokenSupplier;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.grpc.config.AccountsServiceConfig;
import org.interledger.spsp.server.grpc.services.AccountsService;
import org.interledger.stream.Denomination;
import org.interledger.stream.Denominations;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import io.grpc.stub.StreamObserver;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@GRpcService
@EnableConfigurationProperties(AccountsServiceConfig.class)
public class IlpHttpGrpcService extends IlpServiceGrpc.IlpServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Value("${interledger.connector.connector-url}")
  private String CONNECTOR_URL;

  @Value("${interledger.connector.admin-key}")
  private String SENDER_PASS_KEY;

  @Autowired
  protected OkHttpClient okHttpClient;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected AccountsService accountService;

  @Override
  public void sendMoney(SendPaymentRequestOuterClass.SendPaymentRequest request, StreamObserver<SendPaymentResponseOuterClass.SendPaymentResponse> responseObserver) {
    SpspClient spspClient = new SimpleSpspClient();

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails connectionDetails =
      spspClient.getStreamConnectionDetails(PaymentPointer.of(request.getDestinationPaymentPointer()));

    String senderAccountId = request.getAccountId();
    AccountSettings senderAccountSettings = accountService.getAccount(AccountId.of(senderAccountId));

    final InterledgerAddress senderAddress =
      InterledgerAddress.of("jc.ilpv4.dev").with(senderAccountId);

    // Use ILP over HTTP for our underlying link
    Link link = newIlpOverHttpLink(senderAddress, senderAccountId);

    // Create SimpleStreamSender for sending STREAM payments
    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);

    // Send payment using STREAM
    SendMoneyResult result = null;
    try {
      result = simpleStreamSender.sendMoney(
        SendMoneyRequest.builder()
          .sourceAddress(senderAddress)
          .amount(UnsignedLong.valueOf(request.getAmount()))
          .denomination(Denomination.builder()
            .assetCode(senderAccountSettings.assetCode())
            .assetScale((short) senderAccountSettings.assetScale())
            .build())
          .destinationAddress(connectionDetails.destinationAddress())
          .timeout(Duration.ofSeconds(request.getTimeoutSeconds()))
          .paymentTracker(new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(request.getAmount())))
          .sharedSecret(SharedSecret.of(connectionDetails.sharedSecret().value()))
          .build()
      ).get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }

    System.out.println("Send money result: " + result);
  }

  private Link newIlpOverHttpLink(InterledgerAddress senderAddress, String senderAccountId) {
    return new IlpOverHttpLink(
      () -> senderAddress,
      HttpUrl.parse(CONNECTOR_URL + "/ilp"),
      newHttpClient(),
      new ObjectMapper(),
      InterledgerCodecContextFactory.oer(),
      new SimpleBearerTokenSupplier(senderAccountId + ":" + SENDER_PASS_KEY)
    );
  }

  private static OkHttpClient newHttpClient() {
    ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT))
      .cookieJar(NO_COOKIES)
      .connectTimeout(5000, TimeUnit.MILLISECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS);
    return builder.connectionPool(connectionPool).build();
  }
}