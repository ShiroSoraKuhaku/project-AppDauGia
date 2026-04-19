package com.group15.daugia.server.resource;

public class DBProperty {
  private static DBProperty instance;

  private String host;
  private String port;
  private String dbName;
  private String username;
  private String password;

  private DBProperty() {
    host = System.getenv().getOrDefault("DB_HOST", "localhost");
    port = System.getenv().getOrDefault("DB_PORT", "3306");
    dbName = System.getenv().getOrDefault("DB_NAME", "DauGiaDB");
    username = System.getenv().getOrDefault("DB_USERNAME", "root");
    password = System.getenv().getOrDefault("DB_PASSWORD", "root");
  }

  public String getDBUrl() {
    return String.format("jdbc:mysql://%s:%s/%s", host, port, dbName);
  }

  public static DBProperty getInstance() {
    if (instance == null) {
      instance = new DBProperty();
    }
    return instance;
  }

  public String getHost() {
    return host;
  }

  public String getDbName() {
    return dbName;
  }

  public String getPassword() {
    return password;
  }

  public String getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }
}
