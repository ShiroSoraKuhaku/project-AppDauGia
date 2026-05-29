package com.group15.daugia.shared.JSON;

public class JSONUserInfoTemp {
  private String username;
  private String role;
  private double balance;
  private double lockedBalance;
  private boolean banned;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public double getLockedBalance() {
    return lockedBalance;
  }

  public void setLockedBalance(double lockedBalance) {
    this.lockedBalance = lockedBalance;
  }

  public boolean isBanned() {
    return banned;
  }

  public void setBanned(boolean banned) {
    this.banned = banned;
  }
}
