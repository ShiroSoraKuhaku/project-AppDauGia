package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SellItemWorker implements Workable {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONItemTemp item = gson.fromJson(data, JSONItemTemp.class);
    JSONItemTemp ans = new JSONItemTemp();

    String sellerUsername = UserDAO.getUserDao().getUsernameByToken(item.getToken());

    LocalDateTime startTime = parseTime(item.getStartTime());
    LocalDateTime endTime = parseTime(item.getEndTime());

    if (sellerUsername == null
        || item.getName() == null
        || item.getName().isBlank()
        || item.getPrice() <= 0
        || startTime == null
        || endTime == null
        || !endTime.isAfter(startTime)) {
      ans.setResponse("400 Bad Request");
    } else {
      int itemId = ItemDAO.getItemDao().addItem(sellerUsername, item.getName(), item.getPrice(), item.getDesc());
      JSONAuctionTemp auction =
          AuctionDao.getInstance()
              .createAuction(itemId, item.getName(), item.getPrice(), startTime, endTime);
      if (itemId <= 0 || auction == null) {
        ans.setResponse("500 Server Error");
      } else {
        AuctionClock.getInstance().scheduleAuction(auction, LocalDateTime.now());
        ans.setId(itemId);
        ans.setResponse("201 Created");
      }
    }
    return gson.toJson(ans);
  }

  private LocalDateTime parseTime(String value) {
    try {
      return value == null ? null : LocalDateTime.parse(value, FMT);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
