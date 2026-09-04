package com.ansh.musiclibrary.notificationservice;

import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NoticeController {
  private final NoticeRepository repo;
  private final RestClient client;
  private final String key;
  private final boolean enabled;

  public NoticeController(
      NoticeRepository r,
      @Qualifier("serviceClient") RestClient.Builder b,
      @Value("${app.internal-secret}") String k,
      @Value("${app.email-enabled}") boolean e) {
    repo = r;
    client = b.build();
    key = k;
    enabled = e;
  }

  public record Event(String id, Long songId, String name) {}

  public record Recipient(Long id, String email, boolean emailOptIn) {}

  @PostMapping("/internal/events")
  public void receive(@RequestBody Event event) {
    Recipient[] people =
        client
            .get()
            .uri("http://user-service/internal/recipients")
            .header("X-Service-Key", key)
            .retrieve()
            .body(Recipient[].class);
    if (people == null)
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Recipients unavailable");
    for (var p : people) {
      if (repo.existsByEventIdAndUserId(event.id(), p.id())) continue;
      var n = new Notice();
      n.id = event.id() + "-" + p.id();
      n.eventId = event.id();
      n.userId = p.id();
      n.songId = event.songId();
      n.message = "New in the library: " + event.name();
      n.createdAt = Instant.now();
      n.email = p.email();
      n.emailStatus = enabled && p.emailOptIn() ? "PENDING" : "DISABLED";
      repo.save(n);
    }
  }

  @GetMapping("/api/notifications")
  public List<Map<String, Object>> list(@AuthenticationPrincipal Jwt j) {
    return repo.findByUserIdOrderByCreatedAtDesc(Long.valueOf(j.getSubject())).stream()
        .limit(100)
        .map(
            n ->
                Map.<String, Object>of(
                    "id",
                    n.id,
                    "songId",
                    n.songId,
                    "message",
                    n.message,
                    "createdAt",
                    n.createdAt,
                    "read",
                    n.read))
        .toList();
  }

  @PutMapping("/api/notifications/{id}/read")
  public void read(@PathVariable String id, @AuthenticationPrincipal Jwt j) {
    var n =
        repo.findById(id)
            .filter(x -> x.userId.equals(Long.valueOf(j.getSubject())))
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    n.read = true;
    repo.save(n);
  }
}
