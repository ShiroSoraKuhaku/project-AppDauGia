package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.server.service.AuctionEventHelper;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONBidTemp;

/**
 * PLACE-BID: đặt giá cho một auction đang ACTIVE.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "...", "bidAmount": 150.0 }
 * <p>Response JSON: { "response": "201 Created", "auctionId": 1, "bidderUsername": "...",
 * "bidAmount": 150.0 }
 *   { "response": "400 Bad Request" } nếu bid không hợp lệ
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "404 Not Found" } nếu auction không tồn tại
 *   { "response": "409 Conflict" } nếu version mismatch / race
 */
public class PlaceBidWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService watcherSvc = AuctionWatcherService.getInstance();
  private final AuctionClock clock = AuctionClock.getInstance();

  @Override
  public String work(String data) {
    JSONBidTemp req = gson.fromJson(data, JSONBidTemp.class);
    JSONBidTemp ans = new JSONBidTemp();

    if (req == null
        || req.getToken() == null
        || req.getToken().isBlank()
        || req.getAuctionId() <= 0
        || !Double.isFinite(req.getBidAmount())
        || req.getBidAmount() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    JSONAuctionTemp snap = dao.getAuctionSnapshot(req.getAuctionId());
    if (snap == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }
    if (!"ACTIVE".equals(snap.getStatus())) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }
    if (req.getBidAmount() <= snap.getCurPrice()) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    boolean usePessimistic =
        snap.getSecondsRemaining() <= 30 || dao.hasActiveAutoBids(req.getAuctionId());
    AuctionDao.AutoBidResult result = dao.placeBidTransactional(
        req.getAuctionId(), username, req.getBidAmount(), snap.getVersion(), usePessimistic);

    if (result == null) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    // Broadcast event cho manual bid
    JSONAuctionEventTemp manualEvent = new JSONAuctionEventTemp();
    manualEvent.setEventType("BID_PLACED");
    manualEvent.setAuctionId(req.getAuctionId());
    manualEvent.setStatus("ACTIVE");
    manualEvent.setCurPrice(req.getBidAmount());
    manualEvent.setCurLeader(username);
    manualEvent.setBidderUsername(username);
    manualEvent.setBidAmount(req.getBidAmount());
    watcherSvc.broadcast(req.getAuctionId(), manualEvent);

    // Nếu có auto-bid xảy ra, broadcast thêm event cho auto-bid
    if (result.bidderUsername != null) {
      JSONAuctionTemp newSnap = dao.getAuctionSnapshot(req.getAuctionId());
      if (newSnap != null) {
        watcherSvc.broadcast(req.getAuctionId(), AuctionEventHelper.buildBidPlacedEvent(newSnap));
        clock.tryExtend(req.getAuctionId());
        ans.setResponse("201 Created");
        ans.setAuctionId(req.getAuctionId());
        ans.setBidderUsername(username);
        ans.setBidAmount(req.getBidAmount());
        return gson.toJson(ans);
      }
    }

    clock.tryExtend(req.getAuctionId());

    ans.setResponse("201 Created");
    ans.setAuctionId(req.getAuctionId());
    ans.setBidderUsername(username);
    ans.setBidAmount(req.getBidAmount());
    return gson.toJson(ans);
  }
}
