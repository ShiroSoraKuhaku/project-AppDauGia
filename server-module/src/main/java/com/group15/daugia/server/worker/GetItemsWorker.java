package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemListTemp;

public class GetItemsWorker implements Workable {
  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONItemListTemp ans = new JSONItemListTemp();
    ans.setResponse("200 OK");
    ans.setItemList(ItemDAO.getItemDao().getAllItems());
    return gson.toJson(ans);
  }
}
