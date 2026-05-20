package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * GET-AUCTION-STATE: trả trạng thái hiện tại của một auction. Dùng để client đồng bộ ban đầu trước
 * khi gửi WATCH-AUCTION.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." } Response JSON: { "response": "200 OK",
 * "auctionId": 1, "status": "ACTIVE", "curPrice": 120.0, "curLeader": "alice", "endTime":
 * "2026-05-20 15:00:00", "secondsRemaining": 300, ... }
 */
public class GetAuctionStateWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();

  @Override
  public String work(String data) {
    JSONAuctionTemp req = gson.fromJson(data, JSONAuctionTemp.class);
    JSONAuctionTemp ans = new JSONAuctionTemp();

    // Validate token
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
