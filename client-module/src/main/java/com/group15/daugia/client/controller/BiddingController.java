package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONMoneyTemp;
import com.group15.daugia.shared.model.BaseItem;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class BiddingController implements Initializable {

  @FXML private Label lblProductName;
  @FXML private Label lblCurrentPrice;
  @FXML private TextField txtYourBid;
  @FXML private Button btnPlaceBid;
  @FXML private ListView<String> bidHistoryList;

  private static final DecimalFormat PRICE_FORMATTER =
      new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
  private final Gson gson = new Gson();
  private double currentPrice = 1000000;
  private double availableBalance = 0;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    lblProductName.setText("Sản phẩm mẫu vô tuyến");
    lblCurrentPrice.setText(formatPrice(currentPrice));
    refreshAvailableBalance();

    btnPlaceBid.setOnAction(
        event -> {
          String bidInput = txtYourBid.getText().trim();
          if (bidInput.isEmpty()) {
            return;
          }

          Double bidPrice = parseMoney(bidInput);
          if (bidPrice == null) {
            showError("Dữ liệu không hợp lệ", "Giá đặt phải là số.");
            return;
          }

          if (!refreshAvailableBalance()) {
            showError("Không tải được số dư", "Vui lòng thử lại sau.");
            return;
          }

          if (bidPrice > availableBalance) {
            showError(
                "Số dư không đủ",
                "Giá đặt không được lớn hơn số dư khả dụng: " + formatPrice(availableBalance));
            return;
          }

          if (bidPrice < currentPrice) {
            showError("Giá đặt không hợp lệ", "Giá đặt phải bằng hoặc cao hơn giá hiện tại.");
            return;
          }

          bidHistoryList.getItems().add(0, "Bạn đã trả: " + formatPrice(bidPrice));
          currentPrice = bidPrice;
          lblCurrentPrice.setText(formatPrice(bidPrice));
          txtYourBid.clear();

          // TODO: Replace this local-only update with PLACE-BID once auction context is passed in.
        });
  }

  public void setItem(BaseItem item) {
    if (item == null) {
      return;
    }

    lblProductName.setText(item.getName());
    currentPrice = item.getPrice();
    lblCurrentPrice.setText(formatPrice(currentPrice));
    bidHistoryList.getItems().clear();
  }

  @FXML
  void handleBack(ActionEvent event) {
    SceneChanger.changeTo("com.group15.daugia.clientResources/dashboard.fxml");
  }

  private Double parseMoney(String value) {
    try {
      return Double.parseDouble(
          value.replace(",", "").replace("VND", "").replace("VNĐ", "").trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String formatPrice(double value) {
    return PRICE_FORMATTER.format(value) + " VND";
  }

  private boolean refreshAvailableBalance() {
    JSONMoneyTemp request = new JSONMoneyTemp();
    request.setToken(SessionManager.getToken());

    try {
      String raw = ShortConnectNetwork.shortReq("GET-BALANCE", gson.toJson(request));
      JSONMoneyTemp answer = gson.fromJson(raw, JSONMoneyTemp.class);
      if (answer == null || answer.getResponse() == null || answer.getResponse().charAt(0) != '2') {
        return false;
      }
      availableBalance = answer.getAvailableBalance();
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  private void showError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(content);
    alert.showAndWait();
  }
}
