package com.group15.daugia.client.model;

public class User {
  private static String username;

  public static void setUsername(String usernameIn) {
    username = usernameIn;
  }

  public static String getUsername() {
    return username;
  }
}
