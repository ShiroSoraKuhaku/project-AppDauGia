package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
  private static ItemDAO instance;
  private final DBProperty dbProperty = DBProperty.getInstance();

  private ItemDAO() {}

  public static ItemDAO getItemDao() {
    if (instance == null) {
      instance = new ItemDAO();
    }
    return instance;
  }

  public String addItem(String sellerUsername, String name, double price, String desc) {
    String sql = "insert into items (seller_username, name, price, `desc`) values (?, ?, ?, ?)";

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setString(1, sellerUsername);
      statement.setString(2, name);
      statement.setDouble(3, price);
      statement.setString(4, desc);

      return statement.executeLargeUpdate() == 1 ? "1" : "0";
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String updateItem(int id, String sellerUsername, String name, double price, String desc) {
    String sql =
        "update items set name = ?, price = ?, `desc` = ? where id = ? and seller_username = ?";

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setString(1, name);
      statement.setDouble(2, price);
      statement.setString(3, desc);
      statement.setInt(4, id);
      statement.setString(5, sellerUsername);

      return statement.executeUpdate() == 1 ? "1" : "0";
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String deleteItem(int id, String sellerUsername) {
    String sql = "delete from items where id = ? and seller_username = ?";

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setInt(1, id);
      statement.setString(2, sellerUsername);

      return statement.executeUpdate() == 1 ? "1" : "0";
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JSONItemTemp> getAllItems() {
    String sql = "select id, seller_username, name, price, `desc` from items order by id desc";
    List<JSONItemTemp> items = new ArrayList<>();

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        JSONItemTemp item = new JSONItemTemp();
        item.setId(resultSet.getInt("id"));
        item.setSellerUsername(resultSet.getNString("seller_username"));
        item.setName(resultSet.getNString("name"));
        item.setPrice(resultSet.getDouble("price"));
        item.setDesc(resultSet.getNString("desc"));
        items.add(item);
      }
      return items;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JSONItemTemp> getItemsBySeller(String sellerUsername){
    String sql =
            "select id, seller_username, name, price, `desc` from items where seller_username = ? order by id desc";
    List<JSONItemTemp> items = new ArrayList<>();

    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword()
            );
         PreparedStatement statement = conn.prepareStatement(sql)){

      statement.setString(1, sellerUsername);

      try (ResultSet resultSet = statement.executeQuery()){
        while (resultSet.next()){
          JSONItemTemp item = new JSONItemTemp();
          item.setId(resultSet.getInt("id"));
          item.setSellerUsername(resultSet.getString("seller_username"));
          item.setName(resultSet.getString("name"));
          item.setPrice(resultSet.getDouble("price"));
          item.setDesc(resultSet.getString("desc"));
          items.add(item);
        }
      }

      return items;
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

}
