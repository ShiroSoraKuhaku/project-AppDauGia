package com.group15.daugia.server;

import java.util.HashMap;
import java.util.Map;

public class Work {
  private static Map<String, Workable> works = new HashMap<>();

  static {
    works.put("LOGIN", new LoginWorker());
    works.put("RM-TOKEN", new LogoutWorker());
    works.put("SIGNUP", new SignupWorker());
    works.put("SELL-ITEM", new SellItemWorker());
    works.put("GET-ITEMS", new GetItemsWorker());
  }

  public static Map<String, Workable> getWorks() {
    return works;
  }
}
