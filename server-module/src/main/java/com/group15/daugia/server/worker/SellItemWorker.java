package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemTemp;

public class SellItemWorker implements Workable {
  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONItemTemp item = gson.fromJson(data, JSONItemTemp.class);
    JSONItemTemp ans = new JSONItemTemp();

    String sellerUsername = UserDAO.getUserDao().getUsernameByToken(item.getToken());

    if (sellerUsername == null
        || item.getName() == null
        || item.getName().isBlank()
        || item.getPrice() <= 0) {
      ans.setResponse("400 Bad Request");
    } else {
      ItemDAO.getItemDao().addItem(sellerUsername, item.getName(), item.getPrice(), item.getDesc());
      ans.setResponse("201 Created");
    }
    return gson.toJson(ans);
  }
}
