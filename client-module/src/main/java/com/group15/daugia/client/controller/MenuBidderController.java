package com.group15.daugia.client.controller;

import com.group15.daugia.shared.model.BaseItem;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuBidderController implements Initializable {
  @FXML TableView<BaseItem> table;

  @FXML TableColumn<BaseItem, String> table_name;

  @FXML TableColumn<BaseItem, String> table_description;

  @FXML TableColumn<BaseItem, Double> table_price;

  ObservableList<BaseItem> items;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {}
}
