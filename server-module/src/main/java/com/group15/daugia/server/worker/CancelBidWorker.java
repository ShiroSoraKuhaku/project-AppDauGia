package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONCancelBidTemp;

/**
 * CANCEL-BID: hủy bid cao nhất của bidder hiện tại trong một auction.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." }
 * <p>Response JSON: { "response": "200 OK", "auctionId": 1 }
 *   { "response": "400 Bad Request" } nếu input không hợp lệ
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "404 Not Found" } nếu auction / bid không tồn tại
 *   { "response": "409 Conflict" } nếu auction không ACTIVE
 */
public class CancelBidWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService watcherSvc = AuctionWatcherService.getInstance();
  private final AuctionClock clock = AuctionClock.getInstance();

  @Override
  public String work(String data) {
    JSONCancelBidTemp req = gson.fromJson(data, JSONCancelBidTemp.class);
    JSONCancelBidTemp ans = new JSONCancelBidTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank()
        || req.getAuctionId() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String result = dao.cancelBid(req.getAuctionId(), username);

    switch (result) {
      case "OK" -> {
        JSONAuctionTemp snap = dao.getAuctionSnapshot(req.getAuctionId());
        if (snap != null) {
          JSONAuctionEventTemp event = new JSONAuctionEventTemp();
          event.setEventType("BID_CANCELLED");
          event.setAuctionId(req.getAuctionId());
          event.setStatus(snap.getStatus());
          event.setCurPrice(snap.getCurPrice());
          event.setCurLeader(snap.getCurLeader());
          event.setEndTime(snap.getEndTime());
          event.setSecondsRemaining(snap.getSecondsRemaining());
          event.setVersion(snap.getVersion());
          watcherSvc.broadcast(req.getAuctionId(), event);
        }
        ans.setResponse("200 OK");
        ans.setAuctionId(req.getAuctionId());
      }
      case "NOT_FOUND"         -> ans.setResponse("404 Not Found");
      case "AUCTION_NOT_ACTIVE" -> ans.setResponse("409 Conflict");
      case "NO_BID"            -> ans.setResponse("404 Not Found");
      default                  -> ans.setResponse("500 Internal Server Error");
    }
    return gson.toJson(ans);
  }
}
