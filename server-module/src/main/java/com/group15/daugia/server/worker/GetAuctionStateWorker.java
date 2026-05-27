package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * GET-AUCTION-STATE: lấy snapshot trạng thái hiện tại của auction.
 *
 * <p>Dùng để client đồng bộ trước khi gửi WATCH-AUCTION.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." }
 *
 * <p>Response JSON: { "response": "200 OK", "auctionId": 1, "itemId": 1, "title": "...", "status":
 * "ACTIVE", "startPrice": 100.0, "curPrice": 120.0, "curLeader": "alice", "startTime": "...",
 * "endTime": "...", "secondsRemaining": 300, "secondsToStart": 0, "version": 1 } { "response": "401
 * Unauthorized" } nếu token sai { "response": "404 Not Found" } nếu auction không tồn tại
 */
public class GetAuctionStateWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();

  @Override
  public String work(String data) {
    JSONAuctionTemp req = gson.fromJson(data, JSONAuctionTemp.class);
    JSONAuctionTemp ans = new JSONAuctionTemp();

    if (req == null
        || req.getToken() == null
        || req.getToken().isBlank()
        || req.getAuctionId() <= 0) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    // Lấy snapshot
    JSONAuctionTemp snap = dao.getAuctionSnapshot(req.getAuctionId());
    if (snap == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }

    snap.setResponse("200 OK");
    return gson.toJson(snap);
  }
}
