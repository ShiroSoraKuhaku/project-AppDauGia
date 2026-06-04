package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.JSON.JSONMoneyTemp;
import com.group15.daugia.shared.model.BaseItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

public class MenuBidderController implements Initializable {

    @FXML private FlowPane cardPane;
    @FXML private Label lblSubtitle;
    @FXML private Label lblBalance;
    @FXML private TextField txtSearch;

    private final Gson gson = new Gson();
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private Timeline autoRefreshTimeline;
    private Timeline countdownTimeline;
    private final Map<Integer, Long> localCountdowns = new HashMap<>();
    private final Map<Integer, Label> countdownLabels = new HashMap<>();
    private final Map<Integer, String> auctionStatuses = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        refreshBalanceLabel();
        loadItems(true);
        startAutoRefresh();
        startLocalCountdown();
        cardPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                stopAutoRefresh();
                stopLocalCountdown();
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadItems(true);
    }

    @FXML
    private void handleSearch() {
        loadItems(true);
    }

    // -----------------------------------------------------------------------
    // Load dữ liệu từ server
    // -----------------------------------------------------------------------
    private void loadItems(boolean showLoading) {
        if (showLoading) {
            lblSubtitle.setText("Đang tải...");
        }

        refreshBalanceLabel();
        String nameFilter = txtSearch != null ? txtSearch.getText().trim() : "";
        List<BaseItem> items = fetchItems(nameFilter);
        if (items.isEmpty()) {
            cardPane.getChildren().clear();
            lblSubtitle.setText("Không có sản phẩm nào.");
            return;
        }

        cardPane.getChildren().clear();
        lblSubtitle.setText(items.size() + " sản phẩm");
        for (BaseItem item : items) {
            localCountdowns.put(item.getAuctionId(), Math.max(0, item.getSecondsRemaining()));
            auctionStatuses.put(item.getAuctionId(), item.getStatus());
            cardPane.getChildren().add(buildCard(item));
        }
    }

    @FXML
    private void handleTopup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nạp tiền vào tài khoản");
        dialog.setContentText("Số tiền:");

        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.get().trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Số tiền không hợp lệ.");
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Số tiền phải lớn hơn 0.");
            return;
        }

        JSONMoneyTemp request = new JSONMoneyTemp();
        request.setToken(SessionManager.getToken());
        request.setAmount(amount);

        String data = ShortConnectNetwork.shortReq("TOPUP-BALANCE", gson.toJson(request));
        JSONMoneyTemp answer = gson.fromJson(data, JSONMoneyTemp.class);
        if (answer == null || answer.getResponse() == null || !"200 OK".equals(answer.getResponse())) {
            showAlert(Alert.AlertType.ERROR, "Nạp tiền thất bại.");
            return;
        }

        refreshBalanceLabel();
        loadItems(false);
        showAlert(
                Alert.AlertType.INFORMATION,
                "Nạp tiền thành công. Số dư hiện tại: " + VND.format((long) answer.getBalance()) + " ₫");
    }

    private List<BaseItem> fetchItems(String nameFilter) {
        List<BaseItem> result = new ArrayList<>();
        try {
            String token = SessionManager.getToken() != null ? SessionManager.getToken() : "";
            JSONItemTemp req = new JSONItemTemp();
            req.setToken(token);
            if (nameFilter != null && !nameFilter.isBlank()) {
                req.setNameFilter(nameFilter);
            }
            String data = ShortConnectNetwork.shortReq("GET-ITEMS", gson.toJson(req));
            JSONItemListTemp answer = gson.fromJson(data, JSONItemListTemp.class);

            if (answer == null || answer.getItemList() == null) {
                return result;
            }

            for (JSONItemTemp item : answer.getItemList()) {
                BaseItem base = new BaseItem(
                        String.valueOf(item.getId()),
                        item.getName(),
                        item.getPrice(),
                        item.getDesc(),
                        item.getStartTime(),
                        item.getEndTime());
                base.setAuctionId(item.getAuctionId() > 0 ? item.getAuctionId() : item.getId());
                base.setSeller(item.getSellerUsername());
                base.setCurPrice(item.getCurPrice() > 0 ? item.getCurPrice() : item.getPrice());
                long displaySeconds = "SCHEDULED".equalsIgnoreCase(item.getStatus())
                        ? item.getSecondsToStart()
                        : item.getSecondsRemaining();
                base.setSecondsRemaining(displaySeconds);
                base.setStatus(item.getStatus());
                result.add(base);
            }
        } catch (Exception e) {
            System.err.println("[MenuBidderController] Lỗi khi tải sản phẩm: " + e.getMessage());
        }
        return result;
    }

    @FXML
    private void handleWithdraw() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rút tiền");
        dialog.setHeaderText("Rút tiền từ tài khoản");
        dialog.setContentText("Số tiền:");

        Optional<String> input = dialog.showAndWait();
        if (input.isEmpty()) {
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.get().trim());
        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Số tiền không hợp lệ.");
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Số tiền phải lớn hơn 0.");
            return;
        }

        JSONMoneyTemp request = new JSONMoneyTemp();
        request.setToken(SessionManager.getToken());
        request.setAmount(amount);

        String data = ShortConnectNetwork.shortReq("WITHDRAW-BALANCE", gson.toJson(request));
        JSONMoneyTemp answer = gson.fromJson(data, JSONMoneyTemp.class);
        if (answer == null || answer.getResponse() == null) {
            showAlert(Alert.AlertType.ERROR, "Rút tiền thất bại.");
            return;
        }
        if ("400 Bad Request".equals(answer.getResponse())) {
            showAlert(Alert.AlertType.ERROR,
                    "Số dư khả dụng không đủ. Số dư khả dụng: "
                    + VND.format((long) answer.getAvailableBalance()) + " ₫");
            return;
        }
        if (!"200 OK".equals(answer.getResponse())) {
            showAlert(Alert.AlertType.ERROR, "Rút tiền thất bại: " + answer.getResponse());
            return;
        }

        refreshBalanceLabel();
        showAlert(Alert.AlertType.INFORMATION,
                "Rút tiền thành công. Số dư còn lại: "
                + VND.format((long) answer.getBalance()) + " ₫");
    }

    @FXML
    private void handleMyHistory() {
        com.group15.daugia.client.controller.MyAuctionHistoryController controller =
            com.group15.daugia.client.util.SceneChanger.changeTo(
                "com.group15.daugia.clientResources/my_auction_history.fxml");
    }

    private void refreshBalanceLabel() {
        if (lblBalance == null) {
            return;
        }

        try {
            String token = SessionManager.getToken() != null ? SessionManager.getToken() : "";
            String data = ShortConnectNetwork.shortReq("GET-BALANCE", "{\"token\":\"" + token + "\"}");
            JSONMoneyTemp answer = gson.fromJson(data, JSONMoneyTemp.class);
            if (answer != null && "200 OK".equals(answer.getResponse())) {
                lblBalance.setText("Số dư: " + VND.format((long) answer.getAvailableBalance()) + " ₫");
                return;
            }
        } catch (Exception e) {
            System.err.println("[MenuBidderController] Lỗi khi tải số dư: " + e.getMessage());
        }

        lblBalance.setText("Số dư: --");
    }

    // -----------------------------------------------------------------------
    // Build product card
    // -----------------------------------------------------------------------
    private VBox buildCard(BaseItem item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(220);
        card.setMaxWidth(220);

        // ---- Header: tên + badge trạng thái ----
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("product-card-name");
        lblName.setWrapText(true);
        lblName.setMaxWidth(140);

        Label badge = buildStatusBadge(item.getStatus());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        headerRow.getChildren().addAll(lblName, spacer, badge);

        // ---- Người bán ----
        String sellerText = item.getSeller() != null ? "Người bán: " + item.getSeller() : "Người bán: —";
        Label lblSeller = new Label(sellerText);
        lblSeller.getStyleClass().add("product-card-seller");

        // ---- Divider ----
        Pane divider = new Pane();
        divider.getStyleClass().add("product-card-divider");
        divider.setMinHeight(1);

        // ---- Giá hiện tại ----
        Label lblPriceKey = new Label("Giá hiện tại");
        lblPriceKey.getStyleClass().add("product-card-price-label");

        double displayPrice = item.getCurPrice() > 0 ? item.getCurPrice() : item.getPrice();
        Label lblPriceVal = new Label(VND.format((long) displayPrice) + " ₫");
        lblPriceVal.getStyleClass().add("product-card-price-value");

        // ---- Thời gian còn lại ----
        String timeKey = "SCHEDULED".equalsIgnoreCase(item.getStatus())
                ? "Bắt đầu sau"
                : "Thời gian còn lại";
        Label lblTimeKey = new Label(timeKey);
        lblTimeKey.getStyleClass().add("product-card-time-label");

        Label lblTimeVal = new Label(formatSeconds(item.getSecondsRemaining(), item.getStatus()));
        lblTimeVal.getStyleClass().add("product-card-time-value");
        countdownLabels.put(item.getAuctionId(), lblTimeVal);

        card.getChildren().addAll(
                headerRow, lblSeller, divider,
                lblPriceKey, lblPriceVal,
                lblTimeKey, lblTimeVal);

        // ---- Click handler ----
        card.setOnMouseClicked(e -> {
            System.out.println("[MenuBidder] Vào phòng đấu giá: " + item.getName()
                    + " (auctionId=" + item.getAuctionId() + ")");
            BiddingController controller =
                    SceneChanger.changeTo("com.group15.daugia.clientResources/bidding.fxml");
            if (controller != null) {
                controller.setAuction(item.getAuctionId(), item.getName());
            }
        });

        return card;
    }

    private Label buildStatusBadge(String status) {
        Label badge = new Label();
        if (status == null) {
            badge.setText("—");
            badge.getStyleClass().add("badge-scheduled");
            return badge;
        }
        switch (status.toUpperCase()) {
            case "ACTIVE":
                badge.setText("● Đang đấu");
                badge.getStyleClass().add("badge-active");
                break;
            case "SCHEDULED":
                badge.setText("Sắp diễn ra");
                badge.getStyleClass().add("badge-scheduled");
                break;
            case "ENDED":
                badge.setText("Đã kết thúc");
                badge.getStyleClass().add("badge-ended");
                break;
            case "CANCELLED":
                badge.setText("Đã hủy");
                badge.getStyleClass().add("badge-cancelled");
                break;
            default:
                badge.setText(status);
                badge.getStyleClass().add("badge-scheduled");
        }
        return badge;
    }

    private String formatSeconds(long seconds, String status) {
        if ("ENDED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            return "Đã kết thúc";
        }
        if (seconds <= 0) {
            return "SCHEDULED".equalsIgnoreCase(status) ? "Sắp bắt đầu" : "—";
        }
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm %02ds", h, m, s);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return String.format("%ds", s);
    }

    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        autoRefreshTimeline =
                new Timeline(new KeyFrame(Duration.seconds(10), e -> loadItems(false)));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void startLocalCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickCountdowns()));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void tickCountdowns() {
        for (Map.Entry<Integer, Label> entry : countdownLabels.entrySet()) {
            Integer auctionId = entry.getKey();
            Label label = entry.getValue();
            String status = auctionStatuses.getOrDefault(auctionId, "");
            if ("ENDED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                label.setText("Đã kết thúc");
                continue;
            }
            if (!"ACTIVE".equalsIgnoreCase(status) && !"SCHEDULED".equalsIgnoreCase(status)) {
                continue;
            }
            long cur = localCountdowns.getOrDefault(auctionId, 0L);
            if (cur > 0) {
                localCountdowns.put(auctionId, cur - 1);
            }
            long display = localCountdowns.getOrDefault(auctionId, 0L);
            label.setText(formatSeconds(display, status));
        }
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private void stopLocalCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
