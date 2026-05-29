package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * CLOSE-AUCTION: admin đóng phiên đấu giá (ACTIVE -> ENDED).
 *
 * <p>Request JSON: { "token": "...", "auctionId": 1 }
 * <p>Response JSON: { "response": "200 OK", "auctionId": 1, "status": "ENDED" }
 *   { "response": "400 Bad Request" } nếu dữ liệu không hợp lệ
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "403 Forbidden" } nếu không phải admin
 *   { "response": "404 Not Found" } nếu auction không tồn tại
 *   { "response": "409 Conflict" } nếu trạng thái không phải ACTIVE
 */
public class CloseAuctionWorker implements Workable {
  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();

  @Override
  public String work(String data) {
    JSONAuctionTemp req = gson.fromJson(data, JSONAuctionTemp.class);
    JSONAuctionTemp ans = new JSONAuctionTemp();

    if (req == null) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }
    if (req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }
    if (req.getAuctionId() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String role = UserDAO.getUserDao().getRoleByToken(req.getToken());
    if (role == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }
    if (!"ADMIN".equalsIgnoreCase(role)) {
      ans.setResponse("403 Forbidden");
      return gson.toJson(ans);
    }

    JSONAuctionTemp snap = dao.getAuctionSnapshot(req.getAuctionId());
    if (snap == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }
    if (!"ACTIVE".equalsIgnoreCase(snap.getStatus())) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    JSONAuctionTemp ended = AuctionClock.getInstance().forceEndNow(req.getAuctionId());
    if (ended == null) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    ans.setResponse("200 OK");
    ans.setAuctionId(req.getAuctionId());
    ans.setStatus("ENDED");
    return gson.toJson(ans);
  }
}
