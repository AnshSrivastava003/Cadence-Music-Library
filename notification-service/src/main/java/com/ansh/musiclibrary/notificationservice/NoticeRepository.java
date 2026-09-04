package com.ansh.musiclibrary.notificationservice;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, String> {
  List<Notice> findByUserIdOrderByCreatedAtDesc(Long id);

  List<Notice> findTop20ByEmailStatusAndAttemptsLessThan(String status, int attempts);

  boolean existsByEventIdAndUserId(String eventId, Long userId);
}
