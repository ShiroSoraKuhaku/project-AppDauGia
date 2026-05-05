package com.group15.daugia.client.controller;

import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.network.ShortConnectNetwork;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

import java.net.URL;
import java.util.ResourceBundle;
import com.google.gson.Gson;
import com.group15.daugia.shared.JSONUserTemplate;
import javafx.scene.control.TextField;

public class LoginController implements Initializable {
  @FXML private Button ok;

  @FXML private Label signup;

  @FXML private TextField username_in;

  @FXML private PasswordField password_in;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    // Đăng nhập
    ok.setOnAction(
        actionEvent -> {
          String username = username_in.getText();
          String password = password_in.getText();

          JSONUserTemplate userTemp = new JSONUserTemplate();
          userTemp.setUsername(username);
          userTemp.setPassword(password);

          Gson gson = new Gson();
          String userData = gson.toJson(userTemp);

          String answer = ShortConnectNetwork.shortReq("LOGIN", userData);
          System.out.println(answer);
          JSONUserTemplate afterLoginData = gson.fromJson(answer, JSONUserTemplate.class);
          if (afterLoginData.getToken() == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Error");
            alert.setHeaderText("Cannot Login");
            alert.setContentText("Username or Password is wrong");
            alert.showAndWait();
          } else {
            MenuBidderController nextController =
                SceneChanger.changeTo("com.group15.daugia.clientResources/menu_bidder.fxml");
            User.setUsername(username);
            SessionManager.setToken(afterLoginData.getToken());
            System.out.println(SessionManager.getToken());
          }
        });
    signup.setOnMouseClicked(
        mouseEvent -> {
          SceneChanger.changeTo("com.group15.daugia.clientResources/signin.fxml");
        });
  }
}
