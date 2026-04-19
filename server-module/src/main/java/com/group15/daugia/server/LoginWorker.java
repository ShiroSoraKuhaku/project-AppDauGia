package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.shared.JSONUserTemplate;

public class LoginWorker implements Workable {

  @Override
  public String work(String data) {
    String answer;
    Gson gson = new Gson();
    JSONUserTemplate userTemplate = gson.fromJson(data, JSONUserTemplate.class);
    String[] userData = userTemplate.getLoginData();
    UserDAO checkAcc = new UserDAO();
    String token = checkAcc.checkLogin(userData[0], userData[1]);
    if (token == null) {
      answer = gson.toJson(new JSONUserTemplate());
    } else {
      answer = gson.toJson(new JSONUserTemplate(token));
    }
    return answer;
  }
}
