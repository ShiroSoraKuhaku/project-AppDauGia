package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

/**
 * BAN-USER: admin ban user, revoke tokens.
 *
 * <p>Request JSON: { "token": "...", "username": "..." }
 * <p>Response JSON: { "response": "200 OK" }
 *   { "response": "400 Bad Request" } nếu thiếu username
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "403 Forbidden" } nếu không phải admin hoặc cố ban admin
 *   { "response": "404 Not Found" } nếu user không tồn tại
 *   { "response": "409 Conflict" } nếu user đã bị ban
 */
public class BanUserWorker implements Workable {
  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONUserTemp req = gson.fromJson(data, JSONUserTemp.class);
    JSONUserTemp ans = new JSONUserTemp();

    if (req == null) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }
    if (req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }
    if (req.getUsername() == null || req.getUsername().isBlank()) {
      ans.setResponse("400 Bad Request");
      return gson.toJson(ans);
    }

    String role = UserDAO.getUserDao().getRoleByToken(req.getToken());
    if (role == null) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }
    if (!"ADMIN".equalsIgnoreCase(role)) {
      ans.setResponse("403 Forbidden");
      return gson.toJson(ans);
    }

    String result = UserDAO.getUserDao().banUser(req.getUsername());
    switch (result) {
      case "OK" -> ans.setResponse("200 OK");
      case "NOT_FOUND" -> ans.setResponse("404 Not Found");
      case "ALREADY_BANNED" -> ans.setResponse("409 Conflict");
      case "FORBIDDEN" -> ans.setResponse("403 Forbidden");
      default -> ans.setResponse("500 Internal Server Error");
    }
    return gson.toJson(ans);
  }
}
