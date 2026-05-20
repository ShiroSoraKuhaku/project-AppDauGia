package com.group15.daugia.shared.JSON;

/**
 * JSON template cho các push event từ server đến watcher. eventType: AUCTION_STARTED |
 * AUCTION_ENDED | AUCTION_EXTENDED | BID_PLACED
 */
public class JSONAuctionEventTemp extends JSONTemp {
  private String eventType;
  private int auctionId;

  // Snapshot trạng thái auction tại thời điểm event
  private String status;
  private double curPrice;
  private String curLeader;
  private String endTime; // ISO-8601, cập nhật khi AUCTION_EXTENDED
  private long secondsRemaining;
  private int version;

  // Thông tin bid (chỉ có khi eventType = BID_PLACED)
  private String bidderUsername;
  private double bidAmount;

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public int getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(int auctionId) {
    this.auctionId = auctionId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
}
