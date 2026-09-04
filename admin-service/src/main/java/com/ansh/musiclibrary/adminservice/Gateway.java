package com.ansh.musiclibrary.adminservice;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;

@RestController
public class Gateway {
  private final RestClient client;

  public Gateway(@Qualifier("serviceClient") RestClient.Builder b) {
    client = b.build();
  }

  @RequestMapping({
    "/api/auth/**",
    "/api/playlists/**",
    "/api/songs/**",
    "/api/admin/**",
    "/api/notifications/**"
  })
  public ResponseEntity<?> proxy(
      HttpServletRequest req, @RequestBody(required = false) String body) {
    String path = req.getRequestURI();
    String service =
        path.startsWith("/api/auth") || path.startsWith("/api/playlists")
            ? "user-service"
            : path.startsWith("/api/notifications") ? "notification-service" : "song-service";
    try {
      var r =
          client
              .method(HttpMethod.valueOf(req.getMethod()))
              .uri(
                  "http://"
                      + service
                      + path
                      + (req.getQueryString() == null ? "" : "?" + req.getQueryString()));
      String token = req.getHeader("Authorization");
      if (token != null) r.header("Authorization", token);
      if (body != null) r.contentType(MediaType.APPLICATION_JSON).body(body);
      var response = r.retrieve().toEntity(String.class);
      var out = ResponseEntity.status(response.getStatusCode());
      if (response.getHeaders().getContentType() != null)
        out.contentType(response.getHeaders().getContentType());
      return out.body(response.getBody());
    } catch (RestClientResponseException e) {
      return ResponseEntity.status(e.getStatusCode())
          .contentType(MediaType.APPLICATION_JSON)
          .body(e.getResponseBodyAsString());
    } catch (Exception e) {
      return ResponseEntity.status(503)
          .body(Map.of("message", "Service is starting or unavailable. Please try again shortly."));
    }
  }
}
