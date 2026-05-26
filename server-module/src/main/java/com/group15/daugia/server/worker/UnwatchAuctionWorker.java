package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

/**
 * UNWATCH-AUCTION: hủy đăng ký watcher. Lệnh này được gửi trong session watch (đọc bởi
 * ClientHandler), nhưng cũng có thể dùng như short-lived request nếu cần.
 *
 * <p>Request JSON: { "auctionId": 1, "token": "..." } Response JSON: { "response": "204 No Content"
 * }
 */
public class UnwatchAuctionWorker implements Workable {

  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONAuctionTemp req = gson.fromJson(data, JSONAuctionTemp.class);
    JSONAuctionTemp ans = new JSONAuctionTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String username = UserDAO.getUserDao().getUsernameByToken(req.getToken());
    if (username == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    // Việc unregister thực sự được thực hiện bởi WatchAuctionWorker.stopWatch()
    // khi ClientHandler nhận lệnh UNWATCH-AUCTION trong session.
    // Worker này chỉ trả response xác nhận.
    ans.setResponse("204 No Content");
    return gson.toJson(ans);
  }
}
