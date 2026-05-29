package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

/**
 * LOGIN: xác thực user và tạo token đăng nhập mới.
 *
 * <p>Request JSON: { "username": "...", "password": "..." }
 * <p>Response JSON: { "response": "201 Created", "username": "...", "token": "...", "role": "..." }
 *   { "response": "401 Unauthorized" } nếu sai tài khoản / mật khẩu
 *   { "response": "409 Conflict" } nếu user đã có phiên đăng nhập
 */
public class LoginWorker implements Workable {

  @Override
  public String work(String data) {
    String answer;
    Gson gson = new Gson();
    JSONUserTemp userTemplate = gson.fromJson(data, JSONUserTemp.class);
    String[] userData = userTemplate.getLoginData();
    UserDAO checkAcc = UserDAO.getUserDao();

    String[] token = checkAcc.checkLogin(userData[0], userData[1]);
    if (token == null) {
      JSONUserTemp ans = new JSONUserTemp();
      ans.setResponse("401 Unauthorized");
      answer = gson.toJson(ans);
    } else if (UserDAO.LOGIN_CONFLICT.equals(token[0])) {
      JSONUserTemp ans = new JSONUserTemp();
      ans.setResponse("409 Conflict");
      answer = gson.toJson(ans);
    } else if (UserDAO.LOGIN_BANNED.equals(token[0])) {
      JSONUserTemp ans = new JSONUserTemp();
      ans.setResponse("403 Forbidden");
      answer = gson.toJson(ans);
    } else {
      JSONUserTemp loggedUser = new JSONUserTemp();
      loggedUser.setResponse("201 Created");
      loggedUser.setToken(token[1]);
      loggedUser.setUsername(token[0]);
      loggedUser.setRole(token[2]);
      answer = gson.toJson(loggedUser);
    }
    return answer;
  }
}
