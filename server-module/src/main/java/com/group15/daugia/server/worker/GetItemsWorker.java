package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;

public class GetItemsWorker implements Workable {
  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONItemTemp req = gson.fromJson(data, JSONItemTemp.class);
    JSONItemListTemp ans = new JSONItemListTemp();

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
    ans.setItemList(ItemDAO.getItemDao().getAllItems());
    return gson.toJson(ans);
  }
}
