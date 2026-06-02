package com.group15.daugia.shared.JSON;

import java.util.List;

/**
 * DTO cho lịch sử phiên đấu giá mà user đã/đang tham gia.
 */
public class JSONMyAuctionHistoryTemp extends JSONTemp {
  private String token;
  private List<AuctionHistoryRecord> auctions;

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public List<AuctionHistoryRecord> getAuctions() { return auctions; }
  public void setAuctions(List<AuctionHistoryRecord> auctions) { this.auctions = auctions; }

  public static class AuctionHistoryRecord {
    private int auctionId;
    private String title;
    private String status;
    private double curPrice;
    private String curLeader;
    private double myTopBid;
    private String startTime;
    private String endTime;

    public int getAuctionId() { return auctionId; }
    public void setAuctionId(int auctionId) { this.auctionId = auctionId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getCurPrice() { return curPrice; }
    public void setCurPrice(double curPrice) { this.curPrice = curPrice; }
    public String getCurLeader() { return curLeader; }
    public void setCurLeader(String curLeader) { this.curLeader = curLeader; }
    public double getMyTopBid() { return myTopBid; }
    public void setMyTopBid(double myTopBid) { this.myTopBid = myTopBid; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
  }
}
