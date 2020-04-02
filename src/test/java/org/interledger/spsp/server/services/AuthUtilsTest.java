package org.interledger.spsp.server.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.spsp.server.model.BearerToken;

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
  public void getJwtWithNullToken() {
    expectedException.expect(NullPointerException.class);
    AuthUtils.getJwt(null);
  }

  @Test
  public void getgetJwtWithEmptyToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("BearerTokens must have a non-empty raw token");
    AuthUtils.getJwt(Optional.of(BearerToken.fromRawToken("")));
  }

  @Test
  public void getgetJwtWithBlankToken() {
    assertThat(AuthUtils.getJwt(Optional.of(BearerToken.fromRawToken(" ")))).isEmpty();
  }

  @Test
  public void getgetJwtWithInvalidBearerSpelling() {
    expectedException.expect(BadCredentialsException.class);
    AuthUtils.getJwt(Optional.of(BearerToken.fromBearerTokenValue("Bear foo")));
  }

  @Test
  public void getgetJwtWithExpiredBearerToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("JWT is expired");

    assertThat(AuthUtils.getJwt(Optional.of(BearerToken.fromBearerTokenValue("Bearer " + EXPIRED_TEST_JWT))));
  }

  @Test
  public void getJwtWithBearerToken() {
    DecodedJWT expectedJwt = JWT.decode(TEST_JWT);
    DecodedJWT actualJwt = AuthUtils.getJwt(Optional.of(BearerToken.fromBearerTokenValue("Bearer " + TEST_JWT))).get();
    assertThat(actualJwt.getToken()).isEqualTo(expectedJwt.getToken());
  }

}
