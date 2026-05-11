package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.Workable;

public class GetItemsWorker implements Workable {
  @Override
  public String work(String data) {
    return new Gson().toJson(ItemDAO.getItemDao().getAllItems());
  }
}
