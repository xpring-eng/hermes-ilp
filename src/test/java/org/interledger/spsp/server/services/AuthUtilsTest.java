package org.interledger.spsp.server.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

/**
 * Unit tests for {@link AuthUtils}.
 */
public class AuthUtilsTest {

  private final String EXPIRED_TEST_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEifQ"
    + ".eyJzdWIiOiIxMjMiLCJpZCI6IjQ1NiIsImdpdGh1YklkIjoiNzg5IiwidHlwZSI6ImF1dGgiLCJpYXQiOjE1ODQ0NTcwMDMsImV4cCI6MTU4NDQ2MDYw"
    + "MywiYXVkIjoic3RhZ2UueHByaW5nLmlvIiwiaXNzIjoiaHR0cHM6Ly9zdGFnZS54cHJpbmcuaW8vcG9ydGFsLyJ9.UF-1dK6XwqvVkOcPBlhyTFY"
    + "aniMEGilKBtH272BvFBE";

  private final String TEST_JWT =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEifQ.eyJzdWIiOiIxMjMiLCJpZCI6IjQ1NiIsImdpdGh1YklkIjoiNzg5IiwidHlwZSI6ImF1dGgiLCJpYXQiOjE1ODQ0NTcwMDMsImV4cCI6MjA4NDQ2MDYwM"
      + "ywiYXVkIjoic3RhZ2UueHByaW5nLmlvIiwiaXNzIjoiaHR0cHM6Ly9zdGFnZS54cHJpbmcuaW8vcG9ydGFsLyJ9.q7EBJPvN1ofZIE0665Gcnq"
      + "YMVbxrLoL3KB1okTiqr6E";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void getAuthorizationAsBearerTokenWithNullToken() {
    expectedException.expect(NullPointerException.class);
    AuthUtils.getBearerTokenFromAuthorizationHeader(null);

  }

  @Test
  public void getAuthorizationAsBearerTokenWithEmptyToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Requests must have a valid Authorization header");
    AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of(""));
  }

  @Test
  public void getAuthorizationAsBearerTokenWithBlankToken() {
    expectedException.expect(BadCredentialsException.class);
    AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of(" "));
  }

  @Test
  public void getAuthorizationAsBearerTokenWithInvalidBearerSpelling() {
    expectedException.expect(BadCredentialsException.class);
    AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of("Bear foo"));
  }

  @Test
  public void getAuthorizationAsBearerTokenWithEmptyBearerToken() {
    assertThat(AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of("Bearer  "))).isEqualTo(" ");
    assertThat(AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of("Bearer f"))).isEqualTo("f");
    assertThat(AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of("Bearer foo"))).isEqualTo("foo");
    assertThat(AuthUtils.getBearerTokenFromAuthorizationHeader(Optional.of("Bearer foo bar"))).isEqualTo("foo bar");
  }

  @Test
  public void getJwtWithNullToken() {
    expectedException.expect(NullPointerException.class);
    AuthUtils.getJwt(null);
  }

  @Test
  public void getgetJwtWithEmptyToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("Requests must have a valid Authorization header");
    AuthUtils.getJwt(Optional.of(""));
  }

  @Test
  public void getgetJwtWithBlankToken() {
    expectedException.expect(BadCredentialsException.class);
    AuthUtils.getJwt(Optional.of(" "));
  }

  @Test
  public void getgetJwtWithInvalidBearerSpelling() {
    expectedException.expect(BadCredentialsException.class);
    AuthUtils.getJwt(Optional.of("Bear foo"));
  }

  @Test
  public void getgetJwtWithExpiredBearerToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("JWT is expired");

    assertThat(AuthUtils.getJwt(Optional.of("Bearer " + EXPIRED_TEST_JWT)));
  }

  @Test
  public void getJwtWithBearerToken() {
    DecodedJWT expectedJwt = JWT.decode(TEST_JWT);
    DecodedJWT actualJwt = AuthUtils.getJwt(Optional.of("Bearer " + TEST_JWT)).get();
    assertThat(actualJwt.getToken()).isEqualTo(expectedJwt.getToken());
  }

}
