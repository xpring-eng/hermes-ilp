package org.interledger.spsp.server.controllers;

import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

public abstract class AbstractController {

  @Autowired
  private HttpServletRequest request;

  public String getBearerToken() {
    return request.getHeader("Authorization");
  }
}
