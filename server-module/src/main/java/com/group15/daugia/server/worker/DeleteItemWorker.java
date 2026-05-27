package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemTemp;

/**
 * DELETE-ITEM: xóa item của người bán hiện tại.
 *
 * <p>Request JSON: { "id": 1, "token": "..." }
 * <p>Response JSON: { "response": "200 OK" }
 *   { "response": "400 Bad Request" } nếu thiếu hoặc sai dữ liệu
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "404 Not Found" } nếu item không tồn tại hoặc không thuộc về seller
 */
public class DeleteItemWorker implements Workable {
  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONItemTemp item = gson.fromJson(data, JSONItemTemp.class);
    JSONItemTemp ans = new JSONItemTemp();

    if (item == null) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String sellerUsername = UserDAO.getUserDao().getUsernameByToken(item.getToken());

    if (sellerUsername == null) {
      ans.setResponse("401 Unauthorized");
    } else if (item.getId() <= 0) {
      ans.setResponse("400 Bad Request");
    } else {
      String result = ItemDAO.getItemDao().deleteItem(item.getId(), sellerUsername);
      ans.setResponse("1".equals(result) ? "200 OK" : "404 Not Found");
    }
    return gson.toJson(ans);
  }
}
