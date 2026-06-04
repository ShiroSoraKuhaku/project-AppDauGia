package com.group15.daugia.shared.JSON;

public class JSONItemTemp extends JSONTemp {
  private int id;
  private int auctionId;
  private String name;
  private double price;
  private double curPrice;
  private String desc;
  private String startTime;
  private String endTime;
  private String status;
  private long secondsRemaining;
  private long secondsToStart;

  private String token;

  private String sellerUsername;

  /** Tên tìm kiếm (LIKE filter), dùng trong GET-ITEMS request */
  private String nameFilter;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
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

  public double getCurPrice() {
    return curPrice;
  }

  public void setCurPrice(double curPrice) {
    this.curPrice = curPrice;
  }

  public String getDesc() {
    return desc;
  }

  public void setDesc(String desc) {
    this.desc = desc;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public long getSecondsRemaining() {
    return secondsRemaining;
  }

  public void setSecondsRemaining(long secondsRemaining) {
    this.secondsRemaining = secondsRemaining;
  }

  public long getSecondsToStart() {
    return secondsToStart;
  }

  public void setSecondsToStart(long secondsToStart) {
    this.secondsToStart = secondsToStart;
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

  public String getNameFilter() {
    return nameFilter;
  }

  public void setNameFilter(String nameFilter) {
    this.nameFilter = nameFilter;
  }
}
