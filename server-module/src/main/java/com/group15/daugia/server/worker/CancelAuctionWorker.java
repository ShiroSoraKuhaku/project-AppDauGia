package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

public class CancelAuctionWorker implements Workable {

  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONAuctionTemp ans = new JSONAuctionTemp();
    ans.setResponse("501 Not Implemented");
    return gson.toJson(ans);
  }
}
