package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSONUserTemp;

public class LogoutWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    UserDAO checkAcc = UserDAO.getUserDao();

    JSONUserTemp userTemp = gson.fromJson(data, JSONUserTemp.class);
    String[] userData = userTemp.getAfterLoginData();
    return checkAcc.removeLogin(userData[0], userData[1]);
  }
}
