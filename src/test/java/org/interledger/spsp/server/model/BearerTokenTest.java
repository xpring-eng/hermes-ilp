package org.interledger.spsp.server.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * Unit tests for {@link BearerToken}.
 */
public class BearerTokenTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void fromBearerTokenValueWithInvalidPrefix() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("BearerTokens must start with the prefix \"Bearer \"");
    BearerToken.fromBearerTokenValue("foo");
  }

  @Test
  public void fromBearerTokenValueWithShortPrefix() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("BearerTokens must start with the prefix \"Bearer \"");
    BearerToken.fromBearerTokenValue("Bearerfoo");
  }

  @Test
  public void fromBearerTokenValueWithPrefixButNoToken() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("BearerTokens must have a non-empty raw token");
    BearerToken.fromBearerTokenValue("Bearer ");
  }

  @Test
  public void fromBearerTokenValueWithPrefix() {
    assertThat(BearerToken.fromBearerTokenValue("Bearer foo").rawToken()).isEqualTo("foo");
    assertThat(BearerToken.fromBearerTokenValue("Bearer foo").value()).isEqualTo("Bearer foo");
    assertThat(BearerToken.fromBearerTokenValue("Bearer foo").toString()).isEqualTo("Bearer foo");
  }

  @Test
  public void fromBearerTokenValue() {
    expectedException.expect(BadCredentialsException.class);
    expectedException.expectMessage("BearerTokens must start with the prefix \"Bearer \"");
    BearerToken.fromBearerTokenValue("foo");
  }

 @Test
 public void fromRawTokenWithPrefixAndToken() {
   assertThat(BearerToken.fromRawToken("BearerFoo").rawToken()).isEqualTo("BearerFoo");
   assertThat(BearerToken.fromRawToken("BearerFoo ").value()).isEqualTo("Bearer BearerFoo ");
   assertThat(BearerToken.fromRawToken("BearerFoo ").toString()).isEqualTo("Bearer BearerFoo ");
 }

  @Test
  public void fromRawTokenWithPrefixButNoToken() {
    assertThat(BearerToken.fromRawToken("Bearer ").rawToken()).isEqualTo("Bearer ");
    assertThat(BearerToken.fromRawToken("Bearer ").value()).isEqualTo("Bearer Bearer ");
    assertThat(BearerToken.fromRawToken("Bearer ").toString()).isEqualTo("Bearer Bearer ");
  }

  @Test
  public void fromRawTokenWithPrefix() {
    assertThat(BearerToken.fromRawToken("Bearer foo").rawToken()).isEqualTo("Bearer foo");
    assertThat(BearerToken.fromRawToken("Bearer foo").value()).isEqualTo("Bearer Bearer foo");
    assertThat(BearerToken.fromRawToken("Bearer foo").toString()).isEqualTo("Bearer Bearer foo");
  }

  @Test
  public void fromRawToken() {
    assertThat(BearerToken.fromRawToken("foo").rawToken()).isEqualTo("foo");
    assertThat(BearerToken.fromRawToken("foo").value()).isEqualTo("Bearer foo");
    assertThat(BearerToken.fromRawToken("foo").toString()).isEqualTo("Bearer foo");
  }
}
