package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONCancelBidTemp;

public class CancelBidWorker implements Workable {

  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONCancelBidTemp ans = new JSONCancelBidTemp();
    ans.setResponse("501 Not Implemented");
    return gson.toJson(ans);
  }
}
