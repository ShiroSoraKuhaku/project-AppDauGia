package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.network.WatchNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONAutoBidTemp;
import com.group15.daugia.shared.JSON.JSONBidTemp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BiddingController implements Initializable {

    // --- Info panel ---
    @FXML private Label lblProductTitle;
    @FXML private Label lblStatusBadge;
    @FXML private Label lblSeller;
    @FXML private Label lblStartPrice;
    @FXML private Label lblLeader;
    @FXML private Label lblCurPrice;
    @FXML private Label lblCountdown;
    @FXML private Label lblHistoryCount;

    // --- Action panel ---
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private Button btnAutoBid;

    // --- Auto-bid panel ---
    @FXML private HBox autoBidPanel;
    @FXML private TextField txtMaxAmount;

    // --- History ---
    @FXML private ListView<String> bidHistoryList;

    // --- State ---
    private int auctionId = -1;
    private boolean autoBidEnabled = false;
    private final Gson gson = new Gson();
    private final WatchNetwork watchNetwork = new WatchNetwork();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> countdownTask;
    private final AtomicLong remainingSeconds = new AtomicLong(0);
    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));
    private String currentStatus;
    private boolean countingToStart = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Sẽ được gọi sau khi setAuction() nếu auctionId chưa được đặt
    }

    /**
     * Được gọi từ MenuBidderController ngay sau khi load FXML.
     *
     * @param auctionId ID phiên đấu giá
     * @param itemName  Tên sản phẩm (hiển thị tạm trong khi tải)
     */
    public void setAuction(int auctionId, String itemName) {
        this.auctionId = auctionId;
        lblProductTitle.setText(itemName);
        loadAuctionState();
        startWatching();
    }

    // =========================================================================
    // Server calls
    // =========================================================================

    /** GET-AUCTION-STATE để hiển thị thông tin ban đầu */
    private void loadAuctionState() {
        try {
            JSONAuctionTemp req = new JSONAuctionTemp();
            req.setAuctionId(auctionId);
            req.setToken(SessionManager.getToken());

            String response = ShortConnectNetwork.shortReq("GET-AUCTION-STATE", gson.toJson(req));
            JSONAuctionTemp state = gson.fromJson(response, JSONAuctionTemp.class);
            if (state != null) {
                Platform.runLater(() -> applySnapshot(state));
            }
        } catch (Exception e) {
            System.err.println("[BiddingController] Lỗi GET-AUCTION-STATE: " + e.getMessage());
        }
    }

    /** Bắt đầu WATCH-AUCTION để nhận push event realtime */
    private void startWatching() {
        watchNetwork.startWatch(
                auctionId,
                SessionManager.getToken(),
                snapshot -> Platform.runLater(() -> applySnapshot(snapshot)),
                event   -> Platform.runLater(() -> handleEvent(event))
        );
    }

    // =========================================================================
    // UI update
    // =========================================================================

    /** Áp dụng snapshot (từ GET-AUCTION-STATE hoặc ACK của WATCH-AUCTION) */
    private void applySnapshot(JSONAuctionTemp s) {
        if (s == null) return;

        if (s.getTitle() != null && !s.getTitle().isEmpty()) {
            lblProductTitle.setText(s.getTitle());
        }

        String status = s.getStatus();
        applyStatus(status);
        lblStartPrice.setText(formatVnd(s.getStartPrice()));
        lblCurPrice.setText(formatVnd(s.getCurPrice() > 0 ? s.getCurPrice() : s.getStartPrice()));
        lblLeader.setText(s.getCurLeader() != null ? s.getCurLeader() : "—");

        if ("SCHEDULED".equalsIgnoreCase(status)) {
            long secsToStart = Math.max(0, s.getSecondsToStart());
            countingToStart = true;
            remainingSeconds.set(secsToStart);
            if (secsToStart > 0) {
                startCountdown();
            } else {
                stopCountdown();
                lblCountdown.setText("Sắp bắt đầu");
            }
        } else {
            countingToStart = false;
            long secs = Math.max(0, s.getSecondsRemaining());
            remainingSeconds.set(secs);
            if (isEndedStatus(status)) {
                stopCountdown();
                lblCountdown.setText("Đã kết thúc");
            } else {
                startCountdown();
            }
        }
    }

    /** Xử lý push event từ server */
    private void handleEvent(JSONAuctionEventTemp event) {
        if (event == null) return;

        // Cập nhật giá + leader nếu có thay đổi
        if (event.getCurPrice() > 0) {
            lblCurPrice.setText(formatVnd(event.getCurPrice()));
        }
        if (event.getCurLeader() != null) {
            lblLeader.setText(event.getCurLeader());
        }
        if (event.getStatus() != null) {
            applyStatus(event.getStatus());
            if (isEndedStatus(currentStatus)) {
                lblCountdown.setText("Đã kết thúc");
            }
        }
        if (event.getSecondsRemaining() > 0) {
            remainingSeconds.set(event.getSecondsRemaining());
            countingToStart = false;
            if (!isEndedStatus(currentStatus)) {
                startCountdown();
            }
        }

        // Thêm vào lịch sử
        String historyLine = buildHistoryLine(event);
        if (historyLine != null) {
            bidHistoryList.getItems().add(0, historyLine);
            lblHistoryCount.setText(bidHistoryList.getItems().size() + " lượt");
        }
    }

    private void applyStatus(String status) {
        if (status == null) return;
        currentStatus = status;
        lblStatusBadge.setText(statusDisplayText(status));
        lblStatusBadge.getStyleClass().removeAll(
                "badge-active", "badge-scheduled", "badge-ended", "badge-cancelled");
        switch (status.toUpperCase()) {
            case "ACTIVE":
                lblStatusBadge.getStyleClass().add("badge-active");
                break;
            case "SCHEDULED":
                lblStatusBadge.getStyleClass().add("badge-scheduled");
                break;
            case "ENDED":
            case "CANCELLED":
                lblStatusBadge.getStyleClass().add("badge-ended");
                btnPlaceBid.setDisable(true);
                btnAutoBid.setDisable(true);
                stopCountdown();
                break;
            default:
                lblStatusBadge.getStyleClass().add("badge-scheduled");
        }
    }

    // =========================================================================
    // Countdown timer
    // =========================================================================

    private void startCountdown() {
        stopCountdown();
        countdownTask = scheduler.scheduleAtFixedRate(() -> {
            long secs = remainingSeconds.getAndDecrement();
            if (secs < 0) {
                stopCountdown();
                Platform.runLater(() -> {
                    if (countingToStart && "SCHEDULED".equalsIgnoreCase(currentStatus)) {
                        lblCountdown.setText("Sắp bắt đầu");
                    } else {
                        lblCountdown.setText("Đã kết thúc");
                    }
                });
                return;
            }
            String formatted = formatCountdown(secs);
            Platform.runLater(() -> {
                if (countingToStart && "SCHEDULED".equalsIgnoreCase(currentStatus)) {
                    lblCountdown.setText("Bắt đầu sau " + formatted);
                } else {
                    lblCountdown.setText(formatted);
                }
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void stopCountdown() {
        if (countdownTask != null && !countdownTask.isCancelled()) {
            countdownTask.cancel(false);
        }
    }

    // =========================================================================
    // Action handlers
    // =========================================================================

    @FXML
    private void handlePlaceBid() {
        String input = txtBidAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập số tiền muốn đặt.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(input.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Số tiền phải là một số hợp lệ.");
            return;
        }

        try {
            JSONBidTemp req = new JSONBidTemp();
            req.setAuctionId(auctionId);
            req.setToken(SessionManager.getToken());
            req.setBidAmount(amount);

            String response = ShortConnectNetwork.shortReq("PLACE-BID", gson.toJson(req));
            JSONBidTemp answer = gson.fromJson(response, JSONBidTemp.class);

            if (answer != null && isSuccess(answer.getResponse())) {
                txtBidAmount.clear();
                // Event sẽ được nhận qua WATCH-AUCTION push
                String line = "✔ Bạn đã đặt: " + formatVnd(amount);
                bidHistoryList.getItems().add(0, line);
                lblHistoryCount.setText(bidHistoryList.getItems().size() + " lượt");
            } else {
                String msg = (answer != null && answer.getResponse() != null)
                        ? answer.getResponse() : "Không rõ lỗi";
                if ("409 Conflict".equals(msg)) {
                    showAlert(Alert.AlertType.ERROR,
                            "Đặt giá thất bại",
                            "Số dư không đủ hoặc đang bị khóa. Hãy nạp tiền rồi thử lại.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Đặt giá thất bại", "Máy chủ phản hồi: " + msg);
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối máy chủ: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleAutoBid() {
        if (!autoBidEnabled) {
            // Mở panel nhập maxAmount
            autoBidPanel.setVisible(true);
            autoBidPanel.setManaged(true);
            txtMaxAmount.requestFocus();
        } else {
            // Tắt auto-bid
            disableAutoBid();
        }
    }

    @FXML
    private void handleConfirmAutoBid() {
        String input = txtMaxAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập giá tối đa.");
            return;
        }

        double maxAmount;
        try {
            maxAmount = Double.parseDouble(input.replace(",", "").replace(".", ""));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá tối đa phải là một số hợp lệ.");
            return;
        }

        try {
            JSONAutoBidTemp req = new JSONAutoBidTemp();
            req.setAuctionId(auctionId);
            req.setToken(SessionManager.getToken());
            req.setMaxAmount(maxAmount);

            String response = ShortConnectNetwork.shortReq("SET-AUTO-BID", gson.toJson(req));
            JSONAutoBidTemp answer = gson.fromJson(response, JSONAutoBidTemp.class);

            if (answer != null && isSuccess(answer.getResponse())) {
                autoBidEnabled = true;
                btnAutoBid.setText("🤖  Tự động: BẬT");
                btnAutoBid.getStyleClass().removeAll("btn-auto-bid");
                btnAutoBid.getStyleClass().add("btn-auto-bid-active");
                autoBidPanel.setVisible(false);
                autoBidPanel.setManaged(false);
                txtMaxAmount.clear();
                bidHistoryList.getItems().add(0,
                        "🤖 Tự động đấu giá bật (tối đa " + formatVnd(maxAmount) + ")");
                lblHistoryCount.setText(bidHistoryList.getItems().size() + " lượt");
            } else {
                String msg = (answer != null && answer.getResponse() != null)
                        ? answer.getResponse() : "Không rõ lỗi";
                showAlert(Alert.AlertType.ERROR, "Bật tự động thất bại", "Máy chủ phản hồi: " + msg);
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối máy chủ: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAutoBid() {
        autoBidPanel.setVisible(false);
        autoBidPanel.setManaged(false);
        txtMaxAmount.clear();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        stopCountdown();
        scheduler.shutdown();
        if (auctionId >= 0) {
            watchNetwork.stopWatch(auctionId, SessionManager.getToken());
        }
        SceneChanger.changeTo("com.group15.daugia.clientResources/dashboard.fxml");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void disableAutoBid() {
        autoBidEnabled = false;
        btnAutoBid.setText("🤖  Tự động đấu giá");
        btnAutoBid.getStyleClass().removeAll("btn-auto-bid-active");
        if (!btnAutoBid.getStyleClass().contains("btn-auto-bid")) {
            btnAutoBid.getStyleClass().add("btn-auto-bid");
        }
        autoBidPanel.setVisible(false);
        autoBidPanel.setManaged(false);
    }

    private String buildHistoryLine(JSONAuctionEventTemp event) {
        if (event == null || event.getEventType() == null) return null;
        switch (event.getEventType()) {
            case "BID_PLACED":
                return String.format("💰 %s đặt %s",
                        event.getBidderUsername() != null ? event.getBidderUsername() : "Ai đó",
                        formatVnd(event.getBidAmount()));
            case "AUCTION_STARTED":
                return "🟢 Phiên đấu giá bắt đầu!";
            case "AUCTION_ENDED":
                return "🔴 Phiên đấu giá kết thúc. Người thắng: " +
                        (event.getCurLeader() != null ? event.getCurLeader() : "—");
            case "AUCTION_EXTENDED":
                return "⏱ Thời gian được gia hạn thêm.";
            case "AUCTION_CANCELLED":
                return "❌ Phiên đấu giá bị hủy.";
            case "BID_CANCELLED":
                return "↩ Lượt đặt giá bị hủy bởi: " +
                        (event.getBidderUsername() != null ? event.getBidderUsername() : "—");
            default:
                return null;
        }
    }

    private boolean isSuccess(String response) {
        return response != null && !response.isEmpty() && response.charAt(0) == '2';
    }

    private String formatVnd(double amount) {
        return VND.format((long) amount) + " ₫";
    }

    private String formatCountdown(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return String.format("%02d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    private String statusDisplayText(String status) {
        if (status == null) return "—";
        switch (status.toUpperCase()) {
            case "ACTIVE":    return "● Đang đấu";
            case "SCHEDULED": return "Sắp diễn ra";
            case "ENDED":     return "Đã kết thúc";
            case "CANCELLED": return "Đã hủy";
            default:          return status;
        }
    }

    private boolean isEndedStatus(String status) {
        return "ENDED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}