package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.server.service.AuctionWatcherService;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * CANCEL-AUCTION: hủy một phiên đấu giá.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." }
 * <p>Response JSON: { "response": "200 OK", "auctionId": 1 }
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "403 Forbidden" } nếu không phải chủ item hoặc admin
 *   { "response": "404 Not Found" } nếu auction không tồn tại
 *   { "response": "409 Conflict" } nếu auction đã kết thúc / bị hủy
 */
public class CancelAuctionWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();
  private final AuctionWatcherService watcherSvc = AuctionWatcherService.getInstance();

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

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String role = UserDAO.getUserDao().getRoleByToken(req.getToken());
    boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

    String result = dao.cancelAuction(req.getAuctionId(), username, isAdmin);

    switch (result) {
      case "OK" -> {
        // Broadcast AUCTION_CANCELLED tới watchers
        JSONAuctionEventTemp event = new JSONAuctionEventTemp();
        event.setEventType("AUCTION_CANCELLED");
        event.setAuctionId(req.getAuctionId());
        event.setStatus("CANCELLED");
        watcherSvc.broadcast(req.getAuctionId(), event);
        ans.setResponse("200 OK");
        ans.setAuctionId(req.getAuctionId());
      }
      case "NOT_FOUND" -> ans.setResponse("404 Not Found");
      case "ALREADY_ENDED" -> ans.setResponse("409 Conflict");
      case "FORBIDDEN" -> ans.setResponse("403 Forbidden");
      default -> ans.setResponse("500 Internal Server Error");
    }
    return gson.toJson(ans);
  }
}
