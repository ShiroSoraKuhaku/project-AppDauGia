package com.group15.daugia.shared.JSON;

public class JSONCancelBidTemp extends JSONTemp {
  private int auctionId;
  private String token;

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
}
