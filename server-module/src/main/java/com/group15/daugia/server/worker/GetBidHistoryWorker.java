package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONBidHistoryTemp;

import java.util.List;

public class GetBidHistoryWorker implements Workable {
  private static final int BID_HISTORY_LIMIT = 20;

  private final Gson gson = new Gson();
  private final AuctionDao dao = AuctionDao.getInstance();

  @Override
  public String work(String data) {
    JSONBidHistoryTemp req = gson.fromJson(data, JSONBidHistoryTemp.class);
    JSONBidHistoryTemp ans = new JSONBidHistoryTemp();

    if (req == null || req.getAuctionId() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }
    if (req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    if (dao.findAuctionById(req.getAuctionId()) == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }

    List<JSONBidHistoryTemp.BidRecord> bids = dao.getRecentBids(req.getAuctionId(), BID_HISTORY_LIMIT);
    ans.setAuctionId(req.getAuctionId());
    ans.setBids(bids);
    ans.setResponse("200 OK");
    return gson.toJson(ans);
  }
}
