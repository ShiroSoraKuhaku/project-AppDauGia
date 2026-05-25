package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemTemp;

public class UpdateItemWorker implements Workable {
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

    if (sellerUsername == null
        || item.getId() <= 0
        || item.getName() == null
        || item.getName().isBlank()
        || item.getPrice() <= 0) {
      ans.setResponse("400 Bad Request");
    } else {
      String result =
          ItemDAO.getItemDao()
              .updateItem(item.getId(), sellerUsername, item.getName(), item.getPrice(), item.getDesc());
      ans.setResponse("1".equals(result) ? "200 OK" : "404 Not Found");
    }
    return gson.toJson(ans);
  }
}
