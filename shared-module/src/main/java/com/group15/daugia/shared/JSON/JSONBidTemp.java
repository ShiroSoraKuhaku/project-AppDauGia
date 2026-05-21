package com.group15.daugia.shared.JSON;

/** JSON template cho PLACE-BID request/response. */
public class JSONBidTemp extends JSONTemp {
  private int auctionId;
  private String token;
  private double bidAmount;

  // Response fields
  private String bidderUsername;
  private String createdAt;

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

  public double getBidAmount() {
    return bidAmount;
  }

  public void setBidAmount(double bidAmount) {
    this.bidAmount = bidAmount;
  }

  public String getBidderUsername() {
    return bidderUsername;
  }

  public void setBidderUsername(String bidderUsername) {
    this.bidderUsername = bidderUsername;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }
}
