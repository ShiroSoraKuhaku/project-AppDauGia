package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;
import com.group15.daugia.shared.JSON.JSONItemTemp;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {
  private static ItemDAO instance;

  private ItemDAO() {}

  public static ItemDAO getItemDao() {
    if (instance == null) {
      instance = new ItemDAO();
    }
    return instance;
  }

  private String normalizeStatus(String status, String startTime, String endTime) {
    if (status == null || status.isBlank()) {
      return status;
    }
    if ("CANCELLED".equalsIgnoreCase(status)) {
      return "CANCELLED";
    }

    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    java.time.LocalDateTime parsedEnd = parseDateTime(endTime);
    if (parsedEnd != null && !now.isBefore(parsedEnd)) {
      return "ENDED";
    }

    java.time.LocalDateTime parsedStart = parseDateTime(startTime);
    if (parsedStart != null && !now.isBefore(parsedStart)) {
      return "ACTIVE";
    }

    return "SCHEDULED";
  }

  private java.time.LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    } catch (Exception ignored) {
      return null;
    }
  }

  public int addItem(String sellerUsername, String name, double price, String desc) {
    String sql = "insert into items (seller_username, name, price, `desc`) values (?, ?, ?, ?)";

    DBProperty dbProperty = DBProperty.getInstance();
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
      int id,
      String sellerUsername,
      boolean isAdmin,
      String name,
      double price,
      String desc,
      String startTime,
      String endTime) {
    String itemSql =
        isAdmin
            ? "update items set name = ?, price = ?, `desc` = ? where id = ?"
            : "update items set name = ?, price = ?, `desc` = ? where id = ? and seller_username = ?";
    String auctionSql =
        "update auctions set title = ?, start_price = ?, start_time = ?, end_time = ?, "
            + "version = version + 1 where item_id = ?";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      conn.setAutoCommit(false);

      try (PreparedStatement statement = conn.prepareStatement(itemSql)) {
        statement.setString(1, name);
        statement.setDouble(2, price);
        statement.setString(3, desc);
        statement.setInt(4, id);
        if (!isAdmin) {
          statement.setString(5, sellerUsername);
        }

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

    DBProperty dbProperty = DBProperty.getInstance();
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
        "select i.id, i.seller_username, i.name, i.price, i.`desc`, "
            + "a.id as auction_id, a.start_time, a.end_time, a.status, a.cur_price "
            + "from items i left join auctions a on a.item_id = i.id order by i.id desc";
    List<JSONItemTemp> items = new ArrayList<>();

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql);
        ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        JSONItemTemp item = new JSONItemTemp();
        item.setId(resultSet.getInt("id"));
        item.setAuctionId(resultSet.getInt("auction_id"));
        item.setSellerUsername(resultSet.getString("seller_username"));
        item.setName(resultSet.getString("name"));
        item.setPrice(resultSet.getDouble("price"));
        item.setDesc(resultSet.getString("desc"));
        item.setStartTime(resultSet.getString("start_time"));
        item.setEndTime(resultSet.getString("end_time"));
        String status = normalizeStatus(
            resultSet.getString("status"), item.getStartTime(), item.getEndTime());
        item.setStatus(status);
        item.setCurPrice(resultSet.getDouble("cur_price"));

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if ("ACTIVE".equals(status) && item.getEndTime() != null) {
          java.time.LocalDateTime end = parseDateTime(item.getEndTime());
          if (end != null) {
            long secs = java.time.Duration.between(now, end).getSeconds();
            item.setSecondsRemaining(Math.max(0, secs));
          }
        } else if ("SCHEDULED".equals(status) && item.getStartTime() != null) {
          java.time.LocalDateTime start = parseDateTime(item.getStartTime());
          if (start != null) {
            long secs = java.time.Duration.between(now, start).getSeconds();
            item.setSecondsToStart(Math.max(0, secs));
          }
        }
        items.add(item);
      }
      return items;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Lấy danh sách item có auction SCHEDULED hoặc ACTIVE, loại trừ item của chính user.
   * Hỗ trợ lọc theo tên (LIKE, case-insensitive). nameFilter=null hoặc blank = không lọc.
   */
  public List<JSONItemTemp> getActiveItems(String username, String nameFilter) {
    boolean hasFilter = nameFilter != null && !nameFilter.isBlank();
    String sql =
        "select i.id, i.seller_username, a.title, i.price, i.`desc`, "
            + "a.id as auction_id, a.start_time, a.end_time, a.status, a.cur_price "
            + "from items i join auctions a on a.item_id = i.id "
            + "where a.status IN ('SCHEDULED','ACTIVE') "
            + "and i.seller_username <> ? "
            + (hasFilter ? "and a.title LIKE ? " : "")
            + "order by a.status DESC, i.id desc";

    List<JSONItemTemp> items = new ArrayList<>();
    java.time.format.DateTimeFormatter fmt =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setString(1, username);
      if (hasFilter) {
        statement.setString(2, "%" + nameFilter + "%");
      }

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          JSONItemTemp item = new JSONItemTemp();
          item.setId(resultSet.getInt("id"));
          item.setAuctionId(resultSet.getInt("auction_id"));
          item.setSellerUsername(resultSet.getString("seller_username"));
          item.setName(resultSet.getString("title"));
          item.setPrice(resultSet.getDouble("price"));
          item.setDesc(resultSet.getString("desc"));
          item.setStartTime(resultSet.getString("start_time"));
          item.setEndTime(resultSet.getString("end_time"));
          String status = normalizeStatus(
              resultSet.getString("status"), item.getStartTime(), item.getEndTime());
          item.setStatus(status);
          item.setCurPrice(resultSet.getDouble("cur_price"));

          if ("ENDED".equals(status) || "CANCELLED".equals(status)) {
            continue;
          }

          if ("ACTIVE".equals(status) && item.getEndTime() != null) {
            java.time.LocalDateTime end = parseDateTime(item.getEndTime());
            if (end != null) {
              long secs = java.time.Duration.between(now, end).getSeconds();
              item.setSecondsRemaining(Math.max(0, secs));
            }
          } else if ("SCHEDULED".equals(status) && item.getStartTime() != null) {
            java.time.LocalDateTime start = parseDateTime(item.getStartTime());
            if (start != null) {
              long secs = java.time.Duration.between(now, start).getSeconds();
              item.setSecondsToStart(Math.max(0, secs));
            }
          }
          items.add(item);
        }
      }
      return items;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public List<JSONItemTemp> getItemsBySeller(String sellerUsername) {
    String sql =
        "select i.id, i.seller_username, i.name, i.price, i.`desc`, "
            + "a.id as auction_id, a.start_time, a.end_time, a.status, a.cur_price "
            + "from items i left join auctions a on a.item_id = i.id "
            + "where i.seller_username = ? order by i.id desc";
    List<JSONItemTemp> items = new ArrayList<>();
    java.time.format.DateTimeFormatter fmt =
        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement = conn.prepareStatement(sql)) {

      statement.setString(1, sellerUsername);

      try (ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          JSONItemTemp item = new JSONItemTemp();
          item.setId(resultSet.getInt("id"));
          item.setAuctionId(resultSet.getInt("auction_id"));
          item.setSellerUsername(resultSet.getString("seller_username"));
          item.setName(resultSet.getString("name"));
          item.setPrice(resultSet.getDouble("price"));
          item.setDesc(resultSet.getString("desc"));
          item.setStartTime(resultSet.getString("start_time"));
          item.setEndTime(resultSet.getString("end_time"));
          String status = normalizeStatus(
              resultSet.getString("status"), item.getStartTime(), item.getEndTime());
          item.setStatus(status);
          item.setCurPrice(resultSet.getDouble("cur_price"));

          if ("ACTIVE".equals(status) && item.getEndTime() != null) {
            java.time.LocalDateTime end = parseDateTime(item.getEndTime());
            if (end != null) {
              long secs = java.time.Duration.between(now, end).getSeconds();
              item.setSecondsRemaining(Math.max(0, secs));
            }
          } else if ("SCHEDULED".equals(status) && item.getStartTime() != null) {
            java.time.LocalDateTime start = parseDateTime(item.getStartTime());
            if (start != null) {
              long secs = java.time.Duration.between(now, start).getSeconds();
              item.setSecondsToStart(Math.max(0, secs));
            }
          }
          items.add(item);
        }
      }

      return items;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
