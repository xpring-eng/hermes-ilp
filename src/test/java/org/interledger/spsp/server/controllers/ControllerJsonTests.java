package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.link.LinkType;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.server.client.AccountBalance;
import org.interledger.spsp.server.client.AccountBalanceResponse;
import org.interledger.spsp.server.client.AccountSettingsResponse;
import org.interledger.spsp.server.client.CreateAccountRestRequest;
import org.interledger.spsp.server.client.PaymentRequest;
import org.interledger.spsp.server.client.PaymentResponse;
import org.interledger.spsp.server.config.jackson.ObjectMapperFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;

import java.time.Instant;

public class ControllerJsonTests {

  public static final String paymentPointer = "$example.com/foo";
  private static final String accountId = "foo";
  private static final AccountRelationship peer = AccountRelationship.PEER;
  private static final LinkType linkType = IlpOverHttpLink.LINK_TYPE;
  private static final String assetCode = "XRP";
  private static final int assetScale = 9;
  private static final int oneThousand = 1000;
  public static final Instant now = Instant.now();

  ObjectMapper objectMapper = ObjectMapperFactory.create();
  private BasicJsonTester jsonTester = new BasicJsonTester(this.getClass());

  @Test
  public void testAccountSettingsResponseJson() throws JsonProcessingException {

    AccountSettingsResponse accountSettings = AccountSettingsResponse.builder()
      .paymentPointer(PaymentPointer.of(paymentPointer))
      .createdAt(now)
      .modifiedAt(now)
      .accountId(AccountId.of(accountId))
      .isInternal(true)
      .accountRelationship(peer)
      .linkType(linkType)
      .assetCode(assetCode)
      .assetScale(assetScale)
      .maximumPacketAmount(UnsignedLong.valueOf(oneThousand))
      .build();

    String serialized = objectMapper.writeValueAsString(accountSettings);
    JsonContentAssert assertJson = assertThat(jsonTester.from(serialized));
    assertJson.extractingJsonPathValue("paymentPointer").isEqualTo(paymentPointer);
    assertJson.extractingJsonPathValue("createdAt").isEqualTo(now.toString());
    assertJson.extractingJsonPathValue("modifiedAt").isEqualTo(now.toString());
    assertJson.extractingJsonPathValue("accountId").isEqualTo(accountId);
    assertJson.extractingJsonPathValue("internal").isEqualTo(true);
    assertJson.extractingJsonPathValue("accountRelationship").isEqualTo(peer.toString());
    assertJson.extractingJsonPathValue("linkType").isEqualTo(linkType.value());
    assertJson.extractingJsonPathValue("assetCode").isEqualTo(assetCode);
    assertJson.extractingJsonPathValue("assetScale").isEqualTo(String.valueOf(assetScale));
    assertJson.extractingJsonPathValue("maximumPacketAmount").isEqualTo(String.valueOf(oneThousand));

    AccountSettingsResponse deserialized = objectMapper.readValue(serialized, AccountSettingsResponse.class);
    assertThat(deserialized.accountId()).isEqualTo(AccountId.of(accountId));
    assertThat(deserialized.paymentPointer()).isEqualTo(PaymentPointer.of(paymentPointer));
    assertThat(deserialized.createdAt()).isEqualTo(now);
    assertThat(deserialized.modifiedAt()).isEqualTo(now);
    assertThat(deserialized.isInternal()).isEqualTo(true);
    assertThat(deserialized.accountRelationship()).isEqualTo(peer);
    assertThat(deserialized.linkType()).isEqualTo(linkType);
    assertThat(deserialized.assetCode()).isEqualTo(assetCode);
    assertThat(deserialized.assetScale()).isEqualTo(assetScale);
  }

  @Test
  public void testCreateAccountRestRequestJson() throws JsonProcessingException {
    CreateAccountRestRequest request = CreateAccountRestRequest.builder(assetCode, assetScale)
      .accountId(accountId)
      .build();

    String serialized = objectMapper.writeValueAsString(request);
    JsonContentAssert assertJson = assertThat(jsonTester.from(serialized));
    assertJson.extractingJsonPathValue("accountId").isEqualTo(accountId);
    assertJson.extractingJsonPathValue("assetCode").isEqualTo(assetCode);
    assertJson.extractingJsonPathValue("assetScale").isEqualTo(String.valueOf(assetScale));
    assertJson.extractingJsonPathValue("description").isEqualTo("");

    CreateAccountRestRequest deserialized = objectMapper.readValue(serialized, CreateAccountRestRequest.class);
    assertThat(deserialized.accountId()).isEqualTo(accountId);
    assertThat(deserialized.assetCode()).isEqualTo(assetCode);
    assertThat(deserialized.assetScale()).isEqualTo(assetScale);
    assertThat(deserialized.description()).isEmpty();
  }

  @Test
  public void testAccountBalanceResponseJson() throws JsonProcessingException {
    AccountBalanceResponse accountBalanceResponse = AccountBalanceResponse.builder()
      .assetCode(assetCode)
      .assetScale(assetScale)
      .accountBalance(AccountBalance.builder()
        .accountId(AccountId.of(accountId))
        .prepaidAmount(oneThousand)
        .clearingBalance(oneThousand)
        .build())
      .build();

    String serialized = objectMapper.writeValueAsString(accountBalanceResponse);
    JsonContentAssert assertJson = assertThat(jsonTester.from(serialized));
    assertJson.extractingJsonPathValue("assetCode").isEqualTo(assetCode);
    assertJson.extractingJsonPathValue("assetScale").isEqualTo(String.valueOf(assetScale));
    assertJson.extractingJsonPathValue("$.accountBalance.accountId").isEqualTo(accountId);
    assertJson.extractingJsonPathValue("$.accountBalance.prepaidAmount").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("$.accountBalance.clearingBalance").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("$.accountBalance.netBalance").isEqualTo(String.valueOf(oneThousand + oneThousand));

    AccountBalanceResponse deserialized = objectMapper.readValue(serialized, AccountBalanceResponse.class);
    assertThat(deserialized.assetCode()).isEqualTo(assetCode);
    assertThat(deserialized.assetScale()).isEqualTo(assetScale);
    assertThat(deserialized.accountBalance().accountId()).isEqualTo(AccountId.of(accountId));
    assertThat(deserialized.accountBalance().prepaidAmount()).isEqualTo(oneThousand);
    assertThat(deserialized.accountBalance().clearingBalance()).isEqualTo(oneThousand);
    assertThat(deserialized.accountBalance().netBalance()).isEqualTo(oneThousand + oneThousand);


  }

  @Test
  public void testPaymentRequestJson() throws JsonProcessingException {
    PaymentRequest paymentRequest = PaymentRequest.builder()
      .amount(UnsignedLong.valueOf(oneThousand))
      .destinationPaymentPointer(paymentPointer)
      .build();

    String serialized = objectMapper.writeValueAsString(paymentRequest);
    JsonContentAssert assertJson = assertThat(jsonTester.from(serialized));
    assertJson.extractingJsonPathValue("amount").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("destinationPaymentPointer").isEqualTo(paymentPointer);

    PaymentRequest deserialized = objectMapper.readValue(serialized, PaymentRequest.class);
    assertThat(deserialized.amount()).isEqualTo(UnsignedLong.valueOf(oneThousand));
    assertThat(deserialized.destinationPaymentPointer()).isEqualTo(paymentPointer);
  }

  @Test
  public void testPaymentResponseJson() throws JsonProcessingException {
    PaymentResponse paymentResponse = PaymentResponse.builder()
      .originalAmount(UnsignedLong.valueOf(oneThousand))
      .amountSent(UnsignedLong.valueOf(oneThousand))
      .amountDelivered(UnsignedLong.valueOf(oneThousand))
      .successfulPayment(true)
      .build();

    String serialized = objectMapper.writeValueAsString(paymentResponse);
    JsonContentAssert assertJson = assertThat(jsonTester.from(serialized));
    assertJson.extractingJsonPathValue("originalAmount").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("amountSent").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("amountDelivered").isEqualTo(String.valueOf(oneThousand));
    assertJson.extractingJsonPathValue("successfulPayment").isEqualTo(true);

    PaymentResponse deserialized = objectMapper.readValue(serialized, PaymentResponse.class);
    assertThat(deserialized.originalAmount()).isEqualTo(UnsignedLong.valueOf(oneThousand));
    assertThat(deserialized.amountSent()).isEqualTo(UnsignedLong.valueOf(oneThousand));
    assertThat(deserialized.amountDelivered()).isEqualTo(UnsignedLong.valueOf(oneThousand));
    assertThat(deserialized.successfulPayment()).isEqualTo(true);
  }


}
