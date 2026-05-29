package com.group15.daugia.server;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigration {
  private SchemaMigration() {}

  public static void ensureSchema() {
    DBProperty dbProperty = DBProperty.getInstance();
    try (Connection conn =
            DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {
      if (!hasColumn(conn, "user", "is_banned")) {
        try (Statement statement = conn.createStatement()) {
          statement.executeUpdate(
              "ALTER TABLE `user` ADD COLUMN `is_banned` BOOLEAN NOT NULL DEFAULT false");
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean hasColumn(Connection conn, String tableName, String columnName)
      throws SQLException {
    String sql =
        "SELECT 1 FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
    try (var preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, tableName);
      preparedStatement.setString(2, columnName);
      try (ResultSet resultSet = preparedStatement.executeQuery()) {
        return resultSet.next();
      }
    }
  }
}
