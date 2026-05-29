package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * OPEN-AUCTION: admin mở phiên đấu giá ngay (SCHEDULED -> ACTIVE).
 *
 * <p>Request JSON: { "token": "...", "auctionId": 1 }
 * <p>Response JSON: { "response": "200 OK", "auctionId": 1, "status": "ACTIVE" }
 *   { "response": "400 Bad Request" } nếu dữ liệu không hợp lệ
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "403 Forbidden" } nếu không phải admin
 *   { "response": "404 Not Found" } nếu auction không tồn tại
 *   { "response": "409 Conflict" } nếu trạng thái không phải SCHEDULED
 */
public class OpenAuctionWorker implements Workable {
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
    if (!"SCHEDULED".equalsIgnoreCase(snap.getStatus())) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    JSONAuctionTemp started = AuctionClock.getInstance().forceStartNow(req.getAuctionId());
    if (started == null) {
      ans.setResponse("409 Conflict");
      return gson.toJson(ans);
    }

    ans.setResponse("200 OK");
    ans.setAuctionId(req.getAuctionId());
    ans.setStatus("ACTIVE");
    ans.setEndTime(started.getEndTime());
    return gson.toJson(ans);
  }
}
