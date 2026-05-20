package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONAutoBidTemp;

public class AutoBidWorker implements Workable {
  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService watcherSvc = AuctionWatcherService.getInstance();
  private final AuctionClock clock = AuctionClock.getInstance();

  @Override
  public String work(String data) {
    JSONAutoBidTemp req = gson.fromJson(data, JSONAutoBidTemp.class);
    JSONAutoBidTemp ans = new JSONAutoBidTemp();

    if (req == null
        || req.getAuctionId() <= 0
        || req.getToken() == null
        || req.getToken().isBlank()
        || !Double.isFinite(req.getMaxAmount())
        || req.getMaxAmount() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    JSONAuctionTemp before = dao.getAuctionSnapshot(req.getAuctionId());
    int oldVersion = before == null ? -1 : before.getVersion();

    String result = dao.setAutoBid(req.getAuctionId(), username, req.getMaxAmount());

    switch (result) {
      case "OK" -> {
        ans.setResponse("201 Created");
        JSONAuctionTemp after = dao.getAuctionSnapshot(req.getAuctionId());
        if (after != null && after.getVersion() != oldVersion) {
          watcherSvc.broadcast(req.getAuctionId(), buildBidEvent(after));
          clock.tryExtend(req.getAuctionId());
        }
      }
      case "INVALID_INPUT" -> ans.setResponse("400 Bad Request");
      case "AUCTION_NOT_FOUND" -> ans.setResponse("404 Not Found");
      case "AUCTION_NOT_ACTIVE" -> ans.setResponse("400 Bad Request");
      case "PRICE_TOO_LOW" -> ans.setResponse("409 Conflict");
      default -> ans.setResponse("500 Internal Server Error");
    }

    ans.setAuctionId(req.getAuctionId());
    ans.setBidderUsername(username);
    ans.setMaxAmount(req.getMaxAmount());
    return gson.toJson(ans);
  }

  private static JSONAuctionEventTemp buildBidEvent(JSONAuctionTemp snap) {
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
