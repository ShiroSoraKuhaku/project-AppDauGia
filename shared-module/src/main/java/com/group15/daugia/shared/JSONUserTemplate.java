package com.group15.daugia.shared; // File template json cho data user

public class JSONUserTemplate {
  private String password;
  private String username;
  private String id;
  private String token;

  public JSONUserTemplate(String username, String password) {
    this.password = password;
    this.username = username;
  }

  public JSONUserTemplate(String token) {
    this.token = token;
  }

  public JSONUserTemplate() {}

  public String[] getLoginData() {
    return new String[] {username, password};
  }

  public String getToken() {
    return token;
  }
}
