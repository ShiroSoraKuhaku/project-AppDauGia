package com.group15.daugia.shared.model;

public class BaseItem {
  String id;
  String name;
  double price;
  String description;
  String startTime;
  String endTime;

  // Thêm cho màn hình người mua
  int auctionId;
  String seller;
  double curPrice;
  long secondsRemaining;
  String status; // SCHEDULED | ACTIVE | ENDED | CANCELLED

  public BaseItem() {}

  public BaseItem(String id, String name, double price, String description) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.description = description;
  }

  public BaseItem(
      String id, String name, double price, String description, String startTime, String endTime) {
    this(id, name, price, description);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public double getPrice() {
    return price;
  }

  public String getName() {
    return name;
  }

  public String getStartTime() {
    return startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  // ---- Getters/Setters cho các field bổ sung ----

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public String getSeller() {
    return seller;
  }

  public void setSeller(String seller) {
    this.seller = seller;
  }

  public double getCurPrice() {
    return curPrice;
  }

  public void setCurPrice(double curPrice) {
    this.curPrice = curPrice;
  }

  public long getSecondsRemaining() {
    return secondsRemaining;
  }

  public void setSecondsRemaining(long secondsRemaining) {
    this.secondsRemaining = secondsRemaining;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
