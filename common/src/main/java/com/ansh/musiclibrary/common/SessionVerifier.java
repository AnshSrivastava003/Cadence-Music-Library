package com.ansh.musiclibrary.common;

public interface SessionVerifier {
  boolean active(String id, String subject);
}
