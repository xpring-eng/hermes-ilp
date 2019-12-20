package org.interledger.spsp.server.config.ilp;

import static okhttp3.CookieJar.NO_COOKIES;
import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.INTERLEDGER_SPSP_SERVER_PARENT_ACCOUNT;
import static org.interledger.spsp.server.config.crypto.CryptoConfigConstants.LINK_TYPE;

import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.spsp.server.model.ParentAccountSettings;
import org.interledger.spsp.server.model.SpspServerSettings;

import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * <p>Configures ILP-over-HTTP, which provides a single Link-layer mechanism for this Connector's peers.</p>
 *
 * <p>This type of Connector supports a single HTTP/2 Server endpoint in addition to multiple, statically configured,
 * ILP-over-HTTP client links.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = INTERLEDGER_SPSP_SERVER_PARENT_ACCOUNT, name = LINK_TYPE, havingValue = IlpOverHttpLink.LINK_TYPE_STRING)
public class IlpOverHttpConfig {

  public static final String ILP_OVER_HTTP = "ILP-over-HTTP";

  @Autowired
  private Supplier<SpspServerSettings> serverSettingsSupplier;

  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected ConnectionPool ilpOverHttpConnectionPool(
    @Value("${interledger.spspServer.ilpOverHttp.connectionDefaults.maxIdleConnections:5}") final int defaultMaxIdleConnections,
    @Value("${interledger.spspServer.ilpOverHttp.connectionDefaults.keepAliveMinutes:1}") final long defaultConnectionKeepAliveMinutes
  ) {
    return new ConnectionPool(
      defaultMaxIdleConnections,
      defaultConnectionKeepAliveMinutes, TimeUnit.MINUTES
    );
  }

  /**
   * A Bean for {@link OkHttp3ClientHttpRequestFactory}.
   *
   * @param ilpOverHttpConnectionPool   A {@link ConnectionPool} as configured above.
   * @param defaultConnectTimeoutMillis Applied when connecting a TCP socket to the target host. A value of 0 means no
   *                                    timeout, otherwise values must be between 1 and {@link Integer#MAX_VALUE} when
   *                                    converted to milliseconds. If unspecified, defaults to 10000.
   * @param defaultReadTimeoutMillis    Applied to both the TCP socket and for individual read IO operations. A value of
   *                                    0 means no timeout, otherwise values must be between 1 and {@link
   *                                    Integer#MAX_VALUE} when converted to milliseconds. If unspecified, defaults to
   *                                    60000.
   * @param defaultWriteTimeoutMillis   Applied to individual write IO operations. A value of 0 means no timeout,
   *                                    otherwise values must be between 1 and {@link Integer#MAX_VALUE} when converted
   *                                    to milliseconds. If unspecified, defaults to 60000.
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttpClient ilpOverHttpClient(
    @Qualifier(ILP_OVER_HTTP) final ConnectionPool ilpOverHttpConnectionPool,
    @Value("${interledger.spspServer.ilpOverHttp.connectionDefaults.connectTimeoutMillis:1000}") final long defaultConnectTimeoutMillis,
    @Value("${interledger.spspServer.ilpOverHttp.connectionDefaults.readTimeoutMillis:60000}") final long defaultReadTimeoutMillis,
    @Value("${interledger.spspServer.ilpOverHttp.connectionDefaults.writeTimeoutMillis:60000}") final long defaultWriteTimeoutMillis
  ) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).build();

    builder.connectionSpecs(Arrays.asList(spec, ConnectionSpec.CLEARTEXT));
    builder.cookieJar(NO_COOKIES);

    builder.connectTimeout(defaultConnectTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.readTimeout(defaultReadTimeoutMillis, TimeUnit.MILLISECONDS);
    builder.writeTimeout(defaultWriteTimeoutMillis, TimeUnit.MILLISECONDS);

    return builder.connectionPool(ilpOverHttpConnectionPool).build();
  }

  /**
   * A Bean for {@link OkHttp3ClientHttpRequestFactory}.
   *
   * @param okHttpClient A {@link OkHttpClient} to use in the Request factory.
   *
   * @return A {@link OkHttp3ClientHttpRequestFactory}.
   */
  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected OkHttp3ClientHttpRequestFactory ilpOverHttpClientHttpRequestFactory(
    @Qualifier(ILP_OVER_HTTP) final OkHttpClient okHttpClient
  ) {
    return new OkHttp3ClientHttpRequestFactory(okHttpClient);
  }

  @Bean
  @Qualifier(ILP_OVER_HTTP)
  protected IlpOverHttpLinkSettings ilpOverHttpLinkSettings() {
    final ParentAccountSettings parentAccountSettings = serverSettingsSupplier.get().parentAccountSettings();
    return IlpOverHttpLinkSettings
      .fromCustomSettings(parentAccountSettings.customSettings())
      .build();
  }
}
