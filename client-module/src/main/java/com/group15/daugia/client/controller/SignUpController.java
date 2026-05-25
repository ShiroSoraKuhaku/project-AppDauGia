package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.PasswordField;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {
  @FXML private TextField username_in;

  @FXML private PasswordField password_in;

  @FXML private Button ok;

  @FXML private Label login;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    ok.setOnAction(
        actionEvent -> {
          String username = username_in.getText();
          String password = password_in.getText();
          Gson gson = new Gson();
          JSONUserTemp userTemp = new JSONUserTemp();
          userTemp.setUsername(username);
          userTemp.setPassword(password);

          String data = ShortConnectNetwork.shortReq("SIGNUP", gson.toJson(userTemp));
          System.out.println(data);
          JSONUserTemp answerData = gson.fromJson(data, JSONUserTemp.class);
          if (answerData.getResponse().charAt(0) == '2') {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Thành công");
            alert.setHeaderText("Bạn đã đăng ký tài khoản thành công");
            alert.setContentText("Vui lòng đăng nhập ở màn hình đăng nhập");
            alert.showAndWait();
            SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
          } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Đăng ký thất bại");
            alert.setHeaderText("Tên đăng nhập hoặc mật khẩu đã được sử dụng");
            alert.setContentText("Vui lòng thử lại");
            alert.showAndWait();
          }
        });
    login.setOnMouseClicked(
        mouseEvent -> {
          SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
        });
  }
}
