package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

/**
 * RM-TOKEN: hủy token đăng nhập hiện tại.
 *
 * <p>Request JSON: { "username": "...", "token": "..." }
 * <p>Response JSON: { "response": "204 No Content" }
 *   { "response": "401 Unauthorized" } nếu thiếu / sai dữ liệu
 */
public class LogoutWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    UserDAO checkAcc = UserDAO.getUserDao();

    JSONUserTemp userTemp = gson.fromJson(data, JSONUserTemp.class);
    JSONUserTemp ans = new JSONUserTemp();
    if (userTemp == null
        || userTemp.getUsername() == null
        || userTemp.getUsername().isBlank()
        || userTemp.getToken() == null
        || userTemp.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String[] userData = userTemp.getAfterLoginData();
    String removed = checkAcc.removeLogin(userData[0], userData[1]);
    ans.setResponse("204 No Content");
    return gson.toJson(ans);
  }
}
