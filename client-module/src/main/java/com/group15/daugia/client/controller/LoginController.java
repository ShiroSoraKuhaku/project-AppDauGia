package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements Initializable {
  @FXML private Button ok;
  @FXML private Label signup;
  @FXML private TextField username_in;
  @FXML private PasswordField password_in;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    ok.setOnAction(
        actionEvent -> {
          String username = username_in.getText();
          String password = password_in.getText();

          JSONUserTemp userTemp = new JSONUserTemp();
          userTemp.setUsername(username);
          userTemp.setPassword(password);

          Gson gson = new Gson();
          String answer = ShortConnectNetwork.shortReq("LOGIN", gson.toJson(userTemp));
          JSONUserTemp afterLoginData = gson.fromJson(answer, JSONUserTemp.class);

          if (afterLoginData == null || afterLoginData.getResponse() == null) {
            showAlert(
                Alert.AlertType.ERROR,
                "Lỗi",
                "Phản hồi từ máy chủ không hợp lệ",
                "Dữ liệu nhận về bị trống hoặc sai định dạng.");
            return;
          }

          if ("409 Conflict".equals(afterLoginData.getResponse())) {
            showAlert(
                Alert.AlertType.WARNING,
                "Đăng nhập bị từ chối",
                "Tài khoản đang có phiên đăng nhập",
                "Hãy đăng xuất ở phiên cũ hoặc xóa token cũ trong DB rồi thử lại.");
            return;
          }

          if (afterLoginData.getResponse().charAt(0) == '4') {
            showAlert(
                Alert.AlertType.INFORMATION,
                "Đăng nhập thất bại",
                "Không thể đăng nhập",
                "Tên đăng nhập hoặc mật khẩu không đúng");
            return;
          }

          User.setUsername(username);
          SessionManager.setToken(afterLoginData.getToken());
          SessionManager.setRole(afterLoginData.getRole());
          SceneChanger.changeTo("com.group15.daugia.clientResources/dashboard.fxml");
        });

    signup.setOnMouseClicked(
        mouseEvent -> SceneChanger.changeTo("com.group15.daugia.clientResources/signin.fxml"));
  }

  private void showAlert(Alert.AlertType type, String title, String header, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
