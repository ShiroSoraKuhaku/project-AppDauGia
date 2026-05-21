package com.group15.daugia.server.service;

import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/** Helper tạo auction event từ snapshot, dùng chung cho PlaceBidWorker và AutoBidWorker. */
public class AuctionEventHelper {

  private AuctionEventHelper() {}

  public static JSONAuctionEventTemp buildBidPlacedEvent(JSONAuctionTemp snap) {
    JSONAuctionEventTemp event = new JSONAuctionEventTemp();
    event.setEventType("BID_PLACED");
    event.setAuctionId(snap.getAuctionId());
    event.setStatus(snap.getStatus());
    event.setCurPrice(snap.getCurPrice());
    event.setCurLeader(snap.getCurLeader());
    event.setEndTime(snap.getEndTime());
    event.setSecondsRemaining(snap.getSecondsRemaining());
    event.setVersion(snap.getVersion());
    event.setBidderUsername(snap.getCurLeader());
    event.setBidAmount(snap.getCurPrice());
    return event;
  }
}
