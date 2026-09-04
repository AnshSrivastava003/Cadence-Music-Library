package com.ansh.musiclibrary.userservice;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class Playlist {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  public Long ownerId;
  public String name;

  @ElementCollection(fetch = FetchType.EAGER)
  @OrderColumn(name = "position_index")
  public List<Long> songIds = new ArrayList<>();
}
