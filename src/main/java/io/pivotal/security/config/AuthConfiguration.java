package io.pivotal.security.config;

import io.pivotal.security.oauth.AuditOAuth2AccessDeniedHandler;
import io.pivotal.security.oauth.AuditOAuth2AuthenticationExceptionHandler;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;

@Configuration
@EnableResourceServer
@EnableWebSecurity
public class AuthConfiguration extends ResourceServerConfigurerAdapter {

  @Autowired
  ResourceServerProperties resourceServerProperties;

  @Autowired
  AuditOAuth2AuthenticationExceptionHandler auditOAuth2AuthenticationExceptionHandler;

  @Autowired
  SecurityProperties securityProperties;

  @Autowired
  AuditOAuth2AccessDeniedHandler auditOAuth2AccessDeniedHandler;

//  @PostConstruct
//  public void init() {
//    Assert.notNull(resourceServerProperties.getJwt().getKeyValue(), "Configuration property security.oauth2.resource.jwt.key-value must be set.");
//    securityProperties.getUser().setName(RandomStringUtils.random(12));
//    securityProperties.getUser().setRole(new ArrayList<>());
//  }

  @Override
  public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
    resources.resourceId(resourceServerProperties.getResourceId());
    resources.authenticationEntryPoint(auditOAuth2AuthenticationExceptionHandler);
    resources.accessDeniedHandler(auditOAuth2AccessDeniedHandler);
    resources.stateless(false);
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
        .authorizeRequests()
        .antMatchers("/info").permitAll()
        .antMatchers("/health").permitAll()
        .antMatchers("/api/v1/**").access("hasRole('MTLS_USER') or (#oauth2.hasScope('credhub.read') and #oauth2.hasScope('credhub.write'))");

    http.x509().subjectPrincipalRegex("CN=(.*?)(?:,|$)").userDetailsService(mTLSUserDetailsService());

    http.httpBasic().disable();
    http.csrf().disable();
  }

  public UserDetailsService mTLSUserDetailsService() {
    return new UserDetailsService() {
      @Override
      public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new User(username, "", AuthorityUtils.createAuthorityList("ROLE_MTLS_USER"));
      }
    };
  }
}
