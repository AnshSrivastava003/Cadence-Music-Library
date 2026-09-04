package com.ansh.musiclibrary.songservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SongController {
  private final SongRepository repo;
  private final EventRepository events;

  public SongController(SongRepository r, EventRepository e) {
    repo = r;
    events = e;
  }

  public record Input(
      @NotBlank @Size(max = 120) String name,
      @NotBlank @Size(max = 100) String singer,
      @NotBlank @Size(max = 100) String musicDirector,
      @NotNull LocalDate releaseDate,
      @NotBlank @Size(max = 120) String albumName,
      @Size(max = 2000) String coverImageUrl,
      @Size(max = 2000) String audioUrl,
      boolean visible,
      @NotBlank @Size(max = 50) String genre,
      @Min(0) @Max(86400) int durationSeconds) {}

  private boolean matches(Song s, String q) {
    return List.of(s.name, s.singer, s.albumName, s.musicDirector).stream()
        .anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(q));
  }

  @GetMapping("/api/songs")
  public List<Song> all(@RequestParam(defaultValue = "") String keyword) {
    String q = keyword.trim().toLowerCase(Locale.ROOT);
    return repo.findByVisibleTrueOrderByIdDesc().stream().filter(s -> matches(s, q)).toList();
  }

  @GetMapping("/api/songs/search")
  public List<Song> search(@RequestParam String keyword) {
    return all(keyword);
  }

  @GetMapping("/api/songs/{id}")
  public Song get(@PathVariable Long id) {
    return repo.findById(id)
        .filter(s -> s.visible)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song unavailable"));
  }

  @GetMapping("/api/admin/songs")
  @PreAuthorize("hasRole('ADMIN')")
  public List<Song> admin() {
    return repo.findAll();
  }

  private void url(String v) {
    if (v != null && !v.isBlank() && !v.startsWith("/assets/") && !v.matches("https://[^\\s]+"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Media URLs must start with https:// or /assets/");
  }

  private void copy(Song s, Input i) {
    url(i.coverImageUrl());
    url(i.audioUrl());
    s.name = i.name().trim();
    s.singer = i.singer().trim();
    s.musicDirector = i.musicDirector().trim();
    s.releaseDate = i.releaseDate();
    s.albumName = i.albumName().trim();
    s.coverImageUrl = i.coverImageUrl();
    s.audioUrl = i.audioUrl();
    s.visible = i.visible();
    s.genre = i.genre().trim();
    s.durationSeconds = i.durationSeconds();
  }

  private void event(Song s) {
    var e = new SongEvent();
    e.id = UUID.randomUUID().toString();
    e.songId = s.id;
    e.name = s.name;
    events.save(e);
  }

  @PostMapping("/api/admin/songs")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public Song add(@Valid @RequestBody Input i) {
    var s = new Song();
    copy(s, i);
    repo.save(s);
    if (s.visible) event(s);
    return s;
  }

  @PutMapping("/api/admin/songs/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  public Song update(@PathVariable Long id, @Valid @RequestBody Input i) {
    var s =
        repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found"));
    boolean was = s.visible;
    copy(s, i);
    repo.save(s);
    if (!was && s.visible) event(s);
    return s;
  }

  @DeleteMapping("/api/admin/songs/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('ADMIN')")
  public void delete(@PathVariable Long id) {
    var s =
        repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found"));
    repo.delete(s);
  }
}
