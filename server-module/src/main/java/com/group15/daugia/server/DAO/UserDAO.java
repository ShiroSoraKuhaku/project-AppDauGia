package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.*;
import java.util.UUID;

public class UserDAO {
  private static UserDAO instance;

  private UserDAO() {}

  public static UserDAO getUserDao() {
    if (instance == null) {
      instance = new UserDAO();
    }
    return instance;
  }

  public String[] checkLogin(String username, String password) {
    String sqlCheckCommand = "select * from user where username = ? and password = ?";
    String sqlSetTokenCommand =
        "insert into tokens (username, token) values (?, ?) "
            + "on duplicate key update token = values(token)";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
        DriverManager.getConnection(
            dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      PreparedStatement statement1 = conn.prepareStatement(sqlCheckCommand);
      statement1.setString(1, username);
      statement1.setString(2, password);

      try (ResultSet resultSet = statement1.executeQuery()) {
        String token = UUID.randomUUID().toString();
        // Set token
        if (!resultSet.next()) {
          return null;
        }
        PreparedStatement statement2 = conn.prepareStatement(sqlSetTokenCommand);
        statement2.setString(1, username);
        statement2.setString(2, token);
        statement2.executeUpdate();
        // TODO: sửa cái này để nó trả về string
        return new String[] {username, token};
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
    String sql = "select username from tokens where token = ?";

    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                    dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword()
            );
         PreparedStatement statement = conn.prepareStatement(sql)){
      statement.setString(1, token);

      try (ResultSet resultSet = statement.executeQuery()){
        if (resultSet.next()){
          return resultSet.getString("username");
        }
        return null;
      }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }
}
