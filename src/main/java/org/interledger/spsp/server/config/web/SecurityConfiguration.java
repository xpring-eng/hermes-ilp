package org.interledger.spsp.server.config.web;

import org.interledger.crypto.ByteArrayUtils;
import org.interledger.crypto.Decryptor;
import org.interledger.spsp.server.auth.BearerTokenSecurityContextRepository;
import org.interledger.spsp.server.auth.IlpOverHttpAuthenticationProvider;
import org.interledger.spsp.server.controllers.IlpHttpController;
import org.interledger.spsp.server.model.SpspServerSettings;

import com.auth0.spring.security.api.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import java.util.function.Supplier;

@Configuration
@EnableWebSecurity
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  Supplier<SpspServerSettings> serverSettingsSupplier;

  @Autowired
  SecurityProblemSupport problemSupport;

  @Autowired
  Decryptor decryptor;

  @Bean
  IlpOverHttpAuthenticationProvider ilpOverHttpAuthenticationProvider() {
    return new IlpOverHttpAuthenticationProvider(
      serverSettingsSupplier, decryptor, serverSettingsSupplier.get().parentAccountSettings()
    );
  }

  /**
   * Required for auto-injection of {@link org.springframework.security.core.Authentication} into controllers.
   *
   * @see "https://github.com/spring-projects/spring-security/issues/4011"
   */
  @Bean
  public SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter() {
    return new SecurityContextHolderAwareRequestFilter();
  }

  @Override
  public void configure(final HttpSecurity http) throws Exception {

    byte[] ephemeralBytes = ByteArrayUtils.generate32RandomBytes();

    // Must come first in order to register properly due to 'denyAll' directive below.
    configureBearerTokenSecurity(http, ephemeralBytes)
      .authorizeRequests()
      //////
      // ILP-over-HTTP
      //////
      .antMatchers(HttpMethod.HEAD, IlpHttpController.ILP_PATH).authenticated()
      .antMatchers(HttpMethod.POST, IlpHttpController.ILP_PATH).authenticated()
    //.antMatchers(HttpMethod.GET, METRICS_ENDPOINT_URL_PATH).permitAll() // permitAll if hidden by LB.
    ;


    // WARNING: Don't add `denyAll` here...it's taken care of after the JWT security below. To verify, turn on debugging
    // for Spring Security (e.g.,  org.springframework.security: DEBUG) and look at the security filter chain).

    http
      //.httpBasic()
      //.and()
      //.authorizeRequests()

      // @formatter:off

      ////////
      // SPSP Endpoints
      ////////

      // Actuator URLs
      //.antMatchers(HttpMethod.GET, "/**").permitAll()

      // Everything else...
      //.anyRequest().denyAll()

      //.and()
      .addFilter(securityContextHolderAwareRequestFilter())
      .cors()
      .and()
      .formLogin().disable()
      .logout().disable()
      //.anonymous().disable()
      .jee().disable()
      //.authorizeRequests()
      //.antMatchers(HttpMethod.GET, HealthController.SLASH_AH_SLASH_HEALTH).permitAll()
      //.anyRequest().denyAll()
      //.and()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER).enableSessionUrlRewriting(false)
      .and()
      .exceptionHandling().authenticationEntryPoint(problemSupport).accessDeniedHandler(problemSupport);

    // @formatter:on
  }

  private HttpSecurity configureBearerTokenSecurity(HttpSecurity http, byte[] ephemeralBytes) throws Exception {
    return http
      .authenticationProvider(ilpOverHttpAuthenticationProvider())
      .securityContext()
      .securityContextRepository(new BearerTokenSecurityContextRepository(ephemeralBytes))
      .and()
      .exceptionHandling()
      .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
      .and()
      .httpBasic().disable()
      .csrf().disable()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and();
  }

}
