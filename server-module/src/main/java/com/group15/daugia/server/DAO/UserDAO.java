package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserDAO {
  public static final long TOKEN_IDLE_MINUTES = 15;
  public static final String LOGIN_CONFLICT = "LOGIN_CONFLICT";

  private static UserDAO instance;

  private UserDAO() {}

  public static UserDAO getUserDao() {
    if (instance == null) {
      instance = new UserDAO();
    }
    return instance;
  }

  public String[] checkLogin(String username, String password) {
    String sqlCheckCommand = "select username, role from user where username = ? and password = ?";
    String sqlCheckTokenCommand =
        "select token from tokens where username = ? and last_accessed >= DATE_SUB(NOW(), INTERVAL ? MINUTE)";
    String sqlDeleteTokenCommand =
        "delete from tokens where username = ? and last_accessed < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
    String sqlSetTokenCommand =
        "insert into tokens (username, token, last_accessed) values (?, ?, NOW())";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
        PreparedStatement statement1 = conn.prepareStatement(sqlCheckCommand)) {
      statement1.setString(1, username);
      statement1.setString(2, password);

      try (ResultSet resultSet = statement1.executeQuery()) {
        if (!resultSet.next()) {
          return null;
        }

        try (PreparedStatement cleanup = conn.prepareStatement(sqlDeleteTokenCommand)) {
          cleanup.setString(1, username);
          cleanup.setLong(2, TOKEN_IDLE_MINUTES);
          cleanup.executeUpdate();
        }

        try (PreparedStatement tokenCheck = conn.prepareStatement(sqlCheckTokenCommand)) {
          tokenCheck.setString(1, username);
          tokenCheck.setLong(2, TOKEN_IDLE_MINUTES);
          try (ResultSet tokenRs = tokenCheck.executeQuery()) {
            if (tokenRs.next()) {
              return new String[] {LOGIN_CONFLICT, null, null};
            }
          }
        }

        String token = UUID.randomUUID().toString();
        try (PreparedStatement statement2 = conn.prepareStatement(sqlSetTokenCommand)) {
          statement2.setString(1, username);
          statement2.setString(2, token);
          statement2.executeUpdate();
        }

        String role = resultSet.getString("role");
        return new String[] {username, token, role};
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String removeLogin(String username, String token) {
    String sqlDeleteToken = "delete from tokens where username = ? and token = ?";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      PreparedStatement statement1 = conn.prepareStatement(sqlDeleteToken);
      statement1.setString(1, username);
      statement1.setString(2, token);

      int result = statement1.executeUpdate();
      if (result != 1) {
        return null;
      } else {
        return "1";
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String signUp(String username, String password) {
    String sqlAddUser = "insert into user (username, password)" + "values (?, ?)";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      PreparedStatement statement1 = conn.prepareStatement(sqlAddUser);
      statement1.setString(1, username);
      statement1.setString(2, password);
      int rowAffected = statement1.executeUpdate();
      return "1";
    } catch (SQLException e) {
      if (e.getErrorCode() == 1062) {
        return "DUPLICATE";
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public String getUsernameByToken(String token){
    String sqlDelete =
        "delete from tokens where token = ? and last_accessed < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
    String sqlSelect = "select username from tokens where token = ?";
    String sqlTouch = "update tokens set last_accessed = NOW() where token = ?";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword()
            )) {
      try (PreparedStatement delete = conn.prepareStatement(sqlDelete)) {
        delete.setString(1, token);
        delete.setLong(2, TOKEN_IDLE_MINUTES);
        if (delete.executeUpdate() > 0) {
          return null;
        }
      }

      try (PreparedStatement select = conn.prepareStatement(sqlSelect)) {
        select.setString(1, token);
        try (ResultSet resultSet = select.executeQuery()){
          if (resultSet.next()){
            try (PreparedStatement touch = conn.prepareStatement(sqlTouch)) {
              touch.setString(1, token);
              touch.executeUpdate();
            }
            return resultSet.getString("username");
          }
          return null;
        }
      }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

  public String getRoleByToken(String token) {
    String sql =
        "select u.role from user u join tokens t on t.username = u.username where t.token = ?";
    String sqlDelete =
        "delete from tokens where token = ? and last_accessed < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
    String sqlTouch = "update tokens set last_accessed = NOW() where token = ?";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword()
            )) {
      try (PreparedStatement delete = conn.prepareStatement(sqlDelete)) {
        delete.setString(1, token);
        delete.setLong(2, TOKEN_IDLE_MINUTES);
        if (delete.executeUpdate() > 0) {
          return null;
        }
      }

      try (PreparedStatement statement = conn.prepareStatement(sql)) {
        statement.setString(1, token);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            try (PreparedStatement touch = conn.prepareStatement(sqlTouch)) {
              touch.setString(1, token);
              touch.executeUpdate();
            }
            return resultSet.getString("role");
          }
          return null;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean deleteIfTokenExpired(String token) {
    String sqlDelete =
        "delete from tokens where token = ? and last_accessed < DATE_SUB(NOW(), INTERVAL ? MINUTE)";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword()
            );
         PreparedStatement delete = conn.prepareStatement(sqlDelete)) {
      delete.setString(1, token);
      delete.setLong(2, TOKEN_IDLE_MINUTES);
      return delete.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int deleteExpiredTokens(LocalDateTime cutoff) {
    String sql = "delete from tokens where last_accessed < ?";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
         PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setTimestamp(1, Timestamp.valueOf(cutoff));
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int deleteExpiredTokens() {
    String sql = "delete from tokens where last_accessed < DATE_SUB(NOW(), INTERVAL ? MINUTE)";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
         PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setLong(1, TOKEN_IDLE_MINUTES);
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public int deleteAllTokens() {
    String sql = "delete from tokens";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
         PreparedStatement statement = conn.prepareStatement(sql)) {
      return statement.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private Timestamp getCurrentTimestamp() {
    return Timestamp.valueOf(LocalDateTime.now());
  }

  private Timestamp getCutoffTimestamp() {
    return Timestamp.valueOf(LocalDateTime.now().minusMinutes(TOKEN_IDLE_MINUTES));
  }

  public double[] getBalanceInfo(String username) {
    String sql = "select balance, locked_balance from user where username = ?";
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
         PreparedStatement statement = conn.prepareStatement(sql)) {
      statement.setString(1, username);
      try (ResultSet rs = statement.executeQuery()) {
        if (!rs.next()) {
          return null;
        }
        double balance = rs.getDouble("balance");
        double lockedBalance = rs.getDouble("locked_balance");
        return new double[] {balance, lockedBalance, balance - lockedBalance};
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public double[] topupBalance(String username, double amount) {
    String sqlUpdate = "update user set balance = balance + ? where username = ?";
    String sqlSelect = "select balance, locked_balance from user where username = ?";
    DBProperty dbProperty = DBProperty.getInstance();

    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      conn.setAutoCommit(false);
      try (PreparedStatement update = conn.prepareStatement(sqlUpdate);
          PreparedStatement select = conn.prepareStatement(sqlSelect)) {
        update.setDouble(1, amount);
        update.setString(2, username);
        if (update.executeUpdate() != 1) {
          conn.rollback();
          return null;
        }

        select.setString(1, username);
        try (ResultSet rs = select.executeQuery()) {
          if (!rs.next()) {
            conn.rollback();
            return null;
          }
          double balance = rs.getDouble("balance");
          double lockedBalance = rs.getDouble("locked_balance");
          conn.commit();
          return new double[] {balance, lockedBalance, balance - lockedBalance};
        }
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
