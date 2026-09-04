package com.ansh.musiclibrary.userservice;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<LoginSession, String> {}
