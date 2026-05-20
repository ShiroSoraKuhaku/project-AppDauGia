package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONBidTemp;

/**
 * PLACE-BID: đặt giá cho một auction đang ACTIVE.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "...", "bidAmount": 150.0 } Response JSON: {
 * "response": "201 Created", ... snapshot ... } { "response": "400 Bad Request" } nếu bid không hợp
 * lệ { "response": "401 Unauthorized" } nếu token sai { "response": "409 Conflict" } nếu version
 * mismatch / race
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

    // 1. Validate token
    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    // 2. Validate auction tồn tại và đang ACTIVE
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

    // 3. Thực hiện đặt giá (transactional với version check)
    boolean success =
        dao.placeBidTransactional(
            req.getAuctionId(), username, req.getBidAmount(), snap.getVersion());

    if (!success) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    // 4. Lấy snapshot mới nhất sau khi bid thành công
    JSONAuctionTemp newSnap = dao.getAuctionSnapshot(req.getAuctionId());

    // 5. Broadcast BID_PLACED event đến tất cả watcher
    JSONAuctionEventTemp event = getJsonAuctionEventTemp(newSnap);
    watcherSvc.broadcast(req.getAuctionId(), event);

    // 6. Kiểm tra gia hạn anti-sniping
    clock.tryExtend(req.getAuctionId());

    // 7. Trả response thành công với trạng thái cuối cùng sau auto bid
    ans.setResponse("201 Created");
    ans.setAuctionId(req.getAuctionId());
    ans.setBidderUsername(newSnap.getCurLeader());
    ans.setBidAmount(newSnap.getCurPrice());
    return gson.toJson(ans);
  }

  private static JSONAuctionEventTemp getJsonAuctionEventTemp(JSONAuctionTemp newSnap) {
    JSONAuctionEventTemp event = new JSONAuctionEventTemp();
    event.setEventType("BID_PLACED");
    event.setAuctionId(newSnap.getAuctionId());
    event.setStatus("ACTIVE");
    event.setCurPrice(newSnap.getCurPrice());
    event.setCurLeader(newSnap.getCurLeader());
    event.setEndTime(newSnap.getEndTime());
    event.setSecondsRemaining(newSnap.getSecondsRemaining());
    event.setVersion(newSnap.getVersion());
    event.setBidderUsername(newSnap.getCurLeader());
    event.setBidAmount(newSnap.getCurPrice());
    return event;
  }
}
