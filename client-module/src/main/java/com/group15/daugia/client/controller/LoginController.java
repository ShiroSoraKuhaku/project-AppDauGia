package com.group15.daugia.client.controller;

import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.network.LoginNetwork;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;

import java.awt.*;
import java.net.URL;
import java.util.ResourceBundle;
import com.google.gson.Gson;
import com.group15.daugia.shared.JSONUserTemplate;

public class LoginController implements Initializable {
  @FXML private Button ok;

  @FXML private Label signup;

  @FXML private TextField username_textbox;

  @FXML private PasswordField password_passwordbox;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    ok.setOnAction(
        actionEvent -> {
          String username = username_textbox.getText();
          String password = password_passwordbox.getText();

          JSONUserTemplate userTemp = new JSONUserTemplate(username, password);
          Gson gson = new Gson();
          String userData = gson.toJson(userTemp);

          String answer = LoginNetwork.loginReq("LOGIN", userData);
          JSONUserTemplate token = gson.fromJson(answer, JSONUserTemplate.class);
          if (token.getToken() != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Cannot Login");
            alert.setContentText("Username or Password is wrong");
            alert.showAndWait();
          } else {
            Menu_BidderController nextController = SceneChanger.changeTo("/menu_bidder.fxml");
            User.setUsername(username);
            User.setToken(token.getToken());
          }
        });
  }
}
