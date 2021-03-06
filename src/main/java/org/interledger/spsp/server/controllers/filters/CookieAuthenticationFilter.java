package org.interledger.spsp.server.controllers.filters;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.interledger.spsp.server.model.BearerToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Request filter which will grab the cookie named "jwt" (if it is present) and add it to the servlet request as an
 * "Authorization" header.
 *
 * In order to integrate with the existing Xpring wallet auth model, which sends JWTs in Cookies, this filter needs to
 * be present.
 */
@Component
@Order(0)
public class CookieAuthenticationFilter extends HttpFilter implements Filter {

  private static final String JWT_COOKIE_NAME = "jwt";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void doFilter(
    HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain
  ) throws IOException, ServletException {
    Optional<ServletRequest> immutableHttpServletRequest = Optional.empty();

    // Give the "Authorization" header precedence over a jwt Cookie
    if (Objects.isNull(servletRequest.getHeader(AUTHORIZATION))) {
      // If there is no "Authorization" header, try to get a JWT from a Cookie
      Cookie[] cookies = servletRequest.getCookies();
      if (Objects.nonNull(cookies)) {
        // Get a jwt from a cookie if the cookie exists
        immutableHttpServletRequest = Optional.ofNullable(
          Arrays.stream(cookies)
            .filter(c -> c.getName().equals(JWT_COOKIE_NAME))
            .map(Cookie::getValue)
            .findFirst()
            .map(BearerToken::fromRawToken)
            .map(bearerToken -> {
              // jwt cookie exists, so create a new request with JWT as "Authorization" header
              Map<String, String> customHeaders = new HashMap<>();
              customHeaders.put(AUTHORIZATION, bearerToken.toString());
              return new CustomHeadersHttpServletRequest(servletRequest, customHeaders);
            }).orElse(null)
        );

      } else {
        logger.debug("No jwt cookie found in request, so did not modify incoming request's Authorization header.");
      }
    } else {
      logger.debug("Authorization header exists, so did not look for a JWT cookie.");
    }

    filterChain.doFilter(immutableHttpServletRequest.orElse(servletRequest), servletResponse);
  }
}
