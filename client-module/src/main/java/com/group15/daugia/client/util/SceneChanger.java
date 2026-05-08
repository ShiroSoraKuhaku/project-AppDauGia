package com.group15.daugia.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneChanger {
  private static Stage mainStage;
  private static boolean startup;

  public static void setMainStage(Stage stage) {
    mainStage = stage;
  }

  public static <T> T changeTo(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(SceneChanger.class.getClassLoader().getResource(fxmlPath));
      Parent root = loader.load();

      if (startup) {
        mainStage.getScene().setRoot(root);
      } else {
        mainStage.setScene(new Scene(root));
        mainStage.show();
        startup = true;
      }
      mainStage.sizeToScene();
      mainStage.centerOnScreen();

      return loader.getController();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
