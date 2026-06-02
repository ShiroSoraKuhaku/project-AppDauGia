package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONMoneyTemp;

/**
 * WITHDRAW-BALANCE: rút tiền khỏi tài khoản (chỉ được rút số dư khả dụng).
 *
 * <p>Request JSON: { "token": "...", "amount": 100.0 }
 * <p>Response JSON: { "response": "200 OK", "balance": 900.0, "lockedBalance": 0.0,
 * "availableBalance": 900.0 }
 *   { "response": "400 Bad Request" } nếu amount không hợp lệ hoặc không đủ số dư
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "404 Not Found" } nếu user không tồn tại
 */
public class WithdrawBalanceWorker implements Workable {

  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONMoneyTemp req = gson.fromJson(data, JSONMoneyTemp.class);
    JSONMoneyTemp ans = new JSONMoneyTemp();

    if (req == null
        || req.getToken() == null
        || req.getToken().isBlank()
        || !Double.isFinite(req.getAmount())
        || req.getAmount() <= 0) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    UserDAO userDao = UserDAO.getUserDao();
    String username = userDao.getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    double[] info = userDao.withdrawBalance(username, req.getAmount());
    if (info == null) {
      ans.setResponse("404 Not Found");
      return gson.toJson(ans);
    }
    if (info[0] < 0) {
      // Số dư khả dụng không đủ
      ans.setResponse("400 Bad Request");
      ans.setAvailableBalance(info[2]);
      return gson.toJson(ans);
    }

    ans.setResponse("200 OK");
    ans.setBalance(info[0]);
    ans.setLockedBalance(info[1]);
    ans.setAvailableBalance(info[2]);
    return gson.toJson(ans);
  }
}
