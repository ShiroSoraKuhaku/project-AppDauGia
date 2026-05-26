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
    if (userTemp == null
        || userTemp.getUsername() == null
        || userTemp.getUsername().isBlank()
        || userTemp.getToken() == null
        || userTemp.getToken().isBlank()) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String tokenUsername = checkAcc.getUsernameByToken(userTemp.getToken());
    if (tokenUsername == null || !tokenUsername.equals(userTemp.getUsername())) {
      ans.setResponse("401 Unauthorized");
      return gson.toJson(ans);
    }

    String[] userData = userTemp.getAfterLoginData();
    String removed = checkAcc.removeLogin(userData[0], userData[1]);
    ans.setResponse("1".equals(removed) ? "204 No Content" : "401 Unauthorized");
    return gson.toJson(ans);
  }
}
