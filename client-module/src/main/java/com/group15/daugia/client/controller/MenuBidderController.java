package com.group15.daugia.client.controller;

import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.model.BaseItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuBidderController implements Initializable {
  @FXML TableView<BaseItem> table;

  @FXML TableColumn<BaseItem, String> table_name;

  @FXML TableColumn<BaseItem, String> table_description;

  @FXML TableColumn<BaseItem, Double> table_price;

  private ObservableList<BaseItem> items;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    // 1. Định nghĩa cách đổ dữ liệu từ thuộc tính của BaseItem vào các cột
    table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
    table_description.setCellValueFactory(new PropertyValueFactory<>("description"));
    table_price.setCellValueFactory(new PropertyValueFactory<>("price"));

    // 2. Mock dữ liệu mẫu (Khi kết nối mạng ổn định, bạn dùng ShortConnectNetwork để fetch list rồi ép sang BaseItem)
    items = FXCollections.observableArrayList();
    // Ví dụ tạo dữ liệu mẫu nếu constructor của bạn hỗ trợ (Nếu lỗi constructor, hãy sửa lại cho đúng thuộc tính)
    // items.add(new BaseItem("Điện thoại iPhone 15", "Hàng lướt 99%", 20000000.0));

    table.setItems(items);

    // 3. Sự kiện: Kích đúp chuột vào một dòng trên bảng để vào phòng đấu giá trực tiếp
    table.setRowFactory(tv -> {
      TableRow<BaseItem> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && (!row.isEmpty())) {
          BaseItem selectedItem = row.getItem();
          System.out.println("Đang vào phòng đấu giá: " + selectedItem.getName());

          // Chuyển sang giao diện đấu giá realtime
          SceneChanger.changeTo("com.group15.daugia.clientResources/bidding.fxml");
        }
      });
      return row;
    });
  }
}