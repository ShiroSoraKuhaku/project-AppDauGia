package com.group15.daugia.server;

import com.group15.daugia.server.worker.*;

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
    works.put("GET-MY-ITEMS", new GetMyItemsWorker());
    // Auction workers
    works.put("WATCH-AUCTION", new WatchAuctionWorker());
    works.put("UNWATCH-AUCTION", new UnwatchAuctionWorker());
    works.put("PLACE-BID", new PlaceBidWorker());
    works.put("GET-AUCTION-STATE", new GetAuctionStateWorker());

    works.put("SET-AUTO-BID", new AutoBidWorker());
  }

  public static Map<String, Workable> getWorks() {
    return works;
  }
}
