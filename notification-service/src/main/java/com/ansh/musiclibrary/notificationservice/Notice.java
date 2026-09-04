package com.ansh.musiclibrary.notificationservice;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"eventId", "userId"}))
public class Notice {
  @Id public String id;
  public String eventId;
  public Long userId;
  public Long songId;
  public String message;
  public Instant createdAt;
  public boolean read;
  public String email;
  public String emailStatus;
  public int attempts;
}
