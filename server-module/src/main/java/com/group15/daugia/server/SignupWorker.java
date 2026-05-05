package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.shared.JSONUserTemplate;

public class SignupWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONUserTemplate userDataJSON = gson.fromJson(data, JSONUserTemplate.class);
    String[] userData = userDataJSON.getLoginData();
    UserDAO userDao = UserDAO.getUserDao();

    String answer = userDao.signUp(userData[0], userData[1]);
    if (answer.equals("1")) {
      return "1";
    } else {
      return "0";
    }
  }
}
