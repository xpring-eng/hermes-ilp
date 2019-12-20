package org.interledger.spsp.server.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.core.InterledgerAddress;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.spsp.server.model.SpspServerSettings;
import org.interledger.stream.receiver.StreamReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UrlPathHelper;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

@RestController
public class SpspController {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final StreamReceiver streamReceiver;
  private final UrlPathHelper urlPathHelper;
  private final Supplier<SpspServerSettings> spspServerSettingsSupplier;

  public SpspController(
    final Supplier<SpspServerSettings> spspServerSettingsSupplier,
    final StreamReceiver streamReceiver
  ) {
    this.streamReceiver = Objects.requireNonNull(streamReceiver);
    this.spspServerSettingsSupplier = spspServerSettingsSupplier;
    this.urlPathHelper = new UrlPathHelper();
  }

  /**
   * A simple SPSP endpoint that merely returns a new Shared Secret and destination address to support a stateless
   * receiver.
   *
   * @param accountId The SPSP path, as a {@link String}.
   *
   * @return
   */
  @RequestMapping(
    path = "/{account_id}/**", method = RequestMethod.GET,
    produces = {"application/spsp4+json", APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<StreamConnectionDetails> getSpspResponse(
    @PathVariable("account_id") final String accountId,
    final HttpServletRequest servletRequest
  ) {
    Objects.requireNonNull(accountId);

    String paymentTarget = this.urlPathHelper.getPathWithinApplication(servletRequest).replaceAll("/", ".");
    if (paymentTarget.startsWith(".")) {
      paymentTarget = paymentTarget.replaceFirst(".", "");
    }
    if (paymentTarget.endsWith(".")) {
      paymentTarget = paymentTarget.substring(0, paymentTarget.length() - 1);
    }

    final InterledgerAddress paymentReceiverAddress = spspServerSettingsSupplier.get().operatorAddress()
      .with(paymentTarget);

    final StreamConnectionDetails connectionDetails = streamReceiver.setupStream(paymentReceiverAddress);
    final org.interledger.spsp.server.model.StreamConnectionDetails returnableStreamConnectionDetails =
      org.interledger.spsp.server.model.StreamConnectionDetails.builder()
        .destinationAddress(connectionDetails.destinationAddress())
        .sharedSecretBase64(Base64.getEncoder().encodeToString(connectionDetails.sharedSecret().key()))
        .build();
    final HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // TODO: Add client-cache directive per RFC (i.e., configurable max-age).
    return new ResponseEntity(returnableStreamConnectionDetails, headers, HttpStatus.OK);
  }
}
