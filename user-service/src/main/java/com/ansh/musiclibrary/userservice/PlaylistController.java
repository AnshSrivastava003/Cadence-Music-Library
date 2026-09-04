package com.ansh.musiclibrary.userservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {
  private final PlaylistRepository repo;
  private final RestClient client;

  public PlaylistController(
      PlaylistRepository r, @Qualifier("serviceClient") RestClient.Builder b) {
    repo = r;
    client = b.build();
  }

  public record Name(@NotBlank @Size(max = 80) String name) {}

  private Playlist owned(Long id, Jwt j) {
    return repo.findById(id)
        .filter(p -> p.ownerId.equals(Long.valueOf(j.getSubject())))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Playlist not found"));
  }

  @GetMapping
  public List<Playlist> list(@AuthenticationPrincipal Jwt j) {
    return repo.findByOwnerIdOrderByIdDesc(Long.valueOf(j.getSubject()));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Playlist create(@AuthenticationPrincipal Jwt j, @Valid @RequestBody Name n) {
    var p = new Playlist();
    p.ownerId = Long.valueOf(j.getSubject());
    p.name = n.name().trim();
    return repo.save(p);
  }

  @PutMapping("/{id}")
  public Playlist rename(
      @PathVariable Long id, @AuthenticationPrincipal Jwt j, @Valid @RequestBody Name n) {
    var p = owned(id, j);
    p.name = n.name().trim();
    return repo.save(p);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt j) {
    repo.delete(owned(id, j));
  }

  @PostMapping("/{id}/songs/{songId}")
  public Playlist add(
      @PathVariable Long id, @PathVariable Long songId, @AuthenticationPrincipal Jwt j) {
    var p = owned(id, j);
    try {
      client
          .get()
          .uri("http://song-service/api/songs/" + songId)
          .header("Authorization", "Bearer " + j.getTokenValue())
          .retrieve()
          .toBodilessEntity();
    } catch (HttpClientErrorException.NotFound e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song unavailable");
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Song service unavailable");
    }
    if (!p.songIds.contains(songId)) p.songIds.add(songId);
    return repo.save(p);
  }

  @DeleteMapping("/{id}/songs/{songId}")
  public Playlist remove(
      @PathVariable Long id, @PathVariable Long songId, @AuthenticationPrincipal Jwt j) {
    var p = owned(id, j);
    p.songIds.remove(songId);
    return repo.save(p);
  }
}
