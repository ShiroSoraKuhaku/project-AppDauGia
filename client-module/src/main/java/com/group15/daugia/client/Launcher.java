package com.group15.daugia.client;

import com.google.gson.Gson;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import javafx.application.Application;

public class Launcher {
  public static void main(String[] args) {
    Gson gson = new Gson();
    JSONUserTemp temp = new JSONUserTemp();
    temp.setUsername("name123");
    System.out.println(gson.toJson(temp));
    Application.launch(MainApp.class, args);
  }
}
