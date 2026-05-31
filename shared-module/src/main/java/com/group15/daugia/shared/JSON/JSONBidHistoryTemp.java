package com.group15.daugia.shared.JSON;

import java.util.ArrayList;
import java.util.List;

public class JSONBidHistoryTemp extends JSONTemp {
  private int auctionId;
  private String token;
  private List<BidRecord> bids = new ArrayList<>();

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

  public List<BidRecord> getBids() {
    return bids;
  }

  public void setBids(List<BidRecord> bids) {
    this.bids = bids;
  }

  public static class BidRecord {
    private String bidderUsername;
    private double bidAmount;
    private String createdAt;

    public String getBidderUsername() {
      return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
      this.bidderUsername = bidderUsername;
    }

    public double getBidAmount() {
      return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
      this.bidAmount = bidAmount;
    }

    public String getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(String createdAt) {
      this.createdAt = createdAt;
    }

    public static BidRecord fromEvent(String bidderUsername, double bidAmount, String createdAt) {
      BidRecord record = new BidRecord();
      record.setBidderUsername(bidderUsername);
      record.setBidAmount(bidAmount);
      record.setCreatedAt(createdAt);
      return record;
    }
  }
}
