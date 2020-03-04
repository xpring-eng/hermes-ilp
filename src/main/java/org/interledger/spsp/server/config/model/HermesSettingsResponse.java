package org.interledger.spsp.server.config.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableHermesSettingsResponse.class)
public interface HermesSettingsResponse {

  static ImmutableHermesSettingsResponse.Builder builder() {
    return ImmutableHermesSettingsResponse.builder();
  }

  String version();
}
