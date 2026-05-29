package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.DAO.AuctionDao;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * UPDATE-ITEM: cập nhật thông tin item và lịch auction liên quan (seller hoặc admin).
 *
 * <p>Request JSON: { "id": 1, "token": "...", "name": "...", "price": 100.0, "desc": "...",
 * "startTime": "yyyy-MM-dd HH:mm:ss", "endTime": "yyyy-MM-dd HH:mm:ss" }
 * <p>Response JSON: { "response": "200 OK" }
 *   { "response": "400 Bad Request" } nếu dữ liệu không hợp lệ
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "404 Not Found" } nếu item không tồn tại hoặc không thuộc về seller
 */
public class UpdateItemWorker implements Workable {
  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    String role = UserDAO.getUserDao().getRoleByToken(item.getToken());
    boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
    LocalDateTime startTime = parseTime(item.getStartTime());
    LocalDateTime endTime = parseTime(item.getEndTime());

    if (sellerUsername == null) {
      ans.setResponse("401 Unauthorized");
    } else if (item.getId() <= 0
        || item.getName() == null
        || item.getName().isBlank()
        || item.getPrice() <= 0
        || startTime == null
        || endTime == null
        || !endTime.isAfter(startTime)) {
      ans.setResponse("400 Bad Request");
    } else {
      String result =
          ItemDAO.getItemDao()
              .updateItem(
                  item.getId(),
                  sellerUsername,
                  isAdmin,
                  item.getName(),
                  item.getPrice(),
                  item.getDesc(),
                  item.getStartTime(),
                  item.getEndTime());
      if ("1".equals(result)) {
        JSONAuctionTemp auction = AuctionDao.getInstance().findAuctionByItemId(item.getId());
        if (auction == null) {
          auction =
              AuctionDao.getInstance()
                  .createAuction(item.getId(), item.getName(), item.getPrice(), startTime, endTime);
        }
        if (auction != null) {
          AuctionClock.getInstance().scheduleAuction(auction, LocalDateTime.now());
        }
      }
      ans.setResponse("1".equals(result) ? "200 OK" : "404 Not Found");
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
