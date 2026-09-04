package com.ansh.musiclibrary.songservice;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<SongEvent, String> {
  List<SongEvent> findTop20ByDeliveredFalse();
}
