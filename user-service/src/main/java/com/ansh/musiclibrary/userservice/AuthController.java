package com.ansh.musiclibrary.userservice;

import com.ansh.musiclibrary.common.SessionVerifier;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AuthController {
  private final AccountRepository accounts;
  private final SessionRepository sessions;
  private final JwtEncoder encoder;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

  public AuthController(AccountRepository a, SessionRepository s, SecretKeySpec key) {
    accounts = a;
    sessions = s;
    encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  public record Register(
      @NotBlank @Size(max = 60) String name,
      @NotBlank @Email @Size(max = 160) String email,
      @NotBlank @Size(min = 10, max = 64) String password,
      boolean emailOptIn) {}

  public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 64) String password) {}

  private Map<String, Object> profile(Account a) {
    return Map.of(
        "id", a.id, "name", a.name, "email", a.email, "role", a.role, "emailOptIn", a.emailOptIn);
  }

  private Map<String, Object> token(Account a) {
    Instant now = Instant.now(), end = now.plusSeconds(7200);
    var s = new LoginSession();
    s.id = UUID.randomUUID().toString();
    s.userId = a.id;
    s.expiresAt = end;
    sessions.save(s);
    var claims =
        JwtClaimsSet.builder()
            .issuer("music-library")
            .subject(a.id.toString())
            .id(s.id)
            .issuedAt(now)
            .expiresAt(end)
            .claim("roles", List.of(a.role))
            .build();
    String t =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return Map.of("token", t, "user", profile(a), "expiresAt", end.toString());
  }

  @PostMapping("/api/auth/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> register(@Valid @RequestBody Register r) {
    String email = r.email().trim().toLowerCase(Locale.ROOT);
    if (accounts.findByEmail(email).isPresent())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    var a = new Account();
    a.name = r.name().trim();
    a.email = email;
    a.passwordHash = passwords.encode(r.password());
    a.role = "USER";
    a.emailOptIn = r.emailOptIn();
    try {
      accounts.saveAndFlush(a);
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    return token(a);
  }

  @PostMapping("/api/auth/login")
  public Map<String, Object> login(@Valid @RequestBody Login r) {
    var a =
        accounts
            .findByEmail(r.email().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Incorrect email or password"));
    if (!passwords.matches(r.password(), a.passwordHash))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
    return token(a);
  }

  @GetMapping("/api/auth/me")
  public Map<String, Object> me(@AuthenticationPrincipal Jwt j) {
    return profile(accounts.findById(Long.valueOf(j.getSubject())).orElseThrow());
  }

  @PostMapping("/api/auth/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@AuthenticationPrincipal Jwt j) {
    sessions.deleteById(j.getId());
  }

  @GetMapping("/internal/sessions/{id}")
  public boolean active(@PathVariable String id, @RequestParam Long user) {
    return sessions
        .findById(id)
        .map(s -> s.userId.equals(user) && s.expiresAt.isAfter(Instant.now()))
        .orElse(false);
  }

  @GetMapping("/internal/recipients")
  public List<Map<String, Object>> recipients() {
    return accounts.findAll().stream()
        .map(a -> Map.<String, Object>of("id", a.id, "email", a.email, "emailOptIn", a.emailOptIn))
        .toList();
  }

  @Bean
  public SessionVerifier localSessions() {
    return (id, sub) -> active(id, Long.valueOf(sub));
  }

  @Bean
  public CommandLineRunner admin(
      @Value("${app.admin-email}") String email, @Value("${app.admin-password}") String password) {
    return args -> {
      if (password.length() < 12)
        throw new IllegalStateException("Admin password needs at least 12 characters");
      if (accounts.findByEmail(email.toLowerCase(Locale.ROOT)).isEmpty()) {
        var a = new Account();
        a.name = "Library Admin";
        a.email = email.toLowerCase(Locale.ROOT);
        a.passwordHash = passwords.encode(password);
        a.role = "ADMIN";
        accounts.save(a);
      }
    };
  }
}
