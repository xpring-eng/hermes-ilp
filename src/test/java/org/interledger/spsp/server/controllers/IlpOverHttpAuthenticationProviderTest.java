package org.interledger.spsp.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.accounts.AccountId;
import org.interledger.crypto.Decryptor;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IlpOverHttpLinkSettings.AuthType;
import org.interledger.spsp.server.auth.BearerAuthentication;
import org.interledger.spsp.server.auth.IlpOverHttpAuthenticationProvider;
import org.interledger.spsp.server.config.crypto.JksCryptoConfig;
import org.interledger.spsp.server.config.model.SpspServerSettingsFromPropertyFile;
import org.interledger.spsp.server.controllers.IlpOverHttpAuthenticationProviderTest.TestConfiguration;
import org.interledger.spsp.server.model.ImmutableParentAccountSettings;
import org.interledger.spsp.server.model.ModifiableSpspServerSettings;
import org.interledger.spsp.server.model.ParentAccountSettings;
import org.interledger.spsp.server.model.SpspServerSettings;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Base64;
import java.util.function.Supplier;

@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = {JksCryptoConfig.class, TestConfiguration.class}
)
@ActiveProfiles("test")
public class IlpOverHttpAuthenticationProviderTest {

  public static final String ILP_OVER_HTTP_INCOMING =
    IlpOverHttpLinkSettings.ILP_OVER_HTTP + "." + IlpOverHttpLinkSettings.INCOMING + ".";
  public static final String ILP_OVER_HTTP_OUTGOING =
    IlpOverHttpLinkSettings.ILP_OVER_HTTP + "." + IlpOverHttpLinkSettings.OUTGOING + ".";
  public static final AccountId ACCOUNT_ID = AccountId.of("bob");
  private static final String SECRET = Base64.getEncoder().encodeToString("shh".getBytes());
  private static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";
  private static final String JWT_TOKEN =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJib2IifQ.E773YFqatHhCSBQKp8kkqpdqFpFf5DkRxdDR35pd67M";
  private static final String SIMPLE_TOKEN = "bob:" + SECRET;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private IlpOverHttpAuthenticationProvider ilpOverHttpAuthenticationProvider;

  @Autowired
  private Decryptor decryptor;

  @Before
  public void setUp() {
    initMocks(this);
    SpspServerSettings spspServerSettings = ModifiableSpspServerSettings.create();

    final ParentAccountSettings parentAccountSettings = mockParentAccountSettings().build();
    ilpOverHttpAuthenticationProvider = new IlpOverHttpAuthenticationProvider(
      () -> spspServerSettings, decryptor, parentAccountSettings
    );
  }

  @Test
  public void authenticateSimpleWithValidToken() {
    mockParentAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(SIMPLE_TOKEN.getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getPrincipal()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void authenticateSimpleWithInvalidSecret() {
    mockParentAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Authentication failed for principal: bob");
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken("bob:badtoken".getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );

    assertThat(result.isAuthenticated()).isFalse();
  }

  /**
   * This test currently passes as being authenticated because the token is correct, and there's only ever a single
   * account on this receiver at the moment, so it doesn't matter what the "user" or "subject" is set to for SIMPLE
   * auth.
   */
  @Test
  public void authenticateSimpleWithInvalidPrincipal() {
    mockParentAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString());
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(("foo:" + SECRET).getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build());

    assertThat(result.isAuthenticated()).isTrue();
  }

  @Test
  public void authenticateJwtWithValidToken() {
    mockParentAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.JWT_HS_256.toString());
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken(JWT_TOKEN.getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );

    assertThat(result.isAuthenticated()).isTrue();
    assertThat(result.getPrincipal()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void authenticateJwtWithInvalidToken() {
    mockParentAccountSettings()
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.JWT_HS_256.toString());

    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Authentication failed for principal");
    Authentication result = ilpOverHttpAuthenticationProvider.authenticate(BearerAuthentication.builder()
      .isAuthenticated(false)
      .bearerToken("not a jwt".getBytes())
      .hmacSha256(HashCode.fromString("1234"))
      .build()
    );
  }

  private ImmutableParentAccountSettings.Builder mockParentAccountSettings() {
    ImmutableParentAccountSettings.Builder builder = ParentAccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .accountId(ACCOUNT_ID)
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString())
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, ACCOUNT_ID.value())
      .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.SHARED_SECRET, ENCRYPTED_SHH)

      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.AUTH_TYPE, AuthType.SIMPLE.toString())
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, ACCOUNT_ID.value())
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.SHARED_SECRET, ENCRYPTED_SHH)
      .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.URL, "http://test.com");

    return builder;
  }

  @EnableConfigurationProperties(SpspServerSettingsFromPropertyFile.class)
  public static class TestConfiguration {

    @Bean
    public Supplier<SpspServerSettings> spspServerSettings(SpspServerSettingsFromPropertyFile settings) {
      return () -> settings;
    }

  }
}
