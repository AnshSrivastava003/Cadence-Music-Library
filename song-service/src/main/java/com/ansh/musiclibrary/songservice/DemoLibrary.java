package com.ansh.musiclibrary.songservice;

import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;

@Configuration
public class DemoLibrary {
  @Bean
  CommandLineRunner seed(SongRepository repo) {
    return args -> {
      if (repo.count() > 0) return;
      String[][] items = {
        {"After Hours", "Night Assembly", "City Signals", "Electronic"},
        {"Golden Hour", "Lena Coast", "Slow Sundays", "Chill"},
        {"Blue Orbit", "Parallel Lines", "Somewhere Else", "Ambient"},
        {"Sunday Morning", "The Quiet Club", "Slow Sundays", "Lo-fi"},
        {"Neon Avenue", "Night Assembly", "City Signals", "Electronic"},
        {"Open Water", "Lena Coast", "Somewhere Else", "Ambient"}
      };
      for (int n = 0; n < items.length; n++) {
        var s = new Song();
        s.name = items[n][0];
        s.singer = items[n][1];
        s.albumName = items[n][2];
        s.genre = items[n][3];
        s.musicDirector = "Music Library Studio";
        s.releaseDate = LocalDate.of(2026, 1 + n, 12);
        s.coverImageUrl = "/assets/cover-" + n + ".svg";
        s.audioUrl = "/assets/demo-" + n + ".wav";
        s.durationSeconds = 24;
        repo.save(s);
      }
    };
  }
}
