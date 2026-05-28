package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONMoneyTemp;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;

public class DashboardController implements Initializable {
  private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.##");

  @FXML private BorderPane contentRoot;
  @FXML private Button bidderButton;
  @FXML private Button sellerButton;
  @FXML private Button adminButton;
  @FXML private Label accountNameLabel;
  @FXML private Label accountBalanceLabel;

  private final Gson gson = new Gson();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    adminButton.setVisible(SessionManager.isAdmin());
    adminButton.setManaged(SessionManager.isAdmin());
    accountNameLabel.setText(User.getUsername() == null ? "-" : User.getUsername());
    refreshBalance();
    showBidder();
  }

  @FXML
  private void showBidder() {
    loadContent("com.group15.daugia.clientResources/menu_bidder.fxml");
    bidderButton.setDisable(true);
    sellerButton.setDisable(false);
    adminButton.setDisable(false);
  }

  @FXML
  private void showSeller() {
    loadContent("com.group15.daugia.clientResources/menu_seller.fxml");
    bidderButton.setDisable(false);
    sellerButton.setDisable(true);
    adminButton.setDisable(false);
  }

  @FXML
  private void showAdmin() {
    if (!SessionManager.isAdmin()) {
      return;
    }
    loadContent("com.group15.daugia.clientResources/admin.fxml");
    bidderButton.setDisable(false);
    sellerButton.setDisable(false);
    adminButton.setDisable(true);
  }

  @FXML
  private void refreshBalance() {
    JSONMoneyTemp req = new JSONMoneyTemp();
    req.setToken(SessionManager.getToken());
    String raw = ShortConnectNetwork.shortReq("GET-BALANCE", gson.toJson(req));
    JSONMoneyTemp ans = gson.fromJson(raw, JSONMoneyTemp.class);

    if (ans == null || ans.getResponse() == null || ans.getResponse().charAt(0) != '2') {
      accountBalanceLabel.setText("Không tải được số dư");
      return;
    }

    accountBalanceLabel.setText(MONEY_FMT.format(ans.getAvailableBalance()) + " VND");
  }

  @FXML
  private void topupBalance() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Nạp tiền");
    dialog.setHeaderText("Nhập số tiền cần nạp");
    dialog.setContentText("Số tiền:");

    Optional<String> input = dialog.showAndWait();
    if (input.isEmpty()) {
      return;
    }

    double amount;
    try {
      amount = Double.parseDouble(input.get().trim());
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Số tiền phải là số.");
      return;
    }

    if (!Double.isFinite(amount) || amount <= 0) {
      showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Số tiền phải lớn hơn 0.");
      return;
    }

    JSONMoneyTemp req = new JSONMoneyTemp();
    req.setToken(SessionManager.getToken());
    req.setAmount(amount);
    String raw = ShortConnectNetwork.shortReq("TOPUP-BALANCE", gson.toJson(req));
    JSONMoneyTemp ans = gson.fromJson(raw, JSONMoneyTemp.class);

    if (ans == null || ans.getResponse() == null || ans.getResponse().charAt(0) != '2') {
      showAlert(Alert.AlertType.ERROR, "Nạp tiền thất bại", "Máy chủ từ chối yêu cầu.");
      return;
    }

    accountBalanceLabel.setText(MONEY_FMT.format(ans.getAvailableBalance()) + " VND");
    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Nạp tiền thành công.");
  }

  @FXML
  private void logout() {
    String username = User.getUsername();
    String token = SessionManager.getToken();
    if (username != null && !username.isBlank() && token != null && !token.isBlank()) {
      JSONUserTemp req = new JSONUserTemp();
      req.setUsername(username);
      req.setToken(token);
      ShortConnectNetwork.shortReq("RM-TOKEN", gson.toJson(req));
    }

    SessionManager.clear();
    User.setUsername(null);
    SceneChanger.changeTo("com.group15.daugia.clientResources/login.fxml");
  }

  private void loadContent(String fxmlPath) {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(fxmlPath));
      Parent view = loader.load();
      contentRoot.setCenter(view);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void showAlert(Alert.AlertType type, String title, String content) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
