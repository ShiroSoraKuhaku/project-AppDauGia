package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.shared.JSON.JSONAdminAuctionTemp;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.JSON.JSONUserInfoTemp;
import com.group15.daugia.shared.JSON.JSONUserListTemp;
import com.group15.daugia.shared.JSON.JSONUserTemp;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class AdminController implements Initializable {
  @FXML private TableView<UserRow> tblUsers;
  @FXML private TableColumn<UserRow, String> colUsername;
  @FXML private TableColumn<UserRow, String> colRole;
  @FXML private TableColumn<UserRow, Boolean> colBanned;
  @FXML private TableColumn<UserRow, String> colBalance;

  @FXML private TableView<AuctionRow> tblAuctions;
  @FXML private TableColumn<AuctionRow, Number> colAuctionId;
  @FXML private TableColumn<AuctionRow, String> colTitle;
  @FXML private TableColumn<AuctionRow, String> colStatus;
  @FXML private TableColumn<AuctionRow, String> colCurPrice;
  @FXML private TableColumn<AuctionRow, String> colStartTime;
  @FXML private TableColumn<AuctionRow, String> colEndTime;

  private final Gson gson = new Gson();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    initTables();
    refreshUsers();
    refreshAuctions();
  }

  private void initTables() {
    colUsername.setCellValueFactory(d -> d.getValue().username);
    colRole.setCellValueFactory(d -> d.getValue().role);
    colBanned.setCellValueFactory(d -> d.getValue().banned);
    colBalance.setCellValueFactory(d -> d.getValue().balance);

    colAuctionId.setCellValueFactory(d -> d.getValue().auctionId);
    colTitle.setCellValueFactory(d -> d.getValue().title);
    colStatus.setCellValueFactory(d -> d.getValue().status);
    colStatus.setCellFactory(
        col ->
            new TableCell<>() {
              @Override
              protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("admin-status-active", "admin-status-scheduled", "admin-status-ended", "admin-status-cancelled");
                if (empty || item == null) {
                  setText(null);
                  return;
                }
                switch (item.toUpperCase()) {
                  case "ACTIVE":
                    setText("Đang đấu");
                    getStyleClass().add("admin-status-active");
                    break;
                  case "SCHEDULED":
                    setText("Sắp diễn ra");
                    getStyleClass().add("admin-status-scheduled");
                    break;
                  case "ENDED":
                    setText("Đã kết thúc");
                    getStyleClass().add("admin-status-ended");
                    break;
                  case "CANCELLED":
                    setText("Đã hủy");
                    getStyleClass().add("admin-status-cancelled");
                    break;
                  default:
                    setText(item);
                }
              }
            });
    colCurPrice.setCellValueFactory(d -> d.getValue().curPrice);
    colStartTime.setCellValueFactory(d -> d.getValue().startTime);
    colEndTime.setCellValueFactory(d -> d.getValue().endTime);
  }

  @FXML
  private void refreshUsers() {
    JSONUserTemp req = new JSONUserTemp();
    req.setToken(SessionManager.getToken());
    JSONUserListTemp ans = gson.fromJson(ShortConnectNetwork.shortReq("GET-USERS", gson.toJson(req)), JSONUserListTemp.class);
    tblUsers.getItems().clear();
    if (ans == null || ans.getUserList() == null || !"200 OK".equals(ans.getResponse())) {
      return;
    }
    NumberFormat vnd = NumberFormat.getInstance(new Locale("vi", "VN"));
    for (JSONUserInfoTemp u : ans.getUserList()) {
      tblUsers.getItems().add(
          new UserRow(
              u.getUsername(), u.getRole(), u.isBanned(), vnd.format((long) u.getBalance()) + " ₫"));
    }
  }

  @FXML
  private void refreshAuctions() {
    String token = SessionManager.getToken() != null ? SessionManager.getToken() : "";
    JSONItemListTemp ans = gson.fromJson(ShortConnectNetwork.shortReq("GET-ITEMS", "{\"token\":\"" + token + "\"}"), JSONItemListTemp.class);
    tblAuctions.getItems().clear();
    if (ans == null || ans.getItemList() == null || !"200 OK".equals(ans.getResponse())) {
      return;
    }
    NumberFormat vnd = NumberFormat.getInstance(new Locale("vi", "VN"));
    for (JSONItemTemp i : ans.getItemList()) {
      tblAuctions.getItems().add(
          new AuctionRow(
              i.getId(),
              i.getName(),
              i.getStatus(),
              vnd.format((long) i.getCurPrice()) + " ₫",
              i.getStartTime(),
              i.getEndTime()));
    }
  }

  @FXML
  private void handleBanUser() {
    UserRow row = tblUsers.getSelectionModel().getSelectedItem();
    if (row == null) return;
    JSONUserTemp req = new JSONUserTemp();
    req.setToken(SessionManager.getToken());
    req.setUsername(row.username.get());
    JSONUserTemp ans = gson.fromJson(ShortConnectNetwork.shortReq("BAN-USER", gson.toJson(req)), JSONUserTemp.class);
    if (ans != null && "200 OK".equals(ans.getResponse())) {
      refreshUsers();
      return;
    }
    showError("BAN-USER thất bại");
  }

  @FXML
  private void handleUnbanUser() {
    UserRow row = tblUsers.getSelectionModel().getSelectedItem();
    if (row == null) return;
    JSONUserTemp req = new JSONUserTemp();
    req.setToken(SessionManager.getToken());
    req.setUsername(row.username.get());
    JSONUserTemp ans = gson.fromJson(ShortConnectNetwork.shortReq("UNBAN-USER", gson.toJson(req)), JSONUserTemp.class);
    if (ans != null && "200 OK".equals(ans.getResponse())) {
      refreshUsers();
      return;
    }
    showError("UNBAN-USER thất bại");
  }

  @FXML
  private void handleOpenAuction() {
    callAuctionAdminCommand("OPEN-AUCTION");
  }

  @FXML
  private void handleCloseAuction() {
    callAuctionAdminCommand("CLOSE-AUCTION");
  }

  @FXML
  private void handleCancelAuction() {
    callAuctionAdminCommand("CANCEL-AUCTION");
  }

  private void callAuctionAdminCommand(String command) {
    AuctionRow row = tblAuctions.getSelectionModel().getSelectedItem();
    if (row == null) return;
    JSONAdminAuctionTemp req = new JSONAdminAuctionTemp();
    req.setToken(SessionManager.getToken());
    req.setAuctionId(row.auctionId.get());
    JSONAdminAuctionTemp ans =
        gson.fromJson(ShortConnectNetwork.shortReq(command, gson.toJson(req)), JSONAdminAuctionTemp.class);
    if (ans != null && ans.getResponse() != null && ans.getResponse().startsWith("200")) {
      refreshAuctions();
      return;
    }
    showError(command + " thất bại");
  }

  private void showError(String msg) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setHeaderText(null);
    alert.setTitle("Lỗi");
    alert.setContentText(msg);
    alert.showAndWait();
  }

  public static class UserRow {
    final SimpleStringProperty username;
    final SimpleStringProperty role;
    final SimpleBooleanProperty banned;
    final SimpleStringProperty balance;

    UserRow(String username, String role, boolean banned, String balance) {
      this.username = new SimpleStringProperty(username);
      this.role = new SimpleStringProperty(role);
      this.banned = new SimpleBooleanProperty(banned);
      this.balance = new SimpleStringProperty(balance);
    }
  }

  public static class AuctionRow {
    final SimpleIntegerProperty auctionId;
    final SimpleStringProperty title;
    final SimpleStringProperty status;
    final SimpleStringProperty curPrice;
    final SimpleStringProperty startTime;
    final SimpleStringProperty endTime;

    AuctionRow(int auctionId, String title, String status, String curPrice, String startTime, String endTime) {
      this.auctionId = new SimpleIntegerProperty(auctionId);
      this.title = new SimpleStringProperty(title);
      this.status = new SimpleStringProperty(status);
      this.curPrice = new SimpleStringProperty(curPrice);
      this.startTime = new SimpleStringProperty(startTime);
      this.endTime = new SimpleStringProperty(endTime);
    }
  }
}
