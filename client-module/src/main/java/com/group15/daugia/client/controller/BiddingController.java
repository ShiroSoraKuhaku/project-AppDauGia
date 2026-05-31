package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.client.network.WatchNetwork;
import com.group15.daugia.client.util.SceneChanger;
import com.group15.daugia.shared.JSON.JSONAuctionEventTemp;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONAutoBidTemp;
import com.group15.daugia.shared.JSON.JSONBidHistoryTemp;
import com.group15.daugia.shared.JSON.JSONBidTemp;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BiddingController implements Initializable {

  private static final int MAX_HISTORY_ITEMS = 20;
  private static final int MAX_CHART_POINTS = 50;
  private static final DateTimeFormatter SERVER_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter UI_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

  @FXML private Label lblProductTitle;
  @FXML private Label lblStatusBadge;
  @FXML private Label lblSeller;
  @FXML private Label lblStartPrice;
  @FXML private Label lblLeader;
  @FXML private Label lblCurPrice;
  @FXML private Label lblCountdown;
  @FXML private Label lblHistoryCount;

  @FXML private TextField txtBidAmount;
  @FXML private Button btnPlaceBid;
  @FXML private Button btnAutoBid;

  @FXML private HBox autoBidPanel;
  @FXML private TextField txtMaxAmount;
  @FXML private TextField txtBidStep;

  @FXML private ListView<String> bidHistoryList;
  @FXML private LineChart<String, Number> priceChart;

  private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

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
    initChart();
  }

  public void setAuction(int auctionId, String itemName) {
    this.auctionId = auctionId;
    lblProductTitle.setText(itemName);
    loadAuctionState();
    loadBidHistory();
    startWatching();
  }

  private void initChart() {
    priceSeries.setName("Giá cao nhất");
    priceChart.setCreateSymbols(false);
    priceChart.setAnimated(false);
    priceChart.setLegendVisible(true);
    priceChart.setData(FXCollections.observableArrayList(priceSeries));
  }

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

  private void loadBidHistory() {
    try {
      JSONBidHistoryTemp req = new JSONBidHistoryTemp();
      req.setAuctionId(auctionId);
      req.setToken(SessionManager.getToken());

      String response = ShortConnectNetwork.shortReq("GET-BID-HISTORY", gson.toJson(req));
      JSONBidHistoryTemp answer = gson.fromJson(response, JSONBidHistoryTemp.class);
      if (answer == null || !"200 OK".equals(answer.getResponse()) || answer.getBids() == null) {
        return;
      }

      List<String> lines = new ArrayList<>();
      for (JSONBidHistoryTemp.BidRecord bid : answer.getBids()) {
        lines.add(formatHistoryLine(bid));
      }

      List<JSONBidHistoryTemp.BidRecord> chartData = new ArrayList<>(answer.getBids());
      Collections.reverse(chartData);

      Platform.runLater(
          () -> {
            bidHistoryList.getItems().setAll(lines);
            trimHistory();
            refreshHistoryCount();
            populateChart(chartData);
          });
    } catch (Exception e) {
      System.err.println("[BiddingController] Lỗi GET-BID-HISTORY: " + e.getMessage());
    }
  }

  private void startWatching() {
    watchNetwork.startWatch(
        auctionId,
        SessionManager.getToken(),
        snapshot -> Platform.runLater(() -> applySnapshot(snapshot)),
        event -> Platform.runLater(() -> handleEvent(event)));
  }

  private void applySnapshot(JSONAuctionTemp s) {
    if (s == null) return;

    if (s.getTitle() != null && !s.getTitle().isEmpty()) {
      lblProductTitle.setText(s.getTitle());
    }

    String status = s.getStatus();
    applyStatus(status);
    lblStartPrice.setText(formatVnd(s.getStartPrice()));
    lblCurPrice.setText(formatVnd(s.getCurPrice() > 0 ? s.getCurPrice() : s.getStartPrice()));
    lblLeader.setText(s.getCurLeader() != null ? s.getCurLeader() : "-");

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

  private void handleEvent(JSONAuctionEventTemp event) {
    if (event == null) return;

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

    if ("BID_PLACED".equals(event.getEventType()) && event.getBidAmount() > 0) {
      JSONBidHistoryTemp.BidRecord bid =
          JSONBidHistoryTemp.BidRecord.fromEvent(
              event.getBidderUsername(), event.getBidAmount(), LocalDateTime.now().format(SERVER_TIME_FMT));
      bidHistoryList.getItems().add(0, formatHistoryLine(bid));
      trimHistory();
      refreshHistoryCount();
      addChartPoint(bid);
    } else {
      String historyLine = buildHistoryLine(event);
      if (historyLine != null) {
        bidHistoryList.getItems().add(0, historyLine);
        trimHistory();
        refreshHistoryCount();
      }
    }
  }

  private void populateChart(List<JSONBidHistoryTemp.BidRecord> bids) {
    priceSeries.getData().clear();
    for (JSONBidHistoryTemp.BidRecord bid : bids) {
      addChartPoint(bid);
    }
  }

  private void addChartPoint(JSONBidHistoryTemp.BidRecord bid) {
    if (bid == null) {
      return;
    }
    String timeLabel = extractTimeLabel(bid.getCreatedAt());
    priceSeries.getData().add(new XYChart.Data<>(timeLabel, bid.getBidAmount()));
    if (priceSeries.getData().size() > MAX_CHART_POINTS) {
      priceSeries.getData().remove(0);
    }
  }

  private void applyStatus(String status) {
    if (status == null) return;
    currentStatus = status;
    lblStatusBadge.setText(statusDisplayText(status));
    lblStatusBadge.getStyleClass().removeAll("badge-active", "badge-scheduled", "badge-ended", "badge-cancelled");
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

  private void startCountdown() {
    stopCountdown();
    countdownTask =
        scheduler.scheduleAtFixedRate(
            () -> {
              long secs = remainingSeconds.getAndDecrement();
              if (secs < 0) {
                stopCountdown();
                Platform.runLater(
                    () -> {
                      if (countingToStart && "SCHEDULED".equalsIgnoreCase(currentStatus)) {
                        lblCountdown.setText("Sắp bắt đầu");
                      } else {
                        lblCountdown.setText("Đã kết thúc");
                      }
                    });
                return;
              }
              String formatted = formatCountdown(secs);
              Platform.runLater(
                  () -> {
                    if (countingToStart && "SCHEDULED".equalsIgnoreCase(currentStatus)) {
                      lblCountdown.setText("Bắt đầu sau " + formatted);
                    } else {
                      lblCountdown.setText(formatted);
                    }
                  });
            },
            0,
            1,
            TimeUnit.SECONDS);
  }

  private void stopCountdown() {
    if (countdownTask != null && !countdownTask.isCancelled()) {
      countdownTask.cancel(false);
    }
  }

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
      } else {
        String msg = (answer != null && answer.getResponse() != null) ? answer.getResponse() : "Không rõ lỗi";
        if ("409 Conflict".equals(msg)) {
          showAlert(
              Alert.AlertType.ERROR,
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
      autoBidPanel.setVisible(true);
      autoBidPanel.setManaged(true);
      txtMaxAmount.requestFocus();
    } else {
      disableAutoBid();
    }
  }

  @FXML
  private void handleConfirmAutoBid() {
    String maxInput = txtMaxAmount.getText().trim();
    if (maxInput.isEmpty()) {
      showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập giá tối đa.");
      return;
    }

    double maxAmount;
    try {
      maxAmount = Double.parseDouble(maxInput.replace(",", "").replace(".", ""));
    } catch (NumberFormatException e) {
      showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá tối đa phải là một số hợp lệ.");
      return;
    }

    double bidStep = 1.0;
    String bidStepInput = txtBidStep.getText().trim();
    if (!bidStepInput.isEmpty()) {
      try {
        bidStep = Double.parseDouble(bidStepInput.replace(",", "."));
      } catch (NumberFormatException e) {
        showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Bước giá phải là một số hợp lệ.");
        return;
      }
    }

    if (!Double.isFinite(bidStep) || bidStep <= 0) {
      showAlert(Alert.AlertType.ERROR, "Bước giá không hợp lệ", "Bước giá phải lớn hơn 0.");
      return;
    }

    try {
      JSONAutoBidTemp req = new JSONAutoBidTemp();
      req.setAuctionId(auctionId);
      req.setToken(SessionManager.getToken());
      req.setMaxAmount(maxAmount);
      req.setBidStep(bidStep);

      String response = ShortConnectNetwork.shortReq("SET-AUTO-BID", gson.toJson(req));
      JSONAutoBidTemp answer = gson.fromJson(response, JSONAutoBidTemp.class);

      if (answer != null && isSuccess(answer.getResponse())) {
        autoBidEnabled = true;
        btnAutoBid.setText("Tự động: BẬT");
        btnAutoBid.getStyleClass().removeAll("btn-auto-bid");
        btnAutoBid.getStyleClass().add("btn-auto-bid-active");
        autoBidPanel.setVisible(false);
        autoBidPanel.setManaged(false);
        txtMaxAmount.clear();
        txtBidStep.clear();
      } else {
        String msg = (answer != null && answer.getResponse() != null) ? answer.getResponse() : "Không rõ lỗi";
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
    txtBidStep.clear();
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

  private void disableAutoBid() {
    autoBidEnabled = false;
    btnAutoBid.setText("Tự động đấu giá");
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
      case "AUCTION_STARTED":
        return "Phiên đấu giá bắt đầu";
      case "AUCTION_ENDED":
        return "Phiên đấu giá kết thúc. Người thắng: " + (event.getCurLeader() != null ? event.getCurLeader() : "-");
      case "AUCTION_EXTENDED":
        return "Thời gian được gia hạn";
      case "AUCTION_CANCELLED":
        return "Phiên đấu giá bị hủy";
      case "BID_CANCELLED":
        return "Lượt đặt giá bị hủy bởi: " + (event.getBidderUsername() != null ? event.getBidderUsername() : "-");
      default:
        return null;
    }
  }

  private String formatHistoryLine(JSONBidHistoryTemp.BidRecord bid) {
    String time = extractTimeLabel(bid.getCreatedAt());
    String user = (bid.getBidderUsername() == null || bid.getBidderUsername().isBlank()) ? "Ẩn danh" : bid.getBidderUsername();
    return "[" + time + "] " + user + ": " + formatVnd(bid.getBidAmount());
  }

  private String extractTimeLabel(String createdAt) {
    if (createdAt == null || createdAt.isBlank()) {
      return LocalDateTime.now().format(UI_TIME_FMT);
    }
    try {
      return LocalDateTime.parse(createdAt, SERVER_TIME_FMT).format(UI_TIME_FMT);
    } catch (Exception ignored) {
      return createdAt;
    }
  }

  private void trimHistory() {
    while (bidHistoryList.getItems().size() > MAX_HISTORY_ITEMS) {
      bidHistoryList.getItems().remove(bidHistoryList.getItems().size() - 1);
    }
  }

  private void refreshHistoryCount() {
    lblHistoryCount.setText(bidHistoryList.getItems().size() + " lượt");
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
    if (status == null) return "-";
    switch (status.toUpperCase()) {
      case "ACTIVE":
        return "Đang đấu";
      case "SCHEDULED":
        return "Sắp diễn ra";
      case "ENDED":
        return "Đã kết thúc";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
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
