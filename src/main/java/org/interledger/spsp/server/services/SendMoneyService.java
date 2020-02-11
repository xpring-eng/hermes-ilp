package org.interledger.spsp.server.services;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.SharedSecret;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.model.Payment;
import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.PreDestroy;

public class SendMoneyService {

  public static final int SEND_TIMEOUT = 30;
  private final SpspClient spspClient;

  private final HttpUrl connectorUrl;

  private final ObjectMapper objectMapper;

  private final ConnectorAdminClient adminClient;
  private OkHttpClient okHttpClient;
  private final ExecutorService executorService;
  private HermesPaymentTracker hermesPaymentTracker;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public SendMoneyService(HttpUrl connectorUrl,
                          ObjectMapper objectMapper,
                          ConnectorAdminClient adminClient,
                          OkHttpClient okHttpClient,
                          SpspClient spspClient,
                          HermesPaymentTracker hermesPaymentTracker) {
    this.connectorUrl = connectorUrl;
    this.objectMapper = objectMapper;
    this.adminClient = adminClient;
    this.okHttpClient = okHttpClient;
    this.executorService = Executors.newCachedThreadPool();
    this.spspClient = spspClient;
    this.hermesPaymentTracker = hermesPaymentTracker;
  }

  @PreDestroy
  private void destroy() {
    executorService.shutdown();
  }

  private void registerPayment(Payment payment) {
    hermesPaymentTracker.registerPayment(payment.paymentId(),
      payment.senderAccountId(),
      payment.originalAmount(),
      payment.destination());
  }

  private void updatePaymentOnComplete(UUID paymentId, SendMoneyResult paymentResult) {
    hermesPaymentTracker.updatePaymentOnComplete(paymentId,
      paymentResult.amountSent(),
      paymentResult.amountDelivered(),
      paymentResult.amountLeftToSend(),
      paymentResult.successfulPayment() ? HermesPaymentTracker.PaymentStatus.SUCCESSFUL : HermesPaymentTracker.PaymentStatus.FAILED);
  }

  private void updatePaymentOnError(UUID paymentId) {
    hermesPaymentTracker.updatePaymentOnError(paymentId);
  }

  public Payment sendMoney(final AccountId senderAccountId,
                           final String bearerToken,
                           final UnsignedLong amount,
                           final PaymentPointer destination,
                           final UUID paymentId) {
    // Fetch shared secret and destination address using SPSP client
    StreamConnectionDetails connectionDetails = spspClient.getStreamConnectionDetails(destination);

    AccountSettings senderAccountSettings = adminClient.findAccount(senderAccountId.value())
      .orElseThrow(() -> new AccountNotFoundProblem(senderAccountId));

    final InterledgerAddress senderAddress =
      InterledgerAddress.of("test.jc.money").with(senderAccountId.value());

    // Use ILP over HTTP for our underlying link
    IlpOverHttpLink link = newIlpOverHttpLink(senderAddress, senderAccountId, bearerToken);

    // Create SimpleStreamSender for sending STREAM payments
    SimpleStreamSender simpleStreamSender = new SimpleStreamSender(new JavaxStreamEncryptionService(),
      link,
      executorService);

    Payment payment = Payment.builder()
      .paymentId(paymentId)
      .senderAccountId(senderAccountId)
      .originalAmount(amount)
      .destination(destination)
      .status(HermesPaymentTracker.PaymentStatus.PENDING)
      .build();

    // Register the payment in Redis.  If this fails, don't send the payment
    this.registerPayment(payment);

    // Execute sendMoney asynchronously
    CompletableFuture.supplyAsync(() -> {
      try {
        SendMoneyResult result = simpleStreamSender.sendMoney(
          SendMoneyRequest.builder()
            .sourceAddress(senderAddress)
            .amount(amount)
            .denomination(Denomination.builder()
              .assetCode(senderAccountSettings.assetCode())
              .assetScale((short) senderAccountSettings.assetScale())
              .build())
            .destinationAddress(connectionDetails.destinationAddress())
            .timeout(Duration.ofSeconds(SEND_TIMEOUT))
            .paymentTracker(new FixedSenderAmountPaymentTracker(amount))
            .sharedSecret(SharedSecret.of(connectionDetails.sharedSecret().value()))
            .build()
        ).get();

        // If no errors in payment, update payment in Redis.
        // Note: Just because there were no exceptions doesn't mean payment was successful!!
        this.updatePaymentOnComplete(paymentId, result);

      } catch (InterruptedException | ExecutionException e) {
        logger.error("");

        // Update the payment in Redis with status = FAILED
        this.updatePaymentOnError(paymentId);
      }

      // We don't care about the result of this. Payment results can be accessed via the getPayment endpoint
      return null;
    });

    return payment;
  }

  private IlpOverHttpLink newIlpOverHttpLink(InterledgerAddress senderAddress, AccountId senderAccountId, String bearerToken) {
    HttpUrl ilpHttpUrl = new HttpUrl.Builder()
      .scheme(connectorUrl.scheme())
      .host(connectorUrl.host())
      .port(connectorUrl.port())
      .addPathSegment("accounts")
      .addPathSegment(senderAccountId.value())
      .addPathSegment("ilp")
      .build();

    IlpOverHttpLink link = new IlpOverHttpLink(
      () -> senderAddress,
      ilpHttpUrl,
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      () -> bearerToken
    );
    link.setLinkId(LinkId.of(senderAccountId.value()));
    return link;
  }
}
