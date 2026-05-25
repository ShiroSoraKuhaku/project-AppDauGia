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

  @Override
  public void start(Stage mainStage) throws Exception {
    mainStage.setTitle("Ứng dụng đấu giá");
    SceneChanger.setMainStage(mainStage);
    SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
    mainStage.setOnCloseRequest(
        windowEvent -> {
          if (SessionManager.getToken() != null && !SessionManager.getToken().isEmpty()) {
            Gson gson = new Gson();
            JSONUserTemp loggedUser = new JSONUserTemp();
            loggedUser.setUsername(User.getUsername());
            loggedUser.setToken(SessionManager.getToken());

            ShortConnectNetwork.shortReq("RM-TOKEN", gson.toJson(loggedUser));
          }
        });
  }
}
