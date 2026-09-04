package com.ansh.musiclibrary.common;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public SecretKeySpec key(@Value("${app.jwt-secret}") String s) {
    if (s.length() < 32)
      throw new IllegalStateException("JWT secret must be at least 32 characters");
    return new SecretKeySpec(s.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  @Bean
  public RestClient directClient() {
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofSeconds(3));
    f.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(f).build();
  }

  @Bean
  @Primary
  public RestClient.Builder directBuilder() {
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofSeconds(3));
    f.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(f);
  }

  @Bean
  @LoadBalanced
  public RestClient.Builder serviceClient() {
    var f = new SimpleClientHttpRequestFactory();
    f.setConnectTimeout(Duration.ofSeconds(3));
    f.setReadTimeout(Duration.ofSeconds(8));
    return RestClient.builder().requestFactory(f);
  }

  @Bean
  @ConditionalOnMissingBean(SessionVerifier.class)
  public SessionVerifier remoteVerifier(
      RestClient directClient,
      @Value("${app.user-url}") String url,
      @Value("${app.internal-secret}") String secret) {
    return (id, sub) -> {
      try {
        return Boolean.TRUE.equals(
            directClient
                .get()
                .uri(url + "/internal/sessions/" + id + "?user=" + sub)
                .header("X-Service-Key", secret)
                .retrieve()
                .body(Boolean.class));
      } catch (Exception e) {
        return false;
      }
    };
  }

  @Bean
  public JwtDecoder decoder(SecretKeySpec key, SessionVerifier sessions) {
    var d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    d.setJwtValidator(JwtValidators.createDefaultWithIssuer("music-library"));
    return token -> {
      Jwt j = d.decode(token);
      if (!sessions.active(j.getId(), j.getSubject()))
        throw new BadJwtException("Session expired or signed out");
      return j;
    };
  }

  @Bean
  @Order(1)
  public SecurityFilterChain internal(
      HttpSecurity h, @Value("${app.internal-secret}") String secret) throws Exception {
    h.securityMatcher("/internal/**")
        .csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    h.addFilterBefore(
        new OncePerRequestFilter() {
          protected void doFilterInternal(
              HttpServletRequest q, HttpServletResponse r, FilterChain chain)
              throws java.io.IOException, ServletException {
            String k = q.getHeader("X-Service-Key");
            if (k == null
                || !MessageDigest.isEqual(
                    k.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
              r.sendError(401);
              return;
            }
            chain.doFilter(q, r);
          }
        },
        UsernamePasswordAuthenticationFilter.class);
    h.authorizeHttpRequests(a -> a.anyRequest().permitAll());
    return h.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain api(HttpSecurity h) throws Exception {
    var converter = new JwtAuthenticationConverter();
    var authorities = new JwtGrantedAuthoritiesConverter();
    authorities.setAuthoritiesClaimName("roles");
    authorities.setAuthorityPrefix("ROLE_");
    converter.setJwtGrantedAuthoritiesConverter(authorities);
    h.csrf(c -> c.disable())
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health", "/error", "/api/auth/login", "/api/auth/register")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET, "/", "/index.html", "/app.js", "/styles.css", "/assets/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .headers(
            x ->
                x.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; img-src 'self' https: data:; media-src 'self'"
                                + " https:; style-src 'self'; script-src 'self'; connect-src"
                                + " 'self'; frame-ancestors 'none'; base-uri 'self'")));
    return h.build();
  }
}
