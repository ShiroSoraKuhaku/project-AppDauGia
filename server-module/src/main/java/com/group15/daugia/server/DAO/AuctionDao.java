package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO cho toàn bộ nghiệp vụ đấu giá. Singleton, mỗi method tự mở/đóng Connection theo pattern của
 * ItemDAO/UserDAO.
 */
public class AuctionDao {

  private static AuctionDao instance;
  private final DBProperty dbProperty = DBProperty.getInstance();

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final double BID_INCREMENT = 1.0;

  private AuctionDao() {}

  public static AuctionDao getInstance() {
    if (instance == null) {
      instance = new AuctionDao();
    }
    return instance;
  }

  // -----------------------------------------------------------------------
  // 1. findAuctionById
  // -----------------------------------------------------------------------
  public JSONAuctionTemp findAuctionById(int auctionId) {
    String sql =
        "SELECT id, item_id, title, status, start_price, cur_price, cur_leader, "
            + "start_time, end_time, version FROM auctions WHERE id = ?";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return mapRow(rs);
        }
        return null;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 2. loadUpcomingAndActiveAuctions – dùng cho bootstrap AuctionClock
  // -----------------------------------------------------------------------
  public List<JSONAuctionTemp> loadUpcomingAndActiveAuctions(LocalDateTime now) {
    String sql =
        "SELECT id, item_id, title, status, start_price, cur_price, cur_leader, "
            + "start_time, end_time, version FROM auctions "
            + "WHERE status IN ('SCHEDULED','ACTIVE') AND end_time > ? "
            + "ORDER BY start_time ASC";
    List<JSONAuctionTemp> list = new ArrayList<>();
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, now.format(FMT));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          list.add(mapRow(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return list;
  }

  // -----------------------------------------------------------------------
  // 3. markAuctionStarted
  // -----------------------------------------------------------------------
  public boolean markAuctionStarted(int auctionId, LocalDateTime now) {
    String sql =
        "UPDATE auctions SET status = 'ACTIVE', version = version + 1 "
            + "WHERE id = ? AND status = 'SCHEDULED'";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      return ps.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 4. markAuctionEnded
  // -----------------------------------------------------------------------
  public boolean markAuctionEnded(int auctionId, LocalDateTime now) {
    String sql =
        "UPDATE auctions SET status = 'ENDED', version = version + 1 "
            + "WHERE id = ? AND status = 'ACTIVE'";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      return ps.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 5. placeBidTransactional
  //    Trả về true nếu đặt giá thành công, false nếu điều kiện không thoả.
  // -----------------------------------------------------------------------
  public boolean placeBidTransactional(
      int auctionId,
      String bidderUsername,
      double bidAmount,
      int expectedVersion,
      boolean usePessimistic) {
    if (usePessimistic) {
      return placeBidPessimistic(auctionId, bidderUsername, bidAmount);
    }
    return placeBidOptimisticWithRetry(auctionId, bidderUsername, bidAmount, expectedVersion);
  }

  private boolean placeBidOptimisticWithRetry(
      int auctionId, String bidderUsername, double bidAmount, int expectedVersion) {
    String sqlCheck = "SELECT status, end_time, cur_price, version FROM auctions WHERE id = ?";
    String sqlUpdate =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, "
            + "version = version + 1 WHERE id = ? AND version = ?";
    String sqlBid =
        "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) " + "VALUES (?, ?, ?)";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        for (int attempt = 1; attempt <= 2; attempt++) {
          int version;
          // Kiểm tra điều kiện
          try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, auctionId);
            try (ResultSet rs = psCheck.executeQuery()) {
              if (!rs.next()) {
                conn.rollback();
                return false;
              }
              String status = rs.getString("status");
              String endTime = rs.getString("end_time");
              double curPrice = rs.getDouble("cur_price");
              version = rs.getInt("version");

              LocalDateTime end = LocalDateTime.parse(endTime, FMT);
              if (!"ACTIVE".equals(status)
                  || LocalDateTime.now().isAfter(end)
                  || bidAmount <= curPrice) {
                conn.rollback();
                return false;
              }
              if (attempt == 1 && version != expectedVersion) {
                conn.rollback();
                continue;
              }
            }
          }

          int versionToUse = (attempt == 1) ? expectedVersion : version;
          // Cập nhật auction
          try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
            psUpdate.setDouble(1, bidAmount);
            psUpdate.setString(2, bidderUsername);
            psUpdate.setInt(3, auctionId);
            psUpdate.setInt(4, versionToUse);
            if (psUpdate.executeUpdate() != 1) {
              conn.rollback();
              if (attempt == 1) {
                continue;
              }
              return false;
            }
          }

          // Ghi lịch sử bid
          try (PreparedStatement psBid = conn.prepareStatement(sqlBid)) {
            psBid.setInt(1, auctionId);
            psBid.setString(2, bidderUsername);
            psBid.setDouble(3, bidAmount);
            psBid.executeUpdate();
          }

          conn.commit();
          return true;
        }
        return false;
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean placeBidPessimistic(int auctionId, String bidderUsername, double bidAmount) {
    String sqlCheck =
        "SELECT status, end_time, cur_price FROM auctions WHERE id = ? FOR UPDATE";
    String sqlUpdate =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, "
            + "version = version + 1 WHERE id = ?";
    String sqlBid =
        "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) " + "VALUES (?, ?, ?)";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        // Kiểm tra điều kiện
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
          psCheck.setInt(1, auctionId);
          try (ResultSet rs = psCheck.executeQuery()) {
            if (!rs.next()) {
              conn.rollback();
              return false;
            }
            String status = rs.getString("status");
            String endTime = rs.getString("end_time");
            double curPrice = rs.getDouble("cur_price");

            LocalDateTime end = LocalDateTime.parse(endTime, FMT);
            if (!"ACTIVE".equals(status)
                || LocalDateTime.now().isAfter(end)
                || bidAmount <= curPrice) {
              conn.rollback();
              return false;
            }
          }
        }

        // Cập nhật auction
        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
          psUpdate.setDouble(1, bidAmount);
          psUpdate.setString(2, bidderUsername);
          psUpdate.setInt(3, auctionId);
          if (psUpdate.executeUpdate() != 1) {
            conn.rollback();
            return false;
          }
        }

        // Ghi lịch sử bid
        try (PreparedStatement psBid = conn.prepareStatement(sqlBid)) {
          psBid.setInt(1, auctionId);
          psBid.setString(2, bidderUsername);
          psBid.setDouble(3, bidAmount);
          psBid.executeUpdate();
        }

        // Nếu có auto bid, xử lý tiếp để phản hồi tự động từ bid tay
        processAutoBids(conn, auctionId);

        conn.commit();
        return true;
      } catch (SQLException ex) {
        conn.rollback();
        throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 6. extendAuctionEndTime – gia hạn anti-sniping
  // -----------------------------------------------------------------------
  public boolean extendAuctionEndTime(
      int auctionId, LocalDateTime newEndTime, int expectedVersion) {
    String sql =
        "UPDATE auctions SET end_time = ?, version = version + 1 "
            + "WHERE id = ? AND version = ? AND status = 'ACTIVE'";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newEndTime.format(FMT));
      ps.setInt(2, auctionId);
      ps.setInt(3, expectedVersion);
      return ps.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 7. getAuctionSnapshot – lấy trạng thái mới nhất để broadcast
  // -----------------------------------------------------------------------
  public JSONAuctionTemp getAuctionSnapshot(int auctionId) {
    return findAuctionById(auctionId);
  }

  // -----------------------------------------------------------------------
  // Helper: map ResultSet row -> JSONAuctionTemp
  // -----------------------------------------------------------------------
  private JSONAuctionTemp mapRow(ResultSet rs) throws SQLException {
    JSONAuctionTemp a = new JSONAuctionTemp();
    a.setAuctionId(rs.getInt("id"));
    a.setItemId(rs.getInt("item_id"));
    a.setTitle(rs.getString("title"));
    a.setStatus(rs.getString("status"));
    a.setStartPrice(rs.getDouble("start_price"));
    a.setCurPrice(rs.getDouble("cur_price"));
    a.setCurLeader(rs.getString("cur_leader"));
    a.setStartTime(rs.getString("start_time"));
    a.setEndTime(rs.getString("end_time"));
    a.setVersion(rs.getInt("version"));

    // Tính secondsRemaining nếu ACTIVE
    if ("ACTIVE".equals(a.getStatus()) && a.getEndTime() != null) {
      try {
        LocalDateTime end = LocalDateTime.parse(a.getEndTime(), FMT);
        long secs = java.time.Duration.between(LocalDateTime.now(), end).getSeconds();
        a.setSecondsRemaining(Math.max(0, secs));
      } catch (Exception ignored) {
      }
    }
    return a;
  }

  private Connection getConn() throws SQLException {
    return DriverManager.getConnection(
        dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
  }


  public String setAutoBid(int auctionId, String bidderUsername, double maxAmount){
    if (auctionId <= 0
        || bidderUsername == null
        || bidderUsername.isBlank()
        || !Double.isFinite(maxAmount)
        || maxAmount <= 0) {
      return "INVALID_INPUT";
    }

    String checkSql = "SELECT status, end_time, cur_price FROM auctions WHERE id = ? FOR UPDATE";

    String upsertSql = "INSERT INTO auction_auto_bids (auction_id, bidder_username, max_amount, active) " +
            "VALUES (?, ?, ?, true) " + "ON DUPLICATE KEY UPDATE max_amount = VALUES(max_amount), active = true";

    try (Connection conn = getConn()){
      conn.setAutoCommit(false);

      try {
        double curPrice;

        try (PreparedStatement ps = conn.prepareStatement(checkSql)){
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()){
              conn.rollback();
              return "AUCTION_NOT_FOUND";
            }

            LocalDateTime end = LocalDateTime.parse(rs.getString("end_time"), FMT);
            if (!"ACTIVE".equals(rs.getString("status")) || LocalDateTime.now().isAfter(end)){
              conn.rollback();
              return "AUCTION_NOT_ACTIVE";
            }

            curPrice = rs.getDouble("cur_price");
          }
        }

        if (maxAmount <= curPrice){
          conn.rollback();
          return "PRICE_TOO_LOW";
        }

        try (PreparedStatement ps = conn.prepareStatement(upsertSql)){
          ps.setInt(1, auctionId);
          ps.setString(2, bidderUsername);
          ps.setDouble(3, maxAmount);
          ps.executeUpdate();
        }

        processAutoBids(conn, auctionId);

        conn.commit();
        return "OK";
      } catch (Exception e) {
        conn.rollback();
        throw new RuntimeException(e);
      }finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
  }

  private static class CurrentAuction{
    double curPrice;
    String curLeader;

    public CurrentAuction(double curPrice, String curLeader) {
      this.curPrice = curPrice;
      this.curLeader = curLeader;
    }
  }

  private static class AutoBidCandidate {
    String username;
    double maxAmount;

    AutoBidCandidate(String username, double maxAmount) {
      this.username = username;
      this.maxAmount = maxAmount;
    }
  }

  private CurrentAuction getCurrentAuction(Connection conn, int auctionId) throws SQLException {
    String sql = "SELECT cur_price, cur_leader FROM auctions WHERE id = ? FOR UPDATE";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) {
          throw new SQLException("Auction not found: " + auctionId);
        }
        return new CurrentAuction(rs.getDouble("cur_price"), rs.getString("cur_leader"));
      }
    }
  }

  private List<AutoBidCandidate> findTopAutoBidCandidates(
          Connection conn, int auctionId, double minAmount) throws SQLException {
    String sql =
            "SELECT bidder_username, max_amount FROM auction_auto_bids "
                    + "WHERE auction_id = ? "
                    + "AND active = true "
                    + "AND max_amount >= ? "
                    + "ORDER BY max_amount DESC, updated_at ASC "
                    + "LIMIT 2";
    List<AutoBidCandidate> candidates = new ArrayList<>();

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setDouble(2, minAmount);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          candidates.add(
              new AutoBidCandidate(rs.getString("bidder_username"), rs.getDouble("max_amount")));
        }
      }
    }
    return candidates;
  }

  private void updateAuctionBid(
          Connection conn, int auctionId, String bidderUsername, double amount) throws SQLException {
    String sql =
            "UPDATE auctions SET cur_price = ?, cur_leader = ?, version = version + 1 WHERE id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, amount);
      ps.setString(2, bidderUsername);
      ps.setInt(3, auctionId);
      if (ps.executeUpdate() != 1) {
        throw new SQLException("Failed to update auction bid: " + auctionId);
      }
    }
  }

  private void insertAuctionBid(
          Connection conn, int auctionId, String bidderUsername, double amount) throws SQLException {
    String sql =
            "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) VALUES (?, ?, ?)";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setString(2, bidderUsername);
      ps.setDouble(3, amount);
      ps.executeUpdate();
    }
  }


  private void processAutoBids(Connection conn, int auctionId) throws SQLException{
    CurrentAuction current = getCurrentAuction(conn, auctionId);
    double minNextBid = current.curPrice + BID_INCREMENT;
    List<AutoBidCandidate> candidates = findTopAutoBidCandidates(conn, auctionId, minNextBid);

    if (candidates.isEmpty()) {
      return;
    }

    AutoBidCandidate winner = candidates.get(0);
    if (winner.username.equals(current.curLeader) && candidates.size() == 1) {
      return;
    }

    double secondLimit = candidates.size() > 1 ? candidates.get(1).maxAmount : current.curPrice;
    double targetAmount = Math.max(minNextBid, secondLimit + BID_INCREMENT);
    double bidAmount = Math.min(winner.maxAmount, targetAmount);

    if (bidAmount <= current.curPrice) {
      return;
    }

    updateAuctionBid(conn, auctionId, winner.username, bidAmount);
    insertAuctionBid(conn, auctionId, winner.username, bidAmount);
  }
}
