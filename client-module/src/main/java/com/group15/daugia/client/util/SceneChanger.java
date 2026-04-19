package com.group15.daugia.client.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneChanger {
  private static Stage mainStage;

  public static void setMainStage() {
    if (mainStage == null) {
      mainStage = new Stage();
    }
  }

  public static <T> T changeTo(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(SceneChanger.class.getClassLoader().getResource(fxmlPath));
      Parent root = loader.load();

      mainStage.setScene(new Scene(root));
      mainStage.show();

      return loader.getController();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
