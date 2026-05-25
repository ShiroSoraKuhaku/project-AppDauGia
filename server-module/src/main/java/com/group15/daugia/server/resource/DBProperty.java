package com.group15.daugia.server.resource;

public class DBProperty {
  private static DBProperty instance;

  private String host;
  private String port;
  private String dbName;
  private String username;
  private String password;

  private DBProperty() {
    host = resolve("DB_HOST", "localhost");
    port = resolve("DB_PORT", "3306");
    dbName = resolve("DB_NAME", "daugiadb");
    username = resolve("DB_USERNAME", "root");
    password = resolve("DB_PASSWORD", "root");
  }

  private static String resolve(String key, String defaultValue) {
    String sysProp = System.getProperty(key);
    if (sysProp != null && !sysProp.isBlank()) return sysProp;
    String envVar = System.getenv(key);
    if (envVar != null && !envVar.isBlank()) return envVar;
    return defaultValue;
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
