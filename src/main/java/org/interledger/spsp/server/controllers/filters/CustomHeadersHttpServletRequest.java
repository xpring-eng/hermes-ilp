package org.interledger.spsp.server.controllers.filters;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wrapper class for {@link HttpServletRequest} which allows for custom headers to be added in its constructor.
 * Once this wrapper is initialized, it is immutable.
 *
 * Should be used with {@link CookieAuthenticationFilter} to add an "Authorization" header to any incoming requests
 */
public class CustomHeadersHttpServletRequest extends HttpServletRequestWrapper {

  // holds custom header and value mapping
  private Map<String, String> customHeaders;

  /**
   * Constructs a request object wrapping the given request.
   *
   * @param request the {@link HttpServletRequest} to be wrapped.
   * @throws IllegalArgumentException if the request is null
   */
  public CustomHeadersHttpServletRequest(HttpServletRequest request) {
    super(request);
  }

  /**
   * Constructs a request object wrapping the given request with given custom headers
   * @param request the {@link HttpServletRequest} to be wrapped
   * @param customHeaders the {@link Map<String, String>} of custom headers to include in the request wrapper
   */
  public CustomHeadersHttpServletRequest(HttpServletRequest request, Map<String, String> customHeaders) {
    super(request);
    this.customHeaders = Objects.requireNonNull(customHeaders);
  }

  public String getHeader(String name) {
    // check the custom headers first
    String headerValue = customHeaders.get(name);

    if (headerValue != null){
      return headerValue;
    }

    return ((HttpServletRequest) getRequest()).getHeader(name);
  }

  public Enumeration<String> getHeaders(String name) {
    Set<String> headerValues = new HashSet<>();
    headerValues.add(this.customHeaders.get(name));

    Enumeration<String> underlyingHeaderValues = ((HttpServletRequest) getRequest()).getHeaders(name);
    while (underlyingHeaderValues.hasMoreElements()) {
      headerValues.add(underlyingHeaderValues.nextElement());
    }

    return Collections.enumeration(headerValues);
  }
}
