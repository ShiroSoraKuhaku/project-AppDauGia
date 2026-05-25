package com.group15.daugia.client.model;

public class SessionManager {
  private static String token;
  private static String role;

  public static void setToken(String tokenIn) {
    token = tokenIn;
  }

  public static String getToken() {
    return token;
  }

  public static void setRole(String roleIn) {
    role = roleIn;
  }

  public static String getRole() {
    return role;
  }

  public static boolean isAdmin() {
    return "ADMIN".equalsIgnoreCase(role);
  }

  public static void clear() {
    token = null;
    role = null;
  }
}
