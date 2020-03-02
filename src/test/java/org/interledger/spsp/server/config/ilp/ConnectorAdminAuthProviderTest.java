package org.interledger.spsp.server.config.ilp;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.crypto.EncryptionService;
import org.interledger.spsp.server.config.crypto.CryptoConfig;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class, classes = CryptoConfig.class)
@ActiveProfiles("jks")
public class ConnectorAdminAuthProviderTest {

  private static final String PASSWORD = "password";

  private static final String ENCRYPTED_PASSWORD =
    "enc:jks:crypto.p12:secret0:1:aes_gcm:AAAADA-UOIUB-aKutRtIjA5v5Lp7wI4-bhrunfIOpwqypI1DNpruzA==";

  private static final String BASIC_AUTH_BASE64_PASSWORD = "Basic cGFzc3dvcmQ=";

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired
  private EncryptionService encryptionService;

  @Test
  public void getAdminAuthNotEncrypted() {
    assertThat(new ConnectorAdminAuthProvider(PASSWORD, encryptionService).getAdminAuth())
      .isEqualTo(BASIC_AUTH_BASE64_PASSWORD);
  }

  @Test
  public void getAdminAuthEncrypted() {
    assertThat(new ConnectorAdminAuthProvider(ENCRYPTED_PASSWORD, encryptionService).getAdminAuth())
      .isEqualTo(BASIC_AUTH_BASE64_PASSWORD);
  }

}