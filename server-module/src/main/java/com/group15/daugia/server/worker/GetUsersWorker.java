package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserListTemp;
import com.group15.daugia.shared.JSON.JSONUserTemp;

/**
 * GET-USERS: admin xem toàn bộ user trên sàn.
 *
 * <p>Request JSON: { "token": "..." }
 * <p>Response JSON: { "response": "200 OK", "userList": [...] }
 *   { "response": "401 Unauthorized" } nếu token sai
 *   { "response": "403 Forbidden" } nếu không phải admin
 */
public class GetUsersWorker implements Workable {
  private final Gson gson = new Gson();

  @Override
  public String work(String data) {
    JSONUserTemp req = gson.fromJson(data, JSONUserTemp.class);
    JSONUserListTemp ans = new JSONUserListTemp();

    if (req == null || req.getToken() == null || req.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
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

    ans.setUserList(UserDAO.getUserDao().getAllUsers());
    ans.setResponse("200 OK");
    return gson.toJson(ans);
  }
}
