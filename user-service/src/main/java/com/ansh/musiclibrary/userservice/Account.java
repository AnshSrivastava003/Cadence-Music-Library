package com.ansh.musiclibrary.userservice;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(unique = true, nullable = false)
  public String email;

  public String name;
  public String passwordHash;
  public String role;
  public boolean emailOptIn;
}
