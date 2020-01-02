package org.interledger.spsp.server.grpc;

import static com.google.common.net.HttpHeaders.ACCEPT;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

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
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
//import org.interledger.spsp.client.SimpleSpspClient;
import org.interledger.spsp.client.SpspClient;
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
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@GRpcService
public class IlpHttpGrpcService extends IlpServiceGrpc.IlpServiceImplBase {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String TESTNET_URI = "https://jc.ilpv4.dev";
  private static final String ACCOUNT_URI = "/accounts";
  // NOTE - replace this with the passkey for your sender account
  private static final String SENDER_PASS_KEY = "YWRtaW46cGFzc3dvcmQ=";
  private static final String BASIC = "Basic ";

  @Autowired
  protected OkHttpClient okHttpClient;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected AccountServiceGrpc accountServiceGrpc;

  @Override
  public void sendMoney(SendPaymentRequestOuterClass.SendPaymentRequest request, StreamObserver<SendPaymentResponseOuterClass.SendPaymentResponse> responseObserver) {
    /*SpspClient spspClient = new SimpleSpspClient();

    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails connectionDetails =
      spspClient.getStreamConnectionDetails(PaymentPointer.of(request.getDestinationPaymentPointer()));


    final InterledgerAddress SENDER_ADDRESS =
      InterledgerAddress.of("jc.ilpv4.dev").with();

    // Use ILP over HTTP for our underlying link
    Link link = newIlpOverHttpLink();

    // Create SimpleStreamSender for sending STREAM payments
    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(link);

    // Send payment using STREAM
    SendMoneyResult result = simpleStreamSender.sendMoney(
      SendMoneyRequest.builder()
        .sourceAddress(SENDER_ADDRESS)
        .amount(UnsignedLong.valueOf(100000))
        .denomination(Denominations.XRP)
        .destinationAddress(connectionDetails.destinationAddress())
        .timeout(Duration.ofMillis(30000))
        .paymentTracker(new FixedSenderAmountPaymentTracker(UnsignedLong.valueOf(100000)))
        .sharedSecret(SharedSecret.of(connectionDetails.sharedSecret().value()))
        .build()
    ).get();

    System.out.println("Send money result: " + result);
    System.out.println("Ending balance for sender: " + rustClient.getBalance(SENDER_ACCOUNT_USERNAME));*/
  }
}