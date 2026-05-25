package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

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
