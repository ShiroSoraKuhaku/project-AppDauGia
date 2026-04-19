package com.group15.daugia.client.model;

public class User {
  private static String username;
  private static String token;

  public static void setUsername(String usernameIn) {
    username = usernameIn;
  }

  public static void setToken(String tokenIn) {
    token = tokenIn;
  }

  public static String getUsername() {
    return username;
  }

  public static String getToken() {
    return token;
  }
}
