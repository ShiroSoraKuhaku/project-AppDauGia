package com.group15.daugia.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
  @FXML private Button ok;

  @FXML private Label signup;

  @FXML private TextField username_textbox;

  @FXML private PasswordField password_passwordbox;

  private Parent root;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
    try {
      root = loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
