package io.pivotal.security.config;

import io.pivotal.security.auth.AuditOAuth2AccessDeniedHandler;
import io.pivotal.security.auth.AuditOAuth2AuthenticationExceptionHandler;
import io.pivotal.security.auth.PreAuthenticationFailureFilter;
import io.pivotal.security.auth.X509AuthenticationProvider;
import io.pivotal.security.exceptionHandlers.ActorAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.x509.X509AuthenticationFilter;

@Configuration
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled=true)
public class AuthConfiguration extends ResourceServerConfigurerAdapter {
  // Only valid for v4 UUID by design.
  private static final String VALID_MTLS_ID =
      "\\bOU=(app:[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[a-f0-9]{4}-[a-f0-9]{12})\\b";

  private final ResourceServerProperties resourceServerProperties;
  private final AuditOAuth2AuthenticationExceptionHandler auditOAuth2AuthenticationExceptionHandler;
  private final AuditOAuth2AccessDeniedHandler auditOAuth2AccessDeniedHandler;
  private final PreAuthenticationFailureFilter preAuthenticationFailureFilter;
  private final ActorAccessDeniedHandler actorAccessDeniedHandler;


  @Autowired
  AuthConfiguration(
      ResourceServerProperties resourceServerProperties,
      AuditOAuth2AuthenticationExceptionHandler auditOAuth2AuthenticationExceptionHandler,
      AuditOAuth2AccessDeniedHandler auditOAuth2AccessDeniedHandler,
      PreAuthenticationFailureFilter preAuthenticationFailureFilter,
      ActorAccessDeniedHandler actorAccessDeniedHandler) {
    this.resourceServerProperties = resourceServerProperties;
    this.auditOAuth2AuthenticationExceptionHandler = auditOAuth2AuthenticationExceptionHandler;
    this.auditOAuth2AccessDeniedHandler = auditOAuth2AccessDeniedHandler;
    this.preAuthenticationFailureFilter = preAuthenticationFailureFilter;
    this.actorAccessDeniedHandler = actorAccessDeniedHandler;
  }

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources.resourceId(resourceServerProperties.getResourceId());
    resources.authenticationEntryPoint(auditOAuth2AuthenticationExceptionHandler);
    resources.accessDeniedHandler(auditOAuth2AccessDeniedHandler);
    resources.stateless(false);
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    /*
      Even though the configuration is non order specific, it's ordered here so one can understand
      the flow of operations. Before the Authenticate Override can be called in the http filter
      the subject principal must be correctly extracted, hence why the UserDetails for that sets
      the "NO_AUTHORITIES", leaving it to the x509v3 checker to set the final authority.

      The aggregate of all this is consumed in the final .access() method.
     */

    http.x509()
        .subjectPrincipalRegex(VALID_MTLS_ID)
        .userDetailsService(mtlsSUserDetailsService())
        .withObjectPostProcessor(new ObjectPostProcessor<X509AuthenticationFilter>() {
          @Override
          public <O extends X509AuthenticationFilter> O postProcess(O filter) {
            filter.setContinueFilterChainOnUnsuccessfulAuthentication(false);
            return filter;
          }
        });

    http.addFilterBefore(preAuthenticationFailureFilter, X509AuthenticationFilter.class)
        .authenticationProvider(getPreAuthenticatedAuthenticationProvider());

    http
        .authorizeRequests()
        .antMatchers("/info").permitAll()
        .antMatchers("/health").permitAll()
        .antMatchers("/api/v1/**")
          .access(String.format("hasRole('%s') "
                  + "or (#oauth2.hasScope('credhub.read') and #oauth2.hasScope('credhub.write'))",
              X509AuthenticationProvider.MTLS_USER));

    http.httpBasic().disable();
  }

  private UserDetailsService mtlsSUserDetailsService() {
    return username -> new User(username, "", AuthorityUtils.NO_AUTHORITIES);
  }

  @Bean
  public PreAuthenticatedAuthenticationProvider getPreAuthenticatedAuthenticationProvider() {
    return new X509AuthenticationProvider();
  }

}
