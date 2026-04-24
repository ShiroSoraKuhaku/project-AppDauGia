package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.shared.JSONUserTemplate;

public class LogoutWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    UserDAO checkAcc = UserDAO.getUserDao();

    JSONUserTemplate userTemp = gson.fromJson(data, JSONUserTemplate.class);
    String[] userData = userTemp.getAfterLoginData();
    String answer = checkAcc.removeLogin(userData[0], userData[1]);
    return answer;
  }
}
