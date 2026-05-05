package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSONUserTemplate;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {
  @FXML private TextField username_in;

  @FXML private TextField password_in;

  @FXML private ChoiceBox<String> signup_role;

  @FXML private Button ok;

  @FXML private Label login;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    String[] roles = new String[] {"Admin", "Bidder", "Seller"};
    signup_role.getItems().addAll(roles);

    ok.setOnAction(
        actionEvent -> {
          String username = username_in.getText();
          String password = password_in.getText();
          String role = signup_role.getValue();
          Gson gson = new Gson();
          JSONUserTemplate userTemp = new JSONUserTemplate();
          userTemp.setUsername(username);
          userTemp.setPassword(password);

          String data = ShortConnectNetwork.shortReq("SIGNUP", gson.toJson(userTemp));
          if (data.equals("1")) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Success");
            alert.setHeaderText("You successfully registed a new account");
            alert.setContentText("Please login on the login screen");
            alert.showAndWait();
            SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
          } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Failed");
            alert.setHeaderText("You account or password has already been used");
            alert.setContentText("Try again");
            alert.showAndWait();
          }
        });
    login.setOnMouseClicked(
        mouseEvent -> {
          SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
        });
  }
}
