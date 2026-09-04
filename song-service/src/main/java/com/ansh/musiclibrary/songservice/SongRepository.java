package com.ansh.musiclibrary.songservice;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
  List<Song> findByVisibleTrueOrderByIdDesc();
}
