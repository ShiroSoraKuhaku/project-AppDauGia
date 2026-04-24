package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.*;
import java.util.UUID;

public class UserDAO {
  private final String token = UUID.randomUUID().toString();
  DBProperty dbProperty = DBProperty.getInstance();
  private static UserDAO instance;

  private UserDAO() {}

  public static UserDAO getUserDao() {
    if (instance == null) {
      instance = new UserDAO();
    }
    return instance;
  }

  public String[] checkLogin(String username, String password) {
    String sqlCheckCommand = "select id from user where username = ? and password = ?";
    String sqlSetTokenCommand = "insert into tokens (id, token) values (?, ?)";

    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      PreparedStatement statement1 = conn.prepareStatement(sqlCheckCommand);
      statement1.setString(1, username);
      statement1.setString(2, password);

      try (ResultSet resultSet = statement1.executeQuery()) {
        // Set token
        if (resultSet.next()) {
          int id = resultSet.getInt("id");

          PreparedStatement statement2 = conn.prepareStatement(sqlSetTokenCommand);
          statement2.setInt(1, id);
          statement2.setString(2, token);
          if (statement2.executeUpdate() == 1) {
            //            return token;
            // TODO: sửa cái này để nó trả về string
            return new String[] {String.valueOf(id), token};
          }
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public String removeLogin(String id, String token) {
    String sqlDeleteToken = "delete from tokens where id = ? and token = ?";
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      PreparedStatement statement1 = conn.prepareStatement(sqlDeleteToken);
      statement1.setString(1, id);
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
}
