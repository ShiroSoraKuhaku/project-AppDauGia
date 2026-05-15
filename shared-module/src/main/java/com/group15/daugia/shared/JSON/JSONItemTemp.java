package com.group15.daugia.shared.JSON;

public class JSONItemTemp extends JSONTemp {
  private int id;
  private String name;
  private double price;
  private String desc;

  private String token;

  private String sellerUsername;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getSellerUsername() {
    return sellerUsername;
  }

  public void setSellerUsername(String sellerUsername) {
    this.sellerUsername = sellerUsername;
  }
}
