package com.ansh.musiclibrary.songservice;

import jakarta.persistence.*;

@Entity
public class SongEvent {
  @Id public String id;
  public Long songId;
  public String name;
  public boolean delivered;
}
