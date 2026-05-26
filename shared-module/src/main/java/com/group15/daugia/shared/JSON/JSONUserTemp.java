package com.group15.daugia.shared.JSON; // File template json cho data user

public class JSONUserTemp extends JSONTemp {
  private String password;
  private String username;
  private String token;
  private String role;

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getUsername() {
    return this.username;
  }

  public String[] getLoginData() {
    return new String[] {username, password};
  }

  public String[] getAfterLoginData() {
    return new String[] {username, token};
  }

  public String getToken() {
    return token;
  }

  public String getRole() {
    return role;
  }
}
