package org.interledger.spsp.server.controllers.filters;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wrapper class for {@link HttpServletRequest} which allows for mutability of headers.
 *
 * Should be used with {@link CookieAuthenticationFilter} to add an "Authorization" header to any incoming requests
 */
final class MutableHttpServletRequest extends HttpServletRequestWrapper {
  // holds custom header and value mapping
  private final Map<String, String> customHeaders;

  public MutableHttpServletRequest(HttpServletRequest request){
    super(request);
    this.customHeaders = new HashMap<String, String>();
  }

  public void putHeader(String name, String value){
    this.customHeaders.put(name, value);
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