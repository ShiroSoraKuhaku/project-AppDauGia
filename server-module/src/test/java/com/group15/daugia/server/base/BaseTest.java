package com.group15.daugia.server.base;

import com.google.gson.Gson;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.AuctionServer;
import com.group15.daugia.server.resource.DBProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * BaseTest: khởi động MySQLContainer + AuctionServer dùng chung cho toàn bộ IT. Subclass có thể gọi
 * sendCommand(), openWatchSession() và các JDBC helper.
 */
@Testcontainers
public abstract class BaseTest {

  protected static final Gson GSON = new Gson();
  protected static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Container
  protected static final MySQLContainer<?> mysql =
      new MySQLContainer<>("mysql:8")
          .withDatabaseName("daugiadb")
          .withUsername("root")
          .withPassword("root")
          .withInitScript("init.sql");

  protected static AuctionServer server;
  private static Thread serverThread;
  protected static int serverPort;

  @BeforeAll
  static void startInfrastructure() throws Exception {
    // Testcontainers đã start container trước @BeforeAll (vì @Container static)
    // Set system properties để DBProperty singleton pick up đúng port
    resetDbPropertySingleton();
    System.setProperty("DB_HOST", mysql.getHost());
    System.setProperty("DB_PORT", String.valueOf(mysql.getMappedPort(3306)));
    System.setProperty("DB_NAME", "daugiadb");
    System.setProperty("DB_USERNAME", "root");
    System.setProperty("DB_PASSWORD", "root");
    resetDbPropertySingleton(); // re-init after setting props

    // Khởi động server trên port ngẫu nhiên
    server = new AuctionServer(0);
    serverPort = server.gerPort();
    serverThread = new Thread(() -> server.start(), "test-server");
    serverThread.setDaemon(true);
    serverThread.start();
    Thread.sleep(300); // chờ server sẵn sàng accept

    // Bootstrap AuctionClock
    AuctionClock.getInstance().bootstrap();
  }

  @AfterAll
  static void stopInfrastructure() throws Exception {
    AuctionClock.getInstance().resetForTests();
    if (server != null) server.stop();
  }

  // -----------------------------------------------------------------------
  // Helper: reset DBProperty singleton để đọc lại system properties
  // -----------------------------------------------------------------------
  protected static void resetDbPropertySingleton() throws Exception {
    Field f = DBProperty.class.getDeclaredField("instance");
    f.setAccessible(true);
    f.set(null, null);
  }

  // -----------------------------------------------------------------------
  // Helper: gửi command + json, nhận 1 dòng response
  // -----------------------------------------------------------------------
  protected static String sendCommand(String command, String json) throws IOException {
    try (Socket s = new Socket("127.0.0.1", serverPort);
        PrintWriter out = new PrintWriter(s.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
      out.println(command);
      out.println(json);
      return in.readLine();
    }
  }

  // -----------------------------------------------------------------------
  // Helper: mở socket watch, trả WatchSession để đọc push events
  // -----------------------------------------------------------------------
  protected static WatchSession openWatchSession(String json) throws IOException {
    Socket s = new Socket("127.0.0.1", serverPort);
    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
    out.println("WATCH-AUCTION");
    out.println(json);
    String ack = in.readLine(); // ACK
    return new WatchSession(s, out, in, ack);
  }

  public static class WatchSession implements Closeable {
    public final Socket socket;
    public final PrintWriter out;
    public final BufferedReader in;
    public final String ack;

    WatchSession(Socket socket, PrintWriter out, BufferedReader in, String ack) {
      this.socket = socket;
      this.out = out;
      this.in = in;
      this.ack = ack;
    }

    public String readEvent(int timeoutMs) throws IOException {
      socket.setSoTimeout(timeoutMs);
      try {
        return in.readLine();
      } catch (java.net.SocketTimeoutException e) {
        return null;
      }
    }

    public void unwatch(String json) throws IOException {
      out.println("UNWATCH-AUCTION");
      out.println(json);
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }
  }

  // -----------------------------------------------------------------------
  // JDBC helpers – seed / cleanup
  // -----------------------------------------------------------------------
  protected static Connection getJdbcConn() throws Exception {
    String url =
        String.format("jdbc:mysql://%s:%d/daugiadb", mysql.getHost(), mysql.getMappedPort(3306));
    return DriverManager.getConnection(url, "root", "root");
  }

  protected static void execSql(String sql) throws Exception {
    try (Connection c = getJdbcConn();
        Statement st = c.createStatement()) {
      st.execute(sql);
    }
  }

  protected static void cleanAll() throws Exception {
    try (Connection c = getJdbcConn();
        Statement st = c.createStatement()) {
      st.execute("SET FOREIGN_KEY_CHECKS=0");
      for (String t :
          new String[] {
            "auction_auto_bids", "auction_bids", "auctions", "tokens", "items", "bids", "user"
          }) {
        st.execute("DELETE FROM `" + t + "`");
      }
      st.execute("SET FOREIGN_KEY_CHECKS=1");
    }
  }

  /** Seed một user và trả token (insert token trực tiếp vào DB). */
  protected static String seedUserWithToken(String username, String password, String token)
      throws Exception {
    execSql(
        "INSERT IGNORE INTO `user` (username, password) VALUES ('"
            + username
            + "','"
            + password
            + "')");
    execSql(
        "INSERT IGNORE INTO tokens (username, token) VALUES ('" + username + "','" + token + "')");
    return token;
  }

  /** Seed một item và trả id. */
  protected static int seedItem(String sellerUsername, String name, double price) throws Exception {
    try (Connection c = getJdbcConn();
        var ps =
            c.prepareStatement(
                "INSERT INTO items (seller_username, name, price) VALUES (?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setString(1, sellerUsername);
      ps.setString(2, name);
      ps.setDouble(3, price);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  /** Seed một auction ACTIVE với end_time = now + offsetSeconds. */
  protected static int seedActiveAuction(
      String title, int itemId, double startPrice, long offsetSeconds) throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String start = now.minusSeconds(60).format(FMT);
    String end = now.plusSeconds(offsetSeconds).format(FMT);
    try (Connection c = getJdbcConn();
        var ps =
            c.prepareStatement(
                "INSERT INTO auctions (item_id, title, status, start_price, cur_price, start_time, end_time) VALUES (?,?,'ACTIVE',?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setString(2, title);
      ps.setDouble(3, startPrice);
      ps.setDouble(4, startPrice);
      ps.setString(5, start);
      ps.setString(6, end);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  /** Seed một auction SCHEDULED với start_time = now + startOffsetSecs. */
  protected static int seedScheduledAuction(
      String title, int itemId, double startPrice, long startOffsetSecs, long endOffsetSecs)
      throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String start = now.plusSeconds(startOffsetSecs).format(FMT);
    String end = now.plusSeconds(endOffsetSecs).format(FMT);
    try (Connection c = getJdbcConn();
        var ps =
            c.prepareStatement(
                "INSERT INTO auctions (item_id, title, status, start_price, cur_price, start_time, end_time) VALUES (?,?,'SCHEDULED',?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setString(2, title);
      ps.setDouble(3, startPrice);
      ps.setDouble(4, startPrice);
      ps.setString(5, start);
      ps.setString(6, end);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }

  /** Seed auction ENDED. */
  protected static int seedEndedAuction(String title, int itemId, double startPrice)
      throws Exception {
    LocalDateTime now = LocalDateTime.now();
    String start = now.minusSeconds(120).format(FMT);
    String end = now.minusSeconds(10).format(FMT);
    try (Connection c = getJdbcConn();
        var ps =
            c.prepareStatement(
                "INSERT INTO auctions (item_id, title, status, start_price, cur_price, start_time, end_time) VALUES (?,?,'ENDED',?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setString(2, title);
      ps.setDouble(3, startPrice);
      ps.setDouble(4, startPrice);
      ps.setString(5, start);
      ps.setString(6, end);
      ps.executeUpdate();
      try (var rs = ps.getGeneratedKeys()) {
        rs.next();
        return rs.getInt(1);
      }
    }
  }
}
