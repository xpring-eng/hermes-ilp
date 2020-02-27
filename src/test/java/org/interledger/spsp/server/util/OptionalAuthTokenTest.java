package org.interledger.spsp.server.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

public class OptionalAuthTokenTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void ofString() {
    assertThat(Optional.of("foo bar")).isNotEmpty().get().isEqualTo("foo bar");
    assertThat(Optional.of("bar")).isNotEmpty().get().isEqualTo("bar");

    // to match method argument type
    String nullString = null;
    assertThat(Optional.ofNullable(nullString)).isEmpty();
  }
}
