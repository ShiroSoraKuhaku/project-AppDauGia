package com.group15.daugia.shared; // File template json cho data user

public class JSONUserTemplate {
  private String password;
  private String username;

  public JSONUserTemplate(String password, String username) {
    this.password = password;
    this.username = username;
  }

  public String[] getData() {
    return new String[] {username, password};
  }
}
