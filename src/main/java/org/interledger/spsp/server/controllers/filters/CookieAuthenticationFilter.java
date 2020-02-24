package org.interledger.spsp.server.controllers.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Request filter which will grab the cookie named "jwt" (if it is present) and add it to the servlet request
 * as an "Authorization" header.
 *
 * In order to integrate with the existing Xpring wallet auth model, which sends JWTs in Cookies, this filter needs to
 * be present.
 */
@Component
@Order(1)
public class CookieAuthenticationFilter implements Filter {

  private static final String JWT_COOKIE_NAME = "jwt";
  public static final String BEARER_SPACE = "Bearer ";
  public static final String AUTHORIZATION = "Authorization";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest((HttpServletRequest) servletRequest);

    Cookie[] cookies = mutableRequest.getCookies();
    if (Objects.nonNull(cookies)) {
      // Get a jwt from a cookie if the cookie exists
      Optional<String> jwt = Arrays.stream(cookies)
        .filter(c -> c.getName().equals(JWT_COOKIE_NAME))
        .map(Cookie::getValue)
        .findFirst();

      if (jwt.isPresent()) {
        // jwt cookie exists, so add the jwt as a header
        mutableRequest.putHeader(AUTHORIZATION, BEARER_SPACE + jwt.get());
      } else {
        logger.debug("No jwt cookie found in request. Using existing token from Authorization header.");
      }
    } else {
      logger.debug("No jwt cookie found in request. Using existing token from Authorization header.");
    }

    filterChain.doFilter(mutableRequest, servletResponse);
  }
}
