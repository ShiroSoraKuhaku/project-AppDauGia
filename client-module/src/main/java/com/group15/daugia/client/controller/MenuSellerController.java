package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.model.BaseItem;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import com.group15.daugia.client.util.SceneChanger;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.NumberFormat;

public class MenuSellerController implements Initializable {

    @FXML private Label lblSubtitle;
    @FXML private FlowPane cardPane;

    @FXML private TextField txtName;
    @FXML private TextField txtDesc;
    @FXML private TextField txtPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<String> cbStartHour;
    @FXML private ComboBox<String> cbStartMinute;
    @FXML private DatePicker dpEndDate;
    @FXML private ComboBox<String> cbEndHour;
    @FXML private ComboBox<String> cbEndMinute;
    @FXML private TextField txtQuickStartAfter;
    @FXML private TextField txtQuickDuration;

    private final Gson gson = new Gson();
    private final NumberFormat vnd = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BaseItem selectedItem;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTimeControls();
        loadMyProducts();
    }

    @FXML
    private void handleRefresh() {
        loadMyProducts();
    }

    @FXML
    private void handleApplyQuickTime() {
        AuctionTimes times = parseQuickTimes();
        if (times == null) {
            return;
        }

        setTimeControls(times.startTime, true);
        setTimeControls(times.endTime, false);
    }

    @FXML
    private void handleAddProduct() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Double price = parsePrice();
        if (price == null || !validateProductInput(name, price)) {
            return;
        }

        AuctionTimes times = resolveAuctionTimes();
        if (times == null) {
            return;
        }

        JSONItemTemp item = buildRequestItem();
        item.setName(name);
        item.setDesc(desc);
        item.setPrice(price);
        item.setStartTime(TIME_FORMATTER.format(times.startTime));
        item.setEndTime(TIME_FORMATTER.format(times.endTime));

        String data = ShortConnectNetwork.shortReq("SELL-ITEM", gson.toJson(item));
        JSONItemTemp answer = parseItemResponse(data, "Đăng sản phẩm thất bại");
        if (answer == null) {
            return;
        }

        if (isSuccess(answer)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công.");
            clearForm();
            loadMyProducts();
        } else {
            showAlert(Alert.AlertType.ERROR, "Đăng sản phẩm thất bại", "Máy chủ trả về: " + answer.getResponse());
        }
    }

    @FXML
    private void handleDeleteProduct() {
        if (selectedItem == null) {
            showAlert(Alert.AlertType.ERROR, "Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trong danh sách trước khi xóa.");
            return;
        }

        JSONItemTemp item = buildRequestItem();
        item.setId(parseSelectedItemId());

        String data = ShortConnectNetwork.shortReq("DELETE-ITEM", gson.toJson(item));
        JSONItemTemp answer = parseItemResponse(data, "Xóa sản phẩm thất bại");
        if (answer == null) {
            return;
        }

        if (isSuccess(answer)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xóa sản phẩm thành công.");
            clearForm();
            loadMyProducts();
        } else {
            showAlert(Alert.AlertType.ERROR, "Xóa sản phẩm thất bại", "Máy chủ trả về: " + answer.getResponse());
        }
    }

    @FXML
    private void handleClearForm() {
        clearForm();
    }

    private void loadMyProducts() {
        String token = SessionManager.getToken();
        if (token == null || token.isBlank()) {
            cardPane.getChildren().clear();
            lblSubtitle.setText("Chưa đăng nhập");
            showEmptyState("Bạn chưa đăng nhập.");
            return;
        }

        lblSubtitle.setText("Đang tải...");
        try {
            JSONItemTemp request = new JSONItemTemp();
            request.setToken(token);

            String data = ShortConnectNetwork.shortReq("GET-MY-ITEMS", gson.toJson(request));
            JSONItemListTemp answer = gson.fromJson(data, JSONItemListTemp.class);

            cardPane.getChildren().clear();
            if (answer == null || answer.getItemList() == null || answer.getItemList().isEmpty()) {
                lblSubtitle.setText("0 sản phẩm");
                showEmptyState("Chưa có sản phẩm nào được đăng.");
                return;
            }

            List<BaseItem> items = new ArrayList<>();
            for (JSONItemTemp item : answer.getItemList()) {
                BaseItem base = new BaseItem(
                        String.valueOf(item.getId()),
                        item.getName(),
                        item.getPrice(),
                        item.getDesc(),
                        item.getStartTime(),
                        item.getEndTime());
                base.setAuctionId(item.getAuctionId());
                base.setSeller(item.getSellerUsername());
                base.setCurPrice(item.getCurPrice() > 0 ? item.getCurPrice() : item.getPrice());
                long displaySeconds = "SCHEDULED".equalsIgnoreCase(item.getStatus())
                        ? item.getSecondsToStart()
                        : item.getSecondsRemaining();
                base.setSecondsRemaining(displaySeconds);
                base.setStatus(item.getStatus());
                items.add(base);
            }

            lblSubtitle.setText(items.size() + " sản phẩm của bạn");
            for (BaseItem item : items) {
                cardPane.getChildren().add(buildCard(item));
            }

            highlightSelectedCard();
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, "Tải dữ liệu thất bại", "Không thể tải danh sách sản phẩm: " + e.getMessage());
            lblSubtitle.setText("Không tải được dữ liệu");
            cardPane.getChildren().clear();
        }
    }

    private VBox buildCard(BaseItem item) {
        VBox card = new VBox(8);
        card.getStyleClass().addAll("product-card", "seller-item-card");
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setUserData(item.getId());

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("product-card-name");
        lblName.setWrapText(true);
        lblName.setMaxWidth(140);

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label lblId = new Label("#" + item.getId());
        lblId.getStyleClass().add("seller-panel-subtitle");

        headerRow.getChildren().addAll(lblName, spacer, lblId);

        Label lblDesc = new Label(item.getDescription() != null && !item.getDescription().isBlank()
                ? item.getDescription()
                : "Không có mô tả.");
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add("product-card-seller");

        Pane divider = new Pane();
        divider.getStyleClass().add("product-card-divider");
        divider.setMinHeight(1);

        Label lblPriceKey = new Label("Giá sàn");
        lblPriceKey.getStyleClass().add("product-card-price-label");

        boolean hasAuction = item.getAuctionId() > 0;
        String priceLabel = hasAuction ? "Giá hiện tại" : "Giá sàn";
        double displayPrice = item.getCurPrice() > 0 ? item.getCurPrice() : item.getPrice();
        lblPriceKey.setText(priceLabel);

        Label lblPriceVal = new Label(vnd.format((long) displayPrice) + " ₫");
        lblPriceVal.getStyleClass().add("product-card-price-value");

        Label lblTimeKey = new Label("Thời gian");
        lblTimeKey.getStyleClass().add("product-card-time-label");

        Label lblTimeVal = new Label(formatRange(item.getStartTime(), item.getEndTime()));
        lblTimeVal.getStyleClass().add("product-card-time-value");
        lblTimeVal.setWrapText(true);

        Label lblStatus = new Label(statusDisplayText(item.getStatus()));
        lblStatus.getStyleClass().add("seller-panel-subtitle");

        Button btnViewAuction = new Button("Xem phiên");
        btnViewAuction.getStyleClass().add("menu-button-secondary");
        btnViewAuction.setMaxWidth(Double.MAX_VALUE);
        btnViewAuction.setDisable(!hasAuction);
        btnViewAuction.setOnAction(e -> {
            e.consume();
            openAuctionDetail(item);
        });

        card.getChildren().addAll(
                headerRow,
                lblDesc,
                divider,
                lblPriceKey,
                lblPriceVal,
                lblTimeKey,
                lblTimeVal,
                lblStatus,
                btnViewAuction);

        card.setOnMouseClicked(e -> selectItem(item));
        return card;
    }

    private void openAuctionDetail(BaseItem item) {
        if (item.getAuctionId() <= 0) {
            showAlert(Alert.AlertType.INFORMATION, "Chưa có phiên đấu giá", "Sản phẩm này chưa có phiên đấu giá để xem.");
            return;
        }

        BiddingController controller = SceneChanger.changeTo("com.group15.daugia.clientResources/bidding.fxml");
        if (controller != null) {
            controller.setSellerAuction(item.getAuctionId(), item.getName(), item.getSeller());
        }
    }

    private void selectItem(BaseItem item) {
        selectedItem = item;
        txtName.setText(safeText(item.getName()));
        txtDesc.setText(safeText(item.getDescription()));
        txtPrice.setText(String.valueOf(item.getPrice()));
        setTimeControlsFromText(item.getStartTime(), true);
        setTimeControlsFromText(item.getEndTime(), false);
        highlightSelectedCard();
    }

    private void highlightSelectedCard() {
        String selectedId = selectedItem != null ? selectedItem.getId() : null;
        for (Node node : cardPane.getChildren()) {
            if (!(node instanceof VBox card)) {
                continue;
            }
            card.getStyleClass().remove("seller-item-card-selected");
            Object id = card.getUserData();
            if (selectedId != null && selectedId.equals(String.valueOf(id))) {
                card.getStyleClass().add("seller-item-card-selected");
            }
        }
    }

    private void clearForm() {
        selectedItem = null;
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        clearTimeControls(true);
        clearTimeControls(false);
        highlightSelectedCard();
    }

    private AuctionTimes resolveAuctionTimes() {
        if (dpStartDate.getValue() == null && dpEndDate.getValue() == null) {
            AuctionTimes quickTimes = parseQuickTimes();
            if (quickTimes != null) {
                return quickTimes;
            }
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Vui lòng chọn thời gian bắt đầu/kết thúc hoặc dùng phần mở nhanh.");
            return null;
        }

        LocalDateTime startTime = getSelectedTime(true, "Thời gian bắt đầu");
        LocalDateTime endTime = getSelectedTime(false, "Thời gian kết thúc");
        if (startTime == null || endTime == null) {
            return null;
        }

        if (!endTime.isAfter(startTime)) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời gian kết thúc phải sau thời gian bắt đầu.");
            return null;
        }

        return new AuctionTimes(startTime, endTime);
    }

    private void setupTimeControls() {
        List<String> hours = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }

        List<String> minutes = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }

        cbStartHour.setItems(FXCollections.observableArrayList(hours));
        cbEndHour.setItems(FXCollections.observableArrayList(hours));
        cbStartMinute.setItems(FXCollections.observableArrayList(minutes));
        cbEndMinute.setItems(FXCollections.observableArrayList(minutes));

        cbStartHour.setValue("00");
        cbStartMinute.setValue("00");
        cbEndHour.setValue("01");
        cbEndMinute.setValue("00");
    }

    private LocalDateTime getSelectedTime(boolean start, String fieldName) {
        DatePicker datePicker = start ? dpStartDate : dpEndDate;
        ComboBox<String> hourBox = start ? cbStartHour : cbEndHour;
        ComboBox<String> minuteBox = start ? cbStartMinute : cbEndMinute;
        LocalDate date = datePicker.getValue();

        if (date == null) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Vui lòng chọn ngày cho " + fieldName.toLowerCase(Locale.ROOT) + ".");
            return null;
        }

        int hour = parseTimePart(hourBox.getValue(), 0);
        int minute = parseTimePart(minuteBox.getValue(), 0);
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    private int parseTimePart(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void setTimeControlsFromText(String value, boolean start) {
        if (value == null || value.isBlank()) {
            clearTimeControls(start);
            return;
        }

        LocalDateTime parsed = parseTime(value, start ? "Thời gian bắt đầu" : "Thời gian kết thúc");
        if (parsed != null) {
            setTimeControls(parsed, start);
        }
    }

    private void setTimeControls(LocalDateTime value, boolean start) {
        DatePicker datePicker = start ? dpStartDate : dpEndDate;
        ComboBox<String> hourBox = start ? cbStartHour : cbEndHour;
        ComboBox<String> minuteBox = start ? cbStartMinute : cbEndMinute;
        datePicker.setValue(value.toLocalDate());
        hourBox.setValue(String.format("%02d", value.getHour()));
        minuteBox.setValue(String.format("%02d", value.getMinute()));
    }

    private void clearTimeControls(boolean start) {
        DatePicker datePicker = start ? dpStartDate : dpEndDate;
        ComboBox<String> hourBox = start ? cbStartHour : cbEndHour;
        ComboBox<String> minuteBox = start ? cbStartMinute : cbEndMinute;
        datePicker.setValue(null);
        hourBox.setValue(start ? "00" : "01");
        minuteBox.setValue("00");
    }

    private AuctionTimes parseQuickTimes() {
        String afterRaw = txtQuickStartAfter != null && txtQuickStartAfter.getText() != null
                ? txtQuickStartAfter.getText().trim()
                : "";
        String durationRaw = txtQuickDuration != null && txtQuickDuration.getText() != null
                ? txtQuickDuration.getText().trim()
                : "";

        if (afterRaw.isEmpty() && durationRaw.isEmpty()) {
            return null;
        }

        if (afterRaw.isEmpty() || durationRaw.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Vui lòng nhập cả thời gian mở sau và thời lượng.");
            return null;
        }

        long afterMinutes;
        long durationMinutes;
        try {
            afterMinutes = Long.parseLong(afterRaw);
            durationMinutes = Long.parseLong(durationRaw);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời gian mở nhanh phải là số phút.");
            return null;
        }

        if (afterMinutes < 0 || durationMinutes <= 0) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời gian phải lớn hơn hoặc bằng 0, thời lượng phải lớn hơn 0.");
            return null;
        }

        LocalDateTime start = LocalDateTime.now().plusMinutes(afterMinutes);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        return new AuctionTimes(start, end);
    }

    private LocalDateTime parseTime(String value, String fieldName) {
        try {
            return LocalDateTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", fieldName + " phải theo định dạng yyyy-MM-dd HH:mm:ss.");
            return null;
        }
    }

    private Double parsePrice() {
        try {
            return Double.parseDouble(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá sản phẩm phải là số.");
            return null;
        }
    }

    private boolean validateProductInput(String name, double price) {
        if (name.isBlank() || price <= 0) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Vui lòng nhập tên sản phẩm và giá lớn hơn 0.");
            return false;
        }
        return true;
    }

    private JSONItemTemp buildRequestItem() {
        JSONItemTemp item = new JSONItemTemp();
        item.setToken(SessionManager.getToken());
        return item;
    }

    private JSONItemTemp parseItemResponse(String data, String title) {
        try {
            return gson.fromJson(data, JSONItemTemp.class);
        } catch (RuntimeException e) {
            showAlert(Alert.AlertType.ERROR, title, "Máy chủ trả về dữ liệu không hợp lệ: " + data);
            return null;
        }
    }

    private boolean isSuccess(JSONItemTemp answer) {
        return answer != null && answer.getResponse() != null && !answer.getResponse().isEmpty() && answer.getResponse().charAt(0) == '2';
    }

    private int parseSelectedItemId() {
        try {
            return Integer.parseInt(selectedItem.getId());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("ID sản phẩm không hợp lệ: " + selectedItem.getId(), e);
        }
    }

    private String formatRange(String startTime, String endTime) {
        String start = startTime == null || startTime.isBlank() ? "—" : startTime;
        String end = endTime == null || endTime.isBlank() ? "—" : endTime;
        return start + " → " + end;
    }

    private String statusDisplayText(String status) {
        if (status == null || status.isBlank()) {
            return "Chưa có phiên đấu giá";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "SCHEDULED" -> "Sắp diễn ra";
            case "ACTIVE" -> "Đang đấu giá";
            case "ENDED" -> "Đã kết thúc";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void showEmptyState(String message) {
        Label empty = new Label(message);
        empty.getStyleClass().add("seller-empty-state");
        cardPane.getChildren().add(empty);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static final class AuctionTimes {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        private AuctionTimes(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
