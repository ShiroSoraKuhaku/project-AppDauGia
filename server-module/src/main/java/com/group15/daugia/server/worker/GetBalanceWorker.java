package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONMoneyTemp;

public class GetBalanceWorker implements Workable {

  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONMoneyTemp req = gson.fromJson(data, JSONMoneyTemp.class);
    JSONMoneyTemp ans = new JSONMoneyTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    UserDAO userDao = UserDAO.getUserDao();
    String username = userDao.getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    double[] info = userDao.getBalanceInfo(username);
    if (info == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }

    ans.setResponse("200 OK");
    ans.setBalance(info[0]);
    ans.setLockedBalance(info[1]);
    ans.setAvailableBalance(info[2]);
    return gson.toJson(ans);
  }
}
