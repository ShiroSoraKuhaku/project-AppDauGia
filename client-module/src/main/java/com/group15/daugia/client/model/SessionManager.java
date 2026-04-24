package com.group15.daugia.client.model;

public class SessionManager {
  private static String token;

  public static void setToken(String tokenIn) {
    token = tokenIn;
  }

  public static String getToken() {
    return token;
  }
}
