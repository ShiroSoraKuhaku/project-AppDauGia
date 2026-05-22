package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONUserTemp;

public class SignupWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONUserTemp userDataJSON = gson.fromJson(data, JSONUserTemp.class);
    JSONUserTemp ans = new JSONUserTemp();
    String[] userData = userDataJSON.getLoginData();
    String role = userDataJSON.getRole();
    if (role == null || role.isBlank()) {
      role = "Bidder";
    }
    UserDAO userDao = UserDAO.getUserDao();

    String answer = userDao.signUp(userData[0], userData[1], role);
    if (answer.equals("1")) {
      ans.setResponse("201 Created");
    } else {
      ans.setResponse("409 Conflict");
    }
    return gson.toJson(ans);
  }
}
