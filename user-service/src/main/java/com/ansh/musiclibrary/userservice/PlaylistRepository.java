package com.ansh.musiclibrary.userservice;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
  List<Playlist> findByOwnerIdOrderByIdDesc(Long ownerId);
}
