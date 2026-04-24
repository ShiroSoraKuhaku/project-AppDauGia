package com.group15.daugia.shared; // File template json cho data user

public class JSONUserTemplate {
  private String password;
  private String username;
  private String id;
  private String token;

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String[] getLoginData() {
    return new String[] {username, password};
  }

  public String[] getAfterLoginData() {
    return new String[] {id, token};
  }

  public String getToken() {
    return token;
  }

  public String getId() {
    return id;
  }
}
