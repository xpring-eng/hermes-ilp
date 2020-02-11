package org.interledger.spsp.server.controllers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import javax.servlet.http.HttpServletRequest;

public abstract class AbstractController {

  private static Logger logger = LoggerFactory.getLogger(AbstractController.class);
  @Autowired
  private HttpServletRequest request;

  public String getAuthorization() {
    return request.getHeader("Authorization");
  }

  public String getBearerToken() {
    return getAuthorization().substring(7);
  }

  public DecodedJWT getJwt() {
    try {
      DecodedJWT jwt = JWT.decode(getBearerToken());
      if (jwt.getExpiresAt().before(new Date())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "JWT is expired");
      }
      return jwt;
    } catch (JWTDecodeException e) {
      logger.debug("Unabled to decode JWT.  Defaulting to SIMPLE auth");
      return null;
    }
  }
}
