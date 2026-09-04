package com.ansh.musiclibrary.userservice;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class LoginSession {
  @Id public String id;
  public Long userId;
  public Instant expiresAt;
}
