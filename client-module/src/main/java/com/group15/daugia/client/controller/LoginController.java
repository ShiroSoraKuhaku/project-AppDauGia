package com.group15.daugia.client.controller;

import com.group15.daugia.client.MainApp;
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
import com.group15.daugia.shared.JSON.JSONUserTemp;
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

          JSONUserTemp userTemp = new JSONUserTemp();
          userTemp.setUsername(username);
          userTemp.setPassword(password);

          Gson gson = new Gson();
          String userData = gson.toJson(userTemp);

          // JSONTemp answer = ShortConnectNetwork.shortReq("LOGIN", userData);
          String answer = ShortConnectNetwork.shortReq("LOGIN", userData);
          System.out.println(answer);
          // JSONUserTemp afterLoginData = (JSONUserTemp) answer;
          JSONUserTemp afterLoginData = gson.fromJson(answer, JSONUserTemp.class);
            // Thêm điều kiện kiểm tra null
            if (afterLoginData == null || afterLoginData.getResponse() == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi");
                alert.setHeaderText("Phản hồi từ máy chủ không hợp lệ");
                alert.setContentText("Dữ liệu nhận về bị trống hoặc sai định dạng.");
                alert.showAndWait();
            } else if (afterLoginData.getResponse().charAt(0) == '4') {
                // Logic xử lý lỗi đăng nhập (4xx)
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Đăng nhập thất bại");
                alert.setHeaderText("Không thể đăng nhập");
                alert.setContentText("Tên đăng nhập hoặc mật khẩu không đúng");
                alert.showAndWait();
            } else {
                // Logic đăng nhập thành công
            User.setUsername(username);
            SessionManager.setToken(afterLoginData.getToken());
            SessionManager.setRole(afterLoginData.getRole());
            MainApp.resetLogoutState();
            System.out.println(SessionManager.getToken());
            SceneChanger.changeTo("com.group15.daugia.clientResources/dashboard.fxml");
          }
        });
    signup.setOnMouseClicked(
        mouseEvent -> {
          SceneChanger.changeTo("com.group15.daugia.clientResources/signin.fxml");
        });
  }
}
