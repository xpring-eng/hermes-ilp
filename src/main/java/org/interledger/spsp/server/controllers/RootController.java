package org.interledger.spsp.server.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.interledger.spsp.server.config.model.HermesSettingsResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

/**
 * A RESTful controller for returning meta-data about the currently running runtime.
 */
@RestController
public class RootController {

  @Autowired
  private BuildProperties buildProperties;

  @Value("${interledger.connector.connector-url}")
  private String ilpOverHttpUrl;

  @RequestMapping(
    method = RequestMethod.GET,
    produces = {APPLICATION_JSON_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public ResponseEntity<HermesSettingsResponse> getServerSettings() {
    return new ResponseEntity<>(
      HermesSettingsResponse.builder()
        .version(this.buildProperties.getVersion())
        .ilpOverHttpUrl(ilpOverHttpUrl)
        .build(),
      HttpStatus.OK
    );
  }
}
