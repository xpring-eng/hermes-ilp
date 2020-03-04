package org.interledger.spsp.server.config.crypto;

import static org.interledger.spsp.server.config.ConfigConstants.DOT;
import static org.interledger.spsp.server.config.ConfigConstants.INTERLEDGER_HERMES;

public interface CryptoConfigConstants {

  String INTERLEDGER_HERMES_KEYSTORE = INTERLEDGER_HERMES + DOT + "keystore";

  String INTERLEDGER_HERMES_KEYSTORE_GCP = INTERLEDGER_HERMES_KEYSTORE + ".gcpkms";

  String INTERLEDGER_HERMES_KEYSTORE_LOCATION_ID = INTERLEDGER_HERMES_KEYSTORE_GCP + ".locationId";

  ///////////////
  // JKS
  ///////////////

  String FILENAME = "filename";
  String PASSWORD = "password";
  String LINK_TYPE = "link-type";

  String INTERLEDGER_HERMES_KEYSTORE_JKS = INTERLEDGER_HERMES_KEYSTORE + ".jks";
  String INTERLEDGER_HERMES_KEYSTORE_JKS_FILENAME = INTERLEDGER_HERMES_KEYSTORE_JKS + DOT + FILENAME;
  String INTERLEDGER_HERMES_KEYSTORE_JKS_PASSWORD = INTERLEDGER_HERMES_KEYSTORE_JKS + DOT + PASSWORD;
  String INTERLEDGER_HERMES_KEYSTORE_JKS_SECRET0_ALIAS = INTERLEDGER_HERMES_KEYSTORE_JKS + ".secret0_alias";
  String INTERLEDGER_HERMES_KEYSTORE_JKS_SECRET0_PASSWORD =
    INTERLEDGER_HERMES_KEYSTORE_JKS + ".secret0_password";
}
