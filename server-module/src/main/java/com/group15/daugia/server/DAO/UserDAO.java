package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.*;
import java.util.UUID;

// Kiểm tra tài khoản và set token nếu thành công
public class UserDAO {
  private final String token = UUID.randomUUID().toString();

  public String checkLogin(String username, String password) {
    DBProperty dbProperty = DBProperty.getInstance();
    String sqlCheckCommand = "select id from user where username = ? and password = ?";
    String sqlSetTokenCommand = "insert into token (id, token) values (?, ?)";

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
            return token;
          }
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
