package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class MyAuctionHistoryController implements Initializable {

    @FXML private TableView<JSONMyAuctionHistoryTemp.AuctionHistoryRecord> tblHistory;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, Number> colId;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colTitle;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colStatus;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colCurPrice;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colMyBid;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colLeader;
    @FXML private TableColumn<JSONMyAuctionHistoryTemp.AuctionHistoryRecord, String> colEndTime;
    @FXML private Label lblSubtitle;

    private final Gson gson = new Gson();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initTable();
        loadHistory();
    }

    private void initTable() {
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAuctionId()));
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("admin-status-active", "admin-status-scheduled",
                        "admin-status-ended", "admin-status-cancelled");
                if (empty || item == null) { setText(null); return; }
                switch (item.toUpperCase()) {
                    case "ACTIVE":
                        setText("Đang đấu"); getStyleClass().add("admin-status-active"); break;
                    case "SCHEDULED":
                        setText("Sắp diễn ra"); getStyleClass().add("admin-status-scheduled"); break;
                    case "ENDED":
                        setText("Đã kết thúc"); getStyleClass().add("admin-status-ended"); break;
                    case "CANCELLED":
                        setText("Đã hủy"); getStyleClass().add("admin-status-cancelled"); break;
                    default:
                        setText(item);
                }
            }
        });
        colCurPrice.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getCurPrice()) + " ₫"));
        colMyBid.setCellValueFactory(d ->
                new SimpleStringProperty(VND.format((long) d.getValue().getMyTopBid()) + " ₫"));
        colLeader.setCellValueFactory(d -> {
            String leader = d.getValue().getCurLeader();
            return new SimpleStringProperty(leader != null ? leader : "—");
        });
        colEndTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEndTime()));
    }

    @FXML
    private void handleRefresh() {
        loadHistory();
    }

    @FXML
    private void handleBack() {
        SceneChanger.changeTo("com.group15.daugia.clientResources/dashboard.fxml");
    }

    private void loadHistory() {
        lblSubtitle.setText("Đang tải...");
        tblHistory.getItems().clear();
        try {
            JSONMyAuctionHistoryTemp req = new JSONMyAuctionHistoryTemp();
            req.setToken(SessionManager.getToken());
            String data = ShortConnectNetwork.shortReq("GET-MY-AUCTION-HISTORY", gson.toJson(req));
            JSONMyAuctionHistoryTemp ans = gson.fromJson(data, JSONMyAuctionHistoryTemp.class);
            if (ans == null || !"200 OK".equals(ans.getResponse()) || ans.getAuctions() == null) {
                lblSubtitle.setText("Không tải được lịch sử.");
                return;
            }
            tblHistory.getItems().setAll(ans.getAuctions());
            lblSubtitle.setText(ans.getAuctions().size() + " phiên đấu giá");
        } catch (Exception e) {
            lblSubtitle.setText("Lỗi kết nối.");
            System.err.println("[MyAuctionHistoryController] " + e.getMessage());
        }
    }
}
