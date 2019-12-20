package org.interledger.spsp.server.config.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.settings.ConnectorKey;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.server.model.ParentAccountSettings;
import org.interledger.spsp.server.model.SpspServerSettings;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Unit test to validate loading of properties into a {@link SpspServerSettingsFromPropertyFile}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpspServerSettingsFromPropertyFileTest.TestConfiguration.class)
@ActiveProfiles("unit-test")
public class SpspServerSettingsFromPropertyFileTest {

  @Autowired
  SpspServerSettingsFromPropertyFile spspServerSettings;

  @Test
  public void testConfig() {
    assertThat(spspServerSettings.operatorAddress()).isEqualTo((InterledgerAddress.of("test.example")));

    assertThat(spspServerSettings.isRequire32ByteSharedSecrets()).isTrue();

    assertThat(spspServerSettings.keys().secret0())
      .isEqualTo(ConnectorKey.builder().alias("secret0").version("2").build());
    assertThat(spspServerSettings.keys().accountSettings())
      .isEqualTo(ConnectorKey.builder().alias("accounts").version("3").build());

    /////////////////////////
    // Parent Account Settings
    /////////////////////////
    {
      final ParentAccountSettings parentAccount = spspServerSettings.parentAccountSettings();

      // TODO: FIXME AccountId comparison should work
      //assertThat(parentAccount.accountId()).isEqualTo(AccountId.of("alice"));
      assertThat(parentAccount.accountId().value()).isEqualTo("alice");
      assertThat(parentAccount.assetCode()).isEqualTo("XRP");
      assertThat(parentAccount.assetScale()).isEqualTo(9);
      assertThat(parentAccount.linkType()).isEqualTo(IlpOverHttpLink.LINK_TYPE);

      assertThat(parentAccount.maximumPacketAmount()).isEqualTo(Optional.of(UnsignedLong.valueOf(100001)));

//      assertThat(parentAccount.settlementEngineDetails().get().baseUrl()).isEqualTo(IlpOverHttpLink.LINK_TYPE);
//      assertThat(parentAccount.settlementEngineDetails().get().settlementEngineAccountId()).isEqualTo(IlpOverHttpLink.LINK_TYPE);
//      assertThat(parentAccount.settlementEngineDetails().get().customSettings().get("foo")).isEqualTo(IlpOverHttpLink.LINK_TYPE);
//      assertThat(parentAccount.settlementEngineDetails().get().customSettings().get("bar")).isEqualTo(IlpOverHttpLink.LINK_TYPE);

      assertThat(parentAccount.balanceSettings().minBalance()).isEqualTo(Optional.of(1L));
      assertThat(parentAccount.balanceSettings().settleTo()).isEqualTo(3L);
      assertThat(parentAccount.balanceSettings().settleThreshold())
        .isEqualTo(Optional.of(10000001L));

      assertThat(parentAccount.customSettings().get("foo")).isEqualTo("bar");
      assertThat(parentAccount.customSettings().get("boo")).isEqualTo("baz");

      final AccountBalanceSettings balanceSettings = parentAccount.balanceSettings();
      assertThat(balanceSettings.minBalance()).isEqualTo(Optional.of(1L));
      assertThat(balanceSettings.settleThreshold()).isEqualTo(Optional.of(10000001L));
      assertThat(balanceSettings.settleTo()).isEqualTo(3L);
    }
  }

  @EnableConfigurationProperties(SpspServerSettingsFromPropertyFile.class)
  public static class TestConfiguration {

    @Bean
    public Supplier<SpspServerSettings> spspServerSettingsSupplier(SpspServerSettingsFromPropertyFile settings) {
      return () -> settings;
    }

  }
}
