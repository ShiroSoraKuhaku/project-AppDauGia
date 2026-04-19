package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSONUserTemplate;

public class LoginWorker implements Workable {

  @Override
  public String work(String data) {
    Gson gson = new Gson();
    JSONUserTemplate userTemplate = gson.fromJson(data, JSONUserTemplate.class);
    String[] userData = userTemplate.getData();
    return null;
  }
}
