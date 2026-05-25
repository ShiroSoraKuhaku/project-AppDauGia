package com.group15.daugia.client.controller;

import com.group15.daugia.client.util.SceneChanger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

public class BiddingController implements Initializable {

    @FXML private Label lblProductName;
    @FXML private Label lblCurrentPrice;
    @FXML private TextField txtYourBid;
    @FXML private Button btnPlaceBid;
    @FXML private ListView<String> bidHistoryList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Khởi tạo thông tin tĩnh ban đầu
        lblProductName.setText("Sản phẩm mẫu vô tuyến");
        lblCurrentPrice.setText("1,000,000 VNĐ");

        // Sự kiện nhấn nút đặt giá
        btnPlaceBid.setOnAction(event -> {
            String bidPrice = txtYourBid.getText().trim();
            if (!bidPrice.isEmpty()) {
                // Tạm thời thêm vào list view hiển thị tại chỗ
                bidHistoryList.getItems().add(0, "Bạn đã trả: " + bidPrice + " VNĐ");
                lblCurrentPrice.setText(bidPrice + " VNĐ");
                txtYourBid.clear();

                // TODO: Gửi chuỗi giá này lên server qua cổng Network (Realtime) của nhóm bạn
            }
        });
    }

    @FXML
    void handleBack(ActionEvent event) {
        // Quay trở lại màn hình danh sách đấu giá ban đầu của Bidder
        SceneChanger.changeTo("com.group15.daugia.clientResources/menu_bidder.fxml");
    }
}