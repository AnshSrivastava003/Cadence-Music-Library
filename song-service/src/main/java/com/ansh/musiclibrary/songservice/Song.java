package com.ansh.musiclibrary.songservice;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Song {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public String name;
  public String singer;
  public String musicDirector;
  public LocalDate releaseDate;
  public String albumName;

  @Column(length = 2000)
  public String coverImageUrl;

  @Column(length = 2000)
  public String audioUrl;

  public boolean visible = true;
  public String genre;
  public int durationSeconds;
}
