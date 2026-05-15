package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

public class LogoutWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    UserDAO checkAcc = UserDAO.getUserDao();

    JSONUserTemp userTemp = gson.fromJson(data, JSONUserTemp.class);
    JSONUserTemp ans = new JSONUserTemp();
    ans.setResponse("204 No Content");
    String[] userData = userTemp.getAfterLoginData();
    checkAcc.removeLogin(userData[0], userData[1]);
    return gson.toJson(ans);
  }
}
