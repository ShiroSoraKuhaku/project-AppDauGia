package com.group15.daugia.client.controller;

import com.google.gson.Gson;
import com.group15.daugia.client.model.SessionManager;
import com.group15.daugia.client.network.ShortConnectNetwork;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import com.group15.daugia.shared.model.BaseItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;

public class MenuSellerController implements Initializable {

    @FXML private TableView<BaseItem> tableProducts;
    @FXML private TableColumn<BaseItem, String> colName;
    @FXML private TableColumn<BaseItem, String> colDesc;
    @FXML private TableColumn<BaseItem, Double> colPrice;
    @FXML private TableColumn<BaseItem, String> colStartTime;
    @FXML private TableColumn<BaseItem, String> colEndTime;

    @FXML private TextField txtName;
    @FXML private TextField txtDesc;
    @FXML private TextField txtPrice;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;

    private ObservableList<BaseItem> productList;
    private final Gson gson = new Gson();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DecimalFormat PRICE_FORMATTER =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        tableProducts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : formatPriceWithUnit(price));
            }
        });
        colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));

        productList = FXCollections.observableArrayList();
        tableProducts.setItems(productList);
        loadMyProducts();

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtName.setText(newSelection.getName());
                txtDesc.setText(newSelection.getDescription());
                txtPrice.setText(formatEditablePrice(newSelection.getPrice()));
                txtStartTime.setText(newSelection.getStartTime());
                txtEndTime.setText(newSelection.getEndTime());
            }
        });
    }

    @FXML
    void handleAddProduct(ActionEvent event) {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Double price = parsePrice();
        String startTime = txtStartTime.getText().trim();
        String endTime = txtEndTime.getText().trim();

        if (price == null || !validateProductInput(name, price) || !validateAuctionTime(startTime, endTime)) {
            return;
        }

        JSONItemTemp item = new JSONItemTemp();
        item.setToken(SessionManager.getToken());
        item.setName(name);
        item.setDesc(desc);
        item.setPrice(price);
        item.setStartTime(startTime);
        item.setEndTime(endTime);

        String data = ShortConnectNetwork.shortReq("SELL-ITEM", gson.toJson(item));
        JSONItemTemp answer = parseItemResponse(data, "Đăng sản phẩm thất bại");
        if (answer == null) {
            return;
        }

        if (isSuccess(answer)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng sản phẩm thành công.");
            clearFields();
            loadMyProducts();
        } else {
            showAlert(Alert.AlertType.ERROR, "Đăng sản phẩm thất bại", "Máy chủ trả về: " + answer.getResponse());
        }
    }

    @FXML
    void handleUpdateProduct(ActionEvent event) {
        BaseItem selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trong bảng trước khi sửa.");
            return;
        }

        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Double price = parsePrice();
        String startTime = txtStartTime.getText().trim();
        String endTime = txtEndTime.getText().trim();

        if (price == null || !validateProductInput(name, price) || !validateAuctionTime(startTime, endTime)) {
            return;
        }

        JSONItemTemp item = new JSONItemTemp();
        item.setId(Integer.parseInt(selected.getId()));
        item.setToken(SessionManager.getToken());
        item.setName(name);
        item.setDesc(desc);
        item.setPrice(price);
        item.setStartTime(startTime);
        item.setEndTime(endTime);

        String data = ShortConnectNetwork.shortReq("UPDATE-ITEM", gson.toJson(item));
        JSONItemTemp answer = parseItemResponse(data, "Sửa sản phẩm thất bại");
        if (answer == null) {
            return;
        }

        if (isSuccess(answer)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sửa thông tin sản phẩm thành công.");
            clearFields();
            loadMyProducts();
        } else {
            showAlert(Alert.AlertType.ERROR, "Sửa sản phẩm thất bại", "Máy chủ trả về: " + answer.getResponse());
        }
    }

    @FXML
    void handleDeleteProduct(ActionEvent event) {
        BaseItem selected = tableProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.ERROR, "Chưa chọn sản phẩm", "Vui lòng chọn một sản phẩm trong bảng trước khi xóa.");
            return;
        }

        JSONItemTemp item = new JSONItemTemp();
        item.setId(Integer.parseInt(selected.getId()));
        item.setToken(SessionManager.getToken());

        String data = ShortConnectNetwork.shortReq("DELETE-ITEM", gson.toJson(item));
        JSONItemTemp answer = parseItemResponse(data, "Xóa sản phẩm thất bại");
        if (answer == null) {
            return;
        }

        if (isSuccess(answer)) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Xóa sản phẩm thành công.");
            clearFields();
            loadMyProducts();
        } else {
            showAlert(Alert.AlertType.ERROR, "Xóa sản phẩm thất bại", "Máy chủ trả về: " + answer.getResponse());
        }
    }

    private void loadMyProducts() {
        if (SessionManager.getToken() == null || SessionManager.getToken().isBlank()) {
            return;
        }

        JSONItemTemp request = new JSONItemTemp();
        request.setToken(SessionManager.getToken());

        String data = ShortConnectNetwork.shortReq("GET-MY-ITEMS", gson.toJson(request));
        JSONItemListTemp answer;
        try {
            answer = gson.fromJson(data, JSONItemListTemp.class);
        } catch (RuntimeException e) {
            return;
        }

        productList.clear();
        if (answer == null || answer.getItemList() == null) {
            return;
        }

        for (JSONItemTemp item : answer.getItemList()) {
            productList.add(
                    new BaseItem(
                            String.valueOf(item.getId()),
                            item.getName(),
                            item.getPrice(),
                            item.getDesc(),
                            item.getStartTime(),
                            item.getEndTime()));
        }
    }

    private Double parsePrice() {
        try {
            return Double.parseDouble(normalizePriceInput(txtPrice.getText()));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Giá sản phẩm phải là số.");
            return null;
        }
    }

    private String normalizePriceInput(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", "").replace("VND", "").trim();
    }

    private String formatPrice(double price) {
        return PRICE_FORMATTER.format(price);
    }

    private String formatPriceWithUnit(double price) {
        return formatPrice(price) + " VND";
    }

    private String formatEditablePrice(double price) {
        if (price == Math.rint(price)) {
            return String.valueOf((long) price);
        }
        return String.valueOf(price);
    }

    private boolean validateProductInput(String name, double price) {
        if (name.isBlank() || price <= 0) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Vui lòng nhập tên sản phẩm và giá lớn hơn 0.");
            return false;
        }
        return true;
    }

    private boolean validateAuctionTime(String startTime, String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime, TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, TIME_FORMATTER);
            if (!end.isAfter(start)) {
                showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời gian kết thúc phải sau thời gian bắt đầu.");
                return false;
            }
            return true;
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.ERROR, "Dữ liệu không hợp lệ", "Thời gian phải theo định dạng yyyy-MM-dd HH:mm:ss.");
            return false;
        }
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
        return answer.getResponse() != null && answer.getResponse().charAt(0) == '2';
    }

    private void clearFields() {
        txtName.clear();
        txtDesc.clear();
        txtPrice.clear();
        txtStartTime.clear();
        txtEndTime.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
