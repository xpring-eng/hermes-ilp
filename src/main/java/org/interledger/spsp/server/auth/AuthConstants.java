package org.interledger.spsp.server.auth;

/**
 * @deprecated Remove this once the version of this in `java-ilp-connector` is extracted per #457.
 * @see "https://github.com/sappenin/java-ilpv4-connector/issues/457"
 */
@Deprecated
public interface AuthConstants {

  interface Authorities {
    String CONNECTOR_ADMIN = "connector:admin";
  }
}
