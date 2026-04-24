package com.group15.daugia.client.model;

public class User {
  private static String username;
  private static String id;

  public static void setUsername(String username) {
    User.username = username;
  }

  public static void setId(String id) {
    User.id = id;
  }

  public static String getUsername() {
    return username;
  }

  public static String getId() {
    return id;
  }
}
