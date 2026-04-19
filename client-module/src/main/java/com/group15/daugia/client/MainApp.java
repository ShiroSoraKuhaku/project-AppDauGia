package com.group15.daugia.client;

import com.group15.daugia.client.util.SceneChanger;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

  @Override
  public void start(Stage mainStage) throws Exception {
    mainStage.setTitle("App");
    SceneChanger.setMainStage(mainStage);
  }
}
