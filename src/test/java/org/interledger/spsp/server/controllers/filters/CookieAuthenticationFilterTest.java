package org.interledger.spsp.server.controllers.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Unit tests for {@link CookieAuthenticationFilter}.
 */
public class CookieAuthenticationFilterTest {

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private MockFilterChain filterChain;

  private CookieAuthenticationFilter filter;

  @Before
  public void setUp() {
    this.filter = new CookieAuthenticationFilter();
    this.request = new MockHttpServletRequest();
    this.response = new MockHttpServletResponse();
    this.filterChain = new MockFilterChain();
  }

  @Test
  public void doFilterWithNoAuthorizationHeader() throws IOException, ServletException {
    filter.doFilter(request, response, filterChain);
    assertThat(((HttpServletRequest) filterChain.getRequest()).getHeader(AUTHORIZATION)).isNull();
  }

  @Test
  public void doFilterWithAuthorizationHeader() throws IOException, ServletException {
    request.addHeader(AUTHORIZATION, "Bearer foo");
    filter.doFilter(request, response, filterChain);
    assertThat(((HttpServletRequest) filterChain.getRequest()).getHeader(AUTHORIZATION)).isEqualTo("Bearer foo");
  }

  @Test
  public void doFilterWithCookieAndNoAuthHeader() throws IOException, ServletException {
    request.setCookies(new MockCookie("jwt", "bar"));
    filter.doFilter(request, response, filterChain);
    assertThat(((HttpServletRequest) filterChain.getRequest()).getHeader(AUTHORIZATION)).isEqualTo("Bearer bar");
  }

  @Test
  public void doFilterWithCookieAndAuthHeader() throws IOException, ServletException {
    request.addHeader(AUTHORIZATION, "Bearer foo");
    request.setCookies(new MockCookie("jwt", "bar"));
    filter.doFilter(request, response, filterChain);
    assertThat(((HttpServletRequest) filterChain.getRequest()).getHeader(AUTHORIZATION)).isEqualTo("Bearer foo");
  }
}
