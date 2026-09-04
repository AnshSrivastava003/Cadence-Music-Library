package com.ansh.musiclibrary.songservice;

import java.util.Map;
import org.springframework.beans.factory.annotation.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Outbox {
  private final EventRepository events;
  private final SongRepository songs;
  private final RestClient client;
  private final String secret;

  public Outbox(
      EventRepository e,
      SongRepository s,
      @Qualifier("serviceClient") RestClient.Builder b,
      @Value("${app.internal-secret}") String key) {
    events = e;
    songs = s;
    client = b.build();
    secret = key;
  }

  @Scheduled(fixedDelay = 15000, initialDelay = 30000)
  public void deliver() {
    for (var e : events.findTop20ByDeliveredFalse()) {
      var song = songs.findById(e.songId);
      if (song.isEmpty() || !song.get().visible) {
        e.delivered = true;
        events.save(e);
        continue;
      }
      try {
        client
            .post()
            .uri("http://notification-service/internal/events")
            .header("X-Service-Key", secret)
            .body(Map.of("id", e.id, "songId", e.songId, "name", e.name))
            .retrieve()
            .toBodilessEntity();
        e.delivered = true;
        events.save(e);
      } catch (Exception ex) {
        System.getLogger("outbox")
            .log(System.Logger.Level.WARNING, "Notification delivery deferred; will retry");
      }
    }
  }
}
