package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.model.BaseItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.ResourceBundle;

public class MenuBidderController implements Initializable {
  @FXML private TableView<BaseItem> table;
  @FXML private TableColumn<BaseItem, String> table_name;
  @FXML private TableColumn<BaseItem, String> table_description;
  @FXML private TableColumn<BaseItem, Double> table_price;
  @FXML private TableColumn<BaseItem, String> table_start_time;
  @FXML private TableColumn<BaseItem, String> table_end_time;

  private ObservableList<BaseItem> items;
  private final Gson gson = new Gson();
  private static final DecimalFormat PRICE_FORMATTER =
      new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    table_name.setCellValueFactory(new PropertyValueFactory<>("name"));
    table_description.setCellValueFactory(new PropertyValueFactory<>("description"));
    table_price.setCellValueFactory(new PropertyValueFactory<>("price"));
    table_price.setCellFactory(column -> new TableCell<>() {
      @Override
      protected void updateItem(Double price, boolean empty) {
        super.updateItem(price, empty);
        setText(empty || price == null ? null : PRICE_FORMATTER.format(price) + " VND");
      }
    });
    table_start_time.setCellValueFactory(new PropertyValueFactory<>("startTime"));
    table_end_time.setCellValueFactory(new PropertyValueFactory<>("endTime"));

    items = FXCollections.observableArrayList();
    table.setItems(items);
    loadItems();

    table.setRowFactory(
        tv -> {
          TableRow<BaseItem> row = new TableRow<>();
          row.setOnMouseClicked(
              event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                  BaseItem selectedItem = row.getItem();
                  System.out.println("Đang vào phòng đấu giá: " + selectedItem.getName());
                  BiddingController controller =
                      SceneChanger.changeTo("com.group15.daugia.clientResources/bidding.fxml");
                  controller.setItem(selectedItem);
                }
              });
          return row;
        });
  }

  private void loadItems() {
    JSONItemTemp request = new JSONItemTemp();
    request.setToken(SessionManager.getToken());

    String data = ShortConnectNetwork.shortReq("GET-ITEMS", gson.toJson(request));
    JSONItemListTemp answer = gson.fromJson(data, JSONItemListTemp.class);

    items.clear();
    if (answer == null || answer.getItemList() == null) {
      return;
    }

    for (JSONItemTemp item : answer.getItemList()) {
      items.add(
          new BaseItem(
              String.valueOf(item.getId()),
              item.getName(),
              item.getPrice(),
              item.getDesc(),
              item.getStartTime(),
              item.getEndTime()));
    }
  }
}
