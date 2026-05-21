package com.group15.daugia.shared.JSON;

/**
 * JSON template cho auction request/response. Dùng cho: GET-AUCTION-STATE, WATCH-AUCTION,
 * UNWATCH-AUCTION, PLACE-BID
 */
public class JSONAuctionTemp extends JSONTemp {
  private int auctionId;
  private String token;

  // Thông tin auction (dùng trong response)
  private String title;
  private String status; // SCHEDULED | ACTIVE | ENDED | CANCELLED
  private double startPrice;
  private double curPrice;
  private String curLeader;
  private String startTime; // ISO-8601 string
  private String endTime; // ISO-8601 string
  private long secondsRemaining;
  private int version;

  // Thông tin item liên kết
  private int itemId;

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public double getStartPrice() {
    return startPrice;
  }

  public void setStartPrice(double startPrice) {
    this.startPrice = startPrice;
  }

  public double getCurPrice() {
    return curPrice;
  }

  public void setCurPrice(double curPrice) {
    this.curPrice = curPrice;
  }

  public String getCurLeader() {
    return curLeader;
  }

  public void setCurLeader(String curLeader) {
    this.curLeader = curLeader;
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

  public long getSecondsRemaining() {
    return secondsRemaining;
  }

  public void setSecondsRemaining(long secondsRemaining) {
    this.secondsRemaining = secondsRemaining;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }
}
