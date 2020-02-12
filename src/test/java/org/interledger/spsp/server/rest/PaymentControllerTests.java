package org.interledger.spsp.server.rest;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.client.ConnectorAdminClient;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.client.SpspClient;
import org.interledger.spsp.server.HermesServerApplication;
import org.interledger.spsp.server.client.ConnectorRoutesClient;
import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;
import org.interledger.spsp.server.model.Payment;
import org.interledger.spsp.server.model.PaymentRequest;
import org.interledger.spsp.server.services.HermesUtils;
import org.interledger.spsp.server.services.NewAccountService;
import org.interledger.spsp.server.services.PaymentService;
import org.interledger.spsp.server.services.tracker.HermesPaymentTracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, PaymentControllerTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class PaymentControllerTests extends AbstractRestControllerTest {

  @LocalServerPort
  private int hermesPort;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private NewAccountService newAccountService;

  @Autowired
  private HermesPaymentTracker hermesPaymentTracker;

  @Autowired
  private OutgoingLinkSettings outgoingLinkSettings;

  @Autowired
  private HttpUrl spspReceiverUrl;

  private final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();

  String hermesUrl;

  AccountSettings senderAccount;
  AccountSettings receiverAccount;
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Before
  public void setUp() throws IOException {
    super.setUp();
    hermesUrl = "http://localhost:" + hermesPort;

    senderAccount = newAccountService.createAccount(Optional.empty(), Optional.empty());
    receiverAccount = newAccountService.createAccount(Optional.empty(), Optional.empty());
  }

  @Test
  public void sendPayment() throws InterruptedException, JsonProcessingException {
    UnsignedLong sendAmount = UnsignedLong.valueOf(100000);
    PaymentRequest request = PaymentRequest.builder()
      .amount(sendAmount)
      .destinationPaymentPointer(HermesUtils.paymentPointerFromSpspUrl(spspReceiverUrl, receiverAccount.accountId()))
      .build();

    final HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + senderAccount.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN).toString());
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity requestEntity = new HttpEntity(request, headers);
    UUID paymentId = UUID.randomUUID();
    ResponseEntity<Payment> response =
      testRestTemplate.exchange(hermesUrl + "/accounts/{accountId}/payments/{paymentId}", HttpMethod.PUT, requestEntity, Payment.class, senderAccount.accountId(), paymentId);

    Payment paymentInRedis = hermesPaymentTracker.payment(paymentId);
    assertThat(paymentInRedis.status()).isEqualTo(HermesPaymentTracker.PaymentStatus.PENDING);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    Payment payment = Objects.requireNonNull(response.getBody());
    assertThat(payment.paymentId()).isEqualTo(paymentId);
    assertThat(payment.senderAccountId()).isEqualTo(senderAccount.accountId());
    assertThat(payment.originalAmount()).isEqualTo(sendAmount);
    assertThat(payment.status()).isEqualTo(HermesPaymentTracker.PaymentStatus.PENDING);
    assertThat(payment.amountSent().longValue()).isEqualTo(UnsignedLong.ZERO.longValue());
    assertThat(payment.amountDelivered().longValue()).isEqualTo(UnsignedLong.ZERO.longValue());
    assertThat(payment.amountLeftToSend().longValue()).isEqualTo(sendAmount.longValue());

    Thread.sleep(5000);
    Payment statusCheck = hermesPaymentTracker.payment(paymentId);
    logger.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(statusCheck));
    assertThat(statusCheck.status()).isNotEqualTo(HermesPaymentTracker.PaymentStatus.PENDING);
  }

  @Test
  public void getPendingPayment() {
    UUID paymentId = UUID.randomUUID();
    AccountId accountId = AccountId.of("foo");
    UnsignedLong originalAmount = UnsignedLong.valueOf(1000);
    PaymentPointer destination = PaymentPointer.of("$foo.bar/baz");
    hermesPaymentTracker.registerPayment(paymentId, accountId, originalAmount, destination);

    Payment payment = testRestTemplate.getForEntity(hermesUrl + "/payments/" + paymentId, Payment.class).getBody();
    assertThat(payment.paymentId()).isEqualTo(paymentId);
    assertThat(payment.senderAccountId()).isEqualTo(accountId);
    assertThat(payment.originalAmount()).isEqualTo(originalAmount);
    assertThat(payment.destination()).isEqualTo(destination);
    assertThat(payment.amountSent()).isEqualTo(UnsignedLong.ZERO);
    assertThat(payment.amountDelivered()).isEqualTo(UnsignedLong.ZERO);
    assertThat(payment.amountLeftToSend()).isEqualTo(originalAmount);
  }

  @Test
  public void getSuccessfulPayment() {
    UUID paymentId = UUID.randomUUID();
    AccountId accountId = AccountId.of("foo");
    UnsignedLong originalAmount = UnsignedLong.valueOf(1000);
    PaymentPointer destination = PaymentPointer.of("$foo.bar/baz");
    hermesPaymentTracker.registerPayment(paymentId, accountId, originalAmount, destination);

    hermesPaymentTracker.updatePaymentOnComplete(paymentId, originalAmount, originalAmount, UnsignedLong.ZERO, HermesPaymentTracker.PaymentStatus.SUCCESSFUL);

    Payment payment = testRestTemplate.getForEntity(hermesUrl + "/payments/" + paymentId, Payment.class).getBody();
    assertThat(payment.paymentId()).isEqualTo(paymentId);
    assertThat(payment.senderAccountId()).isEqualTo(accountId);
    assertThat(payment.originalAmount()).isEqualTo(originalAmount);
    assertThat(payment.destination()).isEqualTo(destination);
    assertThat(payment.amountLeftToSend()).isEqualTo(UnsignedLong.ZERO);
    assertThat(payment.amountDelivered()).isEqualTo(originalAmount);
    assertThat(payment.amountSent()).isEqualTo(originalAmount);
  }

  public static class TestConfig {

    /**
     * Overrides the adminClient bean for test purposes to connect to our Connector container
     *
     * @return a ConnectorAdminClient that can speak to the test container connector
     */
    @Bean
    @Primary
    public ConnectorAdminClient adminClient() {
      return ConnectorAdminClient.construct(getInterledgerBaseUri(), template -> {
        template.header(AUTHORIZATION, "Basic " + ADMIN_AUTH_TOKEN);
      });
    }

    @Bean
    @Primary
    public ConnectorRoutesClient routesClient() {
      return ConnectorRoutesClient.construct(getInterledgerBaseUri(), template -> {
        template.header("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
      });
    }

    @Bean
    @Primary
    public PaymentService sendMoneyService(ObjectMapper objectMapper,
                                           ConnectorAdminClient adminClient,
                                           OkHttpClient okHttpClient,
                                           SpspClient spspClient,
                                           HermesPaymentTracker hermesPaymentTracker) {
      return new PaymentService(getInterledgerBaseUri(), objectMapper, adminClient, okHttpClient, spspClient, hermesPaymentTracker);
    }
  }
}
