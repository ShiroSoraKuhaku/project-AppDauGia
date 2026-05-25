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

  public int addItem(String sellerUsername, String name, double price, String desc) {
    String sql = "insert into items (seller_username, name, price, `desc`) values (?, ?, ?, ?)";

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      statement.setString(1, sellerUsername);
      statement.setString(2, name);
      statement.setDouble(3, price);
      statement.setString(4, desc);

      if (statement.executeUpdate() != 1) {
        return 0;
      }

      try (ResultSet keys = statement.getGeneratedKeys()) {
        return keys.next() ? keys.getInt(1) : 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String updateItem(
      int id, String sellerUsername, String name, double price, String desc, String startTime, String endTime) {
    String itemSql =
        "update items set name = ?, price = ?, `desc` = ? where id = ? and seller_username = ?";
    String auctionSql =
        "update auctions set title = ?, start_price = ?, start_time = ?, end_time = ?, "
            + "version = version + 1 where item_id = ?";

    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      conn.setAutoCommit(false);

      try (PreparedStatement statement = conn.prepareStatement(itemSql)) {
        statement.setString(1, name);
        statement.setDouble(2, price);
        statement.setString(3, desc);
        statement.setInt(4, id);
        statement.setString(5, sellerUsername);

        if (statement.executeUpdate() != 1) {
          conn.rollback();
          return "0";
        }
      }

      try (PreparedStatement statement = conn.prepareStatement(auctionSql)) {
        statement.setString(1, name);
        statement.setDouble(2, price);
        statement.setString(3, startTime);
        statement.setString(4, endTime);
        statement.setInt(5, id);
        statement.executeUpdate();
      }

      conn.commit();
      return "1";
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String deleteItem(int id, String sellerUsername) {
    String deleteAuctionsSql = "delete from auctions where item_id = ?";
    String deleteItemSql = "delete from items where id = ? and seller_username = ?";

    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      conn.setAutoCommit(false);

      try (PreparedStatement statement = conn.prepareStatement(deleteAuctionsSql)) {
        statement.setInt(1, id);
        statement.executeUpdate();
      }

      try (PreparedStatement statement = conn.prepareStatement(deleteItemSql)) {
        statement.setInt(1, id);
        statement.setString(2, sellerUsername);

        if (statement.executeUpdate() != 1) {
          conn.rollback();
          return "0";
        }
      }

      conn.commit();
      return "1";
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JSONItemTemp> getAllItems() {
    String sql =
        "select i.id, i.seller_username, i.name, i.price, i.`desc`, a.start_time, a.end_time "
            + "from items i left join auctions a on a.item_id = i.id order by i.id desc";
    List<JSONItemTemp> items = new ArrayList<>();

    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        JSONItemTemp item = new JSONItemTemp();
        item.setId(resultSet.getInt("id"));
        item.setSellerUsername(resultSet.getString("seller_username"));
        item.setName(resultSet.getString("name"));
        item.setPrice(resultSet.getDouble("price"));
        item.setDesc(resultSet.getString("desc"));
        item.setStartTime(resultSet.getString("start_time"));
        item.setEndTime(resultSet.getString("end_time"));
        items.add(item);
      }
      return items;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JSONItemTemp> getItemsBySeller(String sellerUsername){
    String sql =
            "select i.id, i.seller_username, i.name, i.price, i.`desc`, a.start_time, a.end_time "
                    + "from items i left join auctions a on a.item_id = i.id "
                    + "where i.seller_username = ? order by i.id desc";
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
          item.setStartTime(resultSet.getString("start_time"));
          item.setEndTime(resultSet.getString("end_time"));
          items.add(item);
        }
      }

      return items;
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

}
