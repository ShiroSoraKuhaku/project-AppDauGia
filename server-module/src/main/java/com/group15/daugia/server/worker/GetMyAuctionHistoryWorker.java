package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp;

/**
 * GET-MY-AUCTION-HISTORY: lấy danh sách phiên đấu giá user đã/đang tham gia.
 *
 * <p>Request JSON: { "token": "..." }
 * <p>Response JSON: { "response": "200 OK", "auctions": [...] }
 *   { "response": "401 Unauthorized" } nếu token sai
 */
public class GetMyAuctionHistoryWorker implements Workable {

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();

  @Override
  public String work(String data) {
    JSONMyAuctionHistoryTemp req = gson.fromJson(data, JSONMyAuctionHistoryTemp.class);
    JSONMyAuctionHistoryTemp ans = new JSONMyAuctionHistoryTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    ans.setResponse("200 OK");
    ans.setAuctions(dao.getMyAuctionHistory(username));
    return gson.toJson(ans);
  }
}
