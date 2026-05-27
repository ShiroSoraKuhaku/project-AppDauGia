package com.group15.daugia.shared.JSON;

public class JSONMoneyTemp extends JSONTemp {
  private String token;
  private double amount;
  private double balance;
  private double lockedBalance;
  private double availableBalance;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public double getAmount() {
    return amount;
  }

  public void setAmount(double amount) {
    this.amount = amount;
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

  public double getAvailableBalance() {
    return availableBalance;
  }

  public void setAvailableBalance(double availableBalance) {
    this.availableBalance = availableBalance;
  }
}
