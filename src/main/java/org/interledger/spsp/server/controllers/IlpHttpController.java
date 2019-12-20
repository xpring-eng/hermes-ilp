package org.interledger.spsp.server.controllers;

import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.INTERLEDGER_SPSP_SERVER_PARENT_ACCOUNT;
import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.LINK_TYPE;

import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.server.model.SpspServerSettings;
import org.interledger.stream.Denomination;
import org.interledger.stream.receiver.StreamReceiver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

/**
 * A RESTful controller for handling ILP over HTTP request/response payloads.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
@RestController
@ConditionalOnProperty(prefix = INTERLEDGER_SPSP_SERVER_PARENT_ACCOUNT, name = LINK_TYPE, havingValue = IlpOverHttpLink.LINK_TYPE_STRING)
public class IlpHttpController {

  public static final String ILP_PATH = "/ilp";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  private StreamReceiver streamReceiver;

  @Autowired
  private SpspServerSettings spspServerSettings;

  /**
   * This handler conforms to the ILP-over-HTTP RFC, accepting an OER-encoded payload in the body of the request.
   *
   * @param preparePacket An {@link InterledgerPreparePacket} containing information about an ILP `sendPacket` request.
   *
   * @return All ILP Packets MUST be returned with the HTTP status code 200: OK. An endpoint MAY return standard HTTP
   *   errors, including but not limited to: a malformed or unauthenticated request, rate limiting, or an unresponsive
   *   upstream service. Connectors SHOULD either retry the request, if applicable, or relay an ILP Reject packet back
   *   to the original sender with an appropriate Final or Temporary error code.
   */
  @RequestMapping(
    value = ILP_PATH, method = {RequestMethod.POST},
    produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE},
    consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
  )
  public InterledgerResponsePacket sendData(
    Authentication authentication, @RequestBody final InterledgerPreparePacket preparePacket
  ) {
    final Denomination denomination = Denomination.builder()
      .assetScale((short) spspServerSettings.parentAccountSettings().assetScale())
      .assetCode(spspServerSettings.parentAccountSettings().assetCode())
      .build();

    // TODO: Remove once https://github.com/hyperledger/quilt/issues/378 is fixed.
    if(preparePacket.getData().length <= 0){
      return InterledgerRejectPacket.builder()
        .triggeredBy(spspServerSettings.operatorAddress())
        .code(InterledgerErrorCode.F06_UNEXPECTED_PAYMENT)
        .message("No STREAM frames in Prepare packet")
        .build();
    }

    return streamReceiver.receiveMoney(preparePacket, spspServerSettings.operatorAddress(), denomination)
      .map(fulfillPacket -> {
          logger.info("Packet fulfilled! preparePacket={} fulfillPacket={}", preparePacket, fulfillPacket);
          return fulfillPacket;
        },
        rejectPacket -> {
          logger.info("Packet fulfilled! preparePacket={} fulfillPacket={}", preparePacket, rejectPacket);
          return rejectPacket;
        }
      );
  }

}
