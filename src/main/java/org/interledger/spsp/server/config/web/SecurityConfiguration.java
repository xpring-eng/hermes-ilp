package org.interledger.spsp.server.config.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

@Configuration
@EnableWebSecurity
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

  @Autowired
  SecurityProblemSupport problemSupport;

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

    // WARNING: Don't add `denyAll` here...it's taken care of after the JWT security below. To verify, turn on debugging
    // for Spring Security (e.g.,  org.springframework.security: DEBUG) and look at the security filter chain).

    http
      .authorizeRequests()
      .anyRequest().permitAll()
      //.antMatchers(HttpMethod.GET, "/**").permitAll()
      .and()
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

}
