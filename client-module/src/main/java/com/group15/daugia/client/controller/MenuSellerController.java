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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuSellerController implements Initializable {

    @FXML private TableView<BaseItem> tableProducts;
    @FXML private TableColumn<BaseItem, String> colName;
    @FXML private TableColumn<BaseItem, String> colDesc;
    @FXML private TableColumn<BaseItem, Double> colPrice;

    @FXML private TextField txtName;
    @FXML private TextField txtDesc;
    @FXML private TextField txtPrice;

    private ObservableList<BaseItem> productList;
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        productList = FXCollections.observableArrayList();
        tableProducts.setItems(productList);
        loadMyProducts();

        tableProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtName.setText(newSelection.getName());
                txtDesc.setText(newSelection.getDescription());
                txtPrice.setText(String.valueOf(newSelection.getPrice()));
            }
        });
    }

    @FXML
    void handleAddProduct(ActionEvent event) {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Double price = parsePrice();

        if (price == null || !validateProductInput(name, price)) {
            return;
        }

        JSONItemTemp item = new JSONItemTemp();
        item.setToken(SessionManager.getToken());
        item.setName(name);
        item.setDesc(desc);
        item.setPrice(price);

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

        if (price == null || !validateProductInput(name, price)) {
            return;
        }

        JSONItemTemp item = new JSONItemTemp();
        item.setId(Integer.parseInt(selected.getId()));
        item.setToken(SessionManager.getToken());
        item.setName(name);
        item.setDesc(desc);
        item.setPrice(price);

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
                            item.getDesc()));
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
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
