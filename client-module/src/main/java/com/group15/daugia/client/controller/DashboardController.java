package com.group15.daugia.client.controller;

import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.model.User;
import com.group15.daugia.client.util.SceneChanger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {
  @FXML private BorderPane contentRoot;
  @FXML private Button bidderButton;
  @FXML private Button sellerButton;
  @FXML private Button adminButton;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    adminButton.setVisible(SessionManager.isAdmin());
    adminButton.setManaged(SessionManager.isAdmin());
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
  private void logout() {
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
}
