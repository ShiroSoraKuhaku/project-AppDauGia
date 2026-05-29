package com.group15.daugia.client;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
  private static boolean logoutSent;

  @Override
  public void start(Stage mainStage) throws Exception {
    mainStage.setTitle("Ứng dụng đấu giá");
    SceneChanger.setMainStage(mainStage);
    SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
    mainStage.setOnCloseRequest(event -> logoutCurrentSession());
  }

  @Override
  public void stop() {
    logoutCurrentSession();
  }

  public static void resetLogoutState() {
    logoutSent = false;
  }

  public static void logoutCurrentSession() {
    if (logoutSent) {
      return;
    }

    String token = SessionManager.getToken();
    String username = User.getUsername();
    if (token == null || token.isBlank() || username == null || username.isBlank()) {
      return;
    }

    logoutSent = true;
    Gson gson = new Gson();
    JSONUserTemp loggedUser = new JSONUserTemp();
    loggedUser.setUsername(username);
    loggedUser.setToken(token);
    ShortConnectNetwork.shortReq("RM-TOKEN", gson.toJson(loggedUser));
  }
}
