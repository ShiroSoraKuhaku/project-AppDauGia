package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;
import com.group15.daugia.shared.JSON.JSONAuctionTemp;
import com.group15.daugia.shared.JSON.JSONBidHistoryTemp;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuctionDao {

  private static AuctionDao instance;

  private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final double DEFAULT_BID_STEP = 1.0;

  private AuctionDao() {}

  public static AuctionDao getInstance() {
    if (instance == null) {
      instance = new AuctionDao();
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

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime parsedEnd = parseDateTime(endTime);
    if (parsedEnd != null && !now.isBefore(parsedEnd)) {
      return "ENDED";
    }

    LocalDateTime parsedStart = parseDateTime(startTime);
    if (parsedStart != null && !now.isBefore(parsedStart)) {
      return "ACTIVE";
    }

    return "SCHEDULED";
  }

  private LocalDateTime parseDateTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDateTime.parse(value, FMT);
    } catch (Exception ignored) {
      return null;
    }
  }

  // -----------------------------------------------------------------------
  // createAuction
  // -----------------------------------------------------------------------
  public JSONAuctionTemp createAuction(
      int itemId, String title, double startPrice, LocalDateTime startTime, LocalDateTime endTime) {
    String status = startTime.isAfter(LocalDateTime.now()) ? "SCHEDULED" : "ACTIVE";
    String sql =
        "INSERT INTO auctions (item_id, title, status, start_price, cur_price, start_time, end_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      ps.setInt(1, itemId);
      ps.setString(2, title);
      ps.setString(3, status);
      ps.setDouble(4, startPrice);
      ps.setDouble(5, startPrice);
      ps.setString(6, startTime.format(FMT));
      ps.setString(7, endTime.format(FMT));

      if (ps.executeUpdate() != 1) {
        return null;
      }

      try (ResultSet keys = ps.getGeneratedKeys()) {
        if (!keys.next()) {
          return null;
        }
        return findAuctionById(keys.getInt(1));
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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

  public JSONAuctionTemp findAuctionByItemId(int itemId) {
    String sql =
        "SELECT id, item_id, title, status, start_price, cur_price, cur_leader, "
            + "start_time, end_time, version FROM auctions WHERE item_id = ?";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, itemId);
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
  // 5. placeBidTransactional – kèm check available balance + hold logic
  // -----------------------------------------------------------------------
  /**
   * Đặt bid. Trả về AutoBidResult:
   * - null nếu thất bại
   * - AutoBidResult với username=null nếu thành công nhưng không có auto-bid sau đó
   * - AutoBidResult với username != null nếu có auto-bid xảy ra
   */
  public AutoBidResult placeBidTransactional(
      int auctionId,
      String bidderUsername,
      double bidAmount,
      int expectedVersion,
      boolean usePessimistic) {
    if (usePessimistic) {
      return placeBidPessimistic(auctionId, bidderUsername, bidAmount);
    }
    boolean ok = placeBidOptimisticWithRetry(auctionId, bidderUsername, bidAmount, expectedVersion);
    return ok ? new AutoBidResult(null, 0) : null;
  }

  private boolean placeBidOptimisticWithRetry(
      int auctionId, String bidderUsername, double bidAmount, int expectedVersion) {
    String sqlCheck =
        "SELECT status, start_time, end_time, cur_price, cur_leader, version FROM auctions WHERE id = ?";
    String sqlUpdate =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, "
            + "version = version + 1 WHERE id = ? AND version = ?";
    String sqlBid =
        "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) VALUES (?, ?, ?)";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        for (int attempt = 1; attempt <= 2; attempt++) {
          int version;
          String prevLeader;
          try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, auctionId);
            try (ResultSet rs = psCheck.executeQuery()) {
              if (!rs.next()) { conn.rollback(); return false; }
              String status = rs.getString("status");
              String startTime = rs.getString("start_time");
              String endTime = rs.getString("end_time");
              double curPrice = rs.getDouble("cur_price");
              version = rs.getInt("version");
              prevLeader = rs.getString("cur_leader");

              String normalizedStatus = normalizeStatus(status, startTime, endTime);
              if (!"ACTIVE".equals(normalizedStatus) || bidAmount <= curPrice) {
                conn.rollback(); return false;
              }
              if (attempt == 1 && version != expectedVersion) { conn.rollback(); continue; }
            }
          }

          // Check available balance
          if (!hasEnoughAvailableBalance(conn, bidderUsername, auctionId, bidAmount)) {
            conn.rollback(); return false;
          }

          int versionToUse = (attempt == 1) ? expectedVersion : version;
          try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
            psUpdate.setDouble(1, bidAmount);
            psUpdate.setString(2, bidderUsername);
            psUpdate.setInt(3, auctionId);
            psUpdate.setInt(4, versionToUse);
            if (psUpdate.executeUpdate() != 1) {
              conn.rollback();
              if (attempt == 1) continue;
              return false;
            }
          }

          try (PreparedStatement psBid = conn.prepareStatement(sqlBid)) {
            psBid.setInt(1, auctionId); psBid.setString(2, bidderUsername);
            psBid.setDouble(3, bidAmount); psBid.executeUpdate();
          }

          // Cập nhật holds: lock bidder mới, release bidder cũ
          upsertHold(conn, auctionId, bidderUsername, bidAmount);
          if (prevLeader != null && !prevLeader.equals(bidderUsername)) {
            releaseHold(conn, auctionId, prevLeader);
          }

          conn.commit();
          return true;
        }
        return false;
      } catch (SQLException ex) {
        conn.rollback(); throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private AutoBidResult placeBidPessimistic(int auctionId, String bidderUsername, double bidAmount) {
    String sqlCheck =
        "SELECT status, start_time, end_time, cur_price, cur_leader FROM auctions WHERE id = ? FOR UPDATE";
    String sqlUpdate =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, version = version + 1 WHERE id = ?";
    String sqlBid =
        "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) VALUES (?, ?, ?)";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        String prevLeader;
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
          psCheck.setInt(1, auctionId);
          try (ResultSet rs = psCheck.executeQuery()) {
            if (!rs.next()) { conn.rollback(); return null; }
            String status = rs.getString("status");
            String startTime = rs.getString("start_time");
            String endTime = rs.getString("end_time");
            double curPrice = rs.getDouble("cur_price");
            prevLeader = rs.getString("cur_leader");

            String normalizedStatus = normalizeStatus(status, startTime, endTime);
            if (!"ACTIVE".equals(normalizedStatus) || bidAmount <= curPrice) {
              conn.rollback(); return null;
            }
          }
        }

        // Check available balance
        if (!hasEnoughAvailableBalance(conn, bidderUsername, auctionId, bidAmount)) {
          conn.rollback(); return null;
        }

        try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
          psUpdate.setDouble(1, bidAmount); psUpdate.setString(2, bidderUsername);
          psUpdate.setInt(3, auctionId);
          if (psUpdate.executeUpdate() != 1) { conn.rollback(); return null; }
        }

        try (PreparedStatement psBid = conn.prepareStatement(sqlBid)) {
          psBid.setInt(1, auctionId); psBid.setString(2, bidderUsername);
          psBid.setDouble(3, bidAmount); psBid.executeUpdate();
        }

        upsertHold(conn, auctionId, bidderUsername, bidAmount);
        if (prevLeader != null && !prevLeader.equals(bidderUsername)) {
          releaseHold(conn, auctionId, prevLeader);
        }

        AutoBidResult autoBid = processAutoBids(conn, auctionId);

        conn.commit();
        // Trả về null-sentinel nếu thành công nhưng không có auto-bid: dùng MANUAL_BID_OK sentinel
        return autoBid != null ? autoBid : new AutoBidResult(null, 0); // null username = không có auto-bid
      } catch (SQLException ex) {
        conn.rollback(); throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 6. extendAuctionEndTime
  // -----------------------------------------------------------------------
  public boolean extendAuctionEndTime(
      int auctionId, LocalDateTime newEndTime, int expectedVersion) {
    String sql =
        "UPDATE auctions SET end_time = ?, version = version + 1 "
            + "WHERE id = ? AND version = ? AND status IN ('SCHEDULED','ACTIVE') "
            + "AND start_time <= ? AND end_time > ?";
    try (Connection conn = getConn();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, newEndTime.format(FMT));
      ps.setInt(2, auctionId);
      ps.setInt(3, expectedVersion);
      String now = LocalDateTime.now().format(FMT);
      ps.setString(4, now);
      ps.setString(5, now);
      return ps.executeUpdate() == 1;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 7. getAuctionSnapshot
  // -----------------------------------------------------------------------
  public JSONAuctionTemp getAuctionSnapshot(int auctionId) {
    return findAuctionById(auctionId);
  }

  public List<JSONBidHistoryTemp.BidRecord> getRecentBids(int auctionId, int limit) {
    String sql =
        "SELECT bidder_username, bid_amount, created_at FROM auction_bids "
            + "WHERE auction_id = ? ORDER BY created_at DESC, id DESC LIMIT ?";
    List<JSONBidHistoryTemp.BidRecord> bids = new ArrayList<>();
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      ps.setInt(2, Math.max(1, limit));
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          JSONBidHistoryTemp.BidRecord bid = new JSONBidHistoryTemp.BidRecord();
          bid.setBidderUsername(rs.getString("bidder_username"));
          bid.setBidAmount(rs.getDouble("bid_amount"));
          Timestamp createdAt = rs.getTimestamp("created_at");
          bid.setCreatedAt(createdAt != null ? createdAt.toLocalDateTime().format(FMT) : null);
          bids.add(bid);
        }
      }
      return bids;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 8. settleAuction – gọi từ AuctionClock.onAuctionEnd
  //    Trừ tiền winner, cộng tiền người bán, release tất cả hold của auction này
  // -----------------------------------------------------------------------
  public void settleAuction(int auctionId) {
    String sqlSnap =
        "SELECT a.cur_leader, a.cur_price, i.seller_username "
            + "FROM auctions a JOIN items i ON i.id = a.item_id WHERE a.id = ?";
    String sqlMarkEnded =
        "UPDATE auctions SET status = 'ENDED', version = version + 1 WHERE id = ? AND status = 'ACTIVE'";
    String sqlDeduct =
        "UPDATE user SET balance = balance - ? WHERE username = ?";
    String sqlCredit =
        "UPDATE user SET balance = balance + ? WHERE username = ?";
    String sqlDecLock =
        "UPDATE user SET locked_balance = GREATEST(0, locked_balance - ?) WHERE username = ?";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        String winner = null;
        String seller = null;
        double finalPrice = 0;

        try (PreparedStatement ps = conn.prepareStatement(sqlSnap)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              winner = rs.getString("cur_leader");
              finalPrice = rs.getDouble("cur_price");
              seller = rs.getString("seller_username");
            }
          }
        }

        if (winner != null) {
          // Lấy hold hiện tại của winner để giải phóng khỏi locked_balance
          double holdAmount = getHoldAmount(conn, auctionId, winner);
          
          // Trừ balance thực của winner
          try (PreparedStatement ps = conn.prepareStatement(sqlDeduct)) {
            ps.setDouble(1, finalPrice);
            ps.setString(2, winner);
            ps.executeUpdate();
          }
          
          // Giảm locked_balance đúng bằng hold
          if (holdAmount > 0) {
            try (PreparedStatement ps = conn.prepareStatement(sqlDecLock)) {
              ps.setDouble(1, holdAmount);
              ps.setString(2, winner);
              ps.executeUpdate();
            }
          }
          
          // Cộng tiền cho người bán
          if (seller != null && !seller.equals(winner)) {
            try (PreparedStatement ps = conn.prepareStatement(sqlCredit)) {
              ps.setDouble(1, finalPrice);
              ps.setString(2, seller);
              ps.executeUpdate();
            }
          }
        }

        // Mark auction as ENDED
        try (PreparedStatement ps = conn.prepareStatement(sqlMarkEnded)) {
          ps.setInt(1, auctionId);
          ps.executeUpdate();
        }

        // Release tất cả hold còn lại (bao gồm những bidder không thắng)
        // và giải phóng locked_balance của họ
        releaseAllHoldsWithUnlock(conn, auctionId);

        conn.commit();
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
  // 9. cancelAuction – seller / admin cancel, release tất cả holds
  //    Trả về: "OK" | "NOT_FOUND" | "ALREADY_ENDED" | "FORBIDDEN"
  // -----------------------------------------------------------------------
  public String cancelAuction(int auctionId, String requesterUsername, boolean isAdmin) {
    String sqlCheck =
        "SELECT a.status, a.start_time, a.end_time, i.seller_username FROM auctions a "
            + "JOIN items i ON i.id = a.item_id WHERE a.id = ? FOR UPDATE";
    String sqlCancel =
        "UPDATE auctions SET status = 'CANCELLED', version = version + 1 WHERE id = ?";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        String status;
        String seller;
        try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { conn.rollback(); return "NOT_FOUND"; }
            status = rs.getString("status");
            String startTime = rs.getString("start_time");
            String endTime = rs.getString("end_time");
            seller = rs.getString("seller_username");

            String normalizedStatus = normalizeStatus(status, startTime, endTime);
            if ("ENDED".equals(normalizedStatus) || "CANCELLED".equals(normalizedStatus)) {
              conn.rollback(); return "ALREADY_ENDED";
            }
          }
        }

        if (!isAdmin && !requesterUsername.equals(seller)) {
          conn.rollback(); return "FORBIDDEN";
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlCancel)) {
          ps.setInt(1, auctionId);
          ps.executeUpdate();
        }

        releaseAllHoldsWithUnlock(conn, auctionId);

        conn.commit();
        return "OK";
      } catch (SQLException ex) {
        conn.rollback(); throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // 10. cancelBid – bidder hủy bid cao nhất của mình
  //     Recompute leader theo plan: chọn bidder có đủ available balance
  //     Trả về: "OK" | "NOT_FOUND" | "AUCTION_NOT_ACTIVE" | "NO_BID"
  // -----------------------------------------------------------------------
  public String cancelBid(int auctionId, String bidderUsername) {
    String sqlCheckAuction =
        "SELECT status, start_price FROM auctions WHERE id = ? FOR UPDATE";
    String sqlGetTopBid =
        "SELECT id, bid_amount FROM auction_bids "
            + "WHERE auction_id = ? AND bidder_username = ? "
            + "ORDER BY bid_amount DESC LIMIT 1";
    String sqlDeleteBid =
        "DELETE FROM auction_bids WHERE id = ?";
    String sqlGetBids =
        "SELECT bidder_username, MAX(bid_amount) AS top_bid FROM auction_bids "
            + "WHERE auction_id = ? GROUP BY bidder_username "
            + "ORDER BY top_bid DESC";
    String sqlUpdateAuction =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, version = version + 1 WHERE id = ?";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        String auctionStatus;
        double startPrice;
        try (PreparedStatement ps = conn.prepareStatement(sqlCheckAuction)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { conn.rollback(); return "NOT_FOUND"; }
            auctionStatus = rs.getString("status");
            startPrice = rs.getDouble("start_price");
          }
        }

        if (!"ACTIVE".equals(auctionStatus)) { conn.rollback(); return "AUCTION_NOT_ACTIVE"; }

        // Lấy bid cao nhất của bidder trong auction
        int topBidId;
        double topBidAmount;
        try (PreparedStatement ps = conn.prepareStatement(sqlGetTopBid)) {
          ps.setInt(1, auctionId); ps.setString(2, bidderUsername);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { conn.rollback(); return "NO_BID"; }
            topBidId = rs.getInt("id");
            topBidAmount = rs.getDouble("bid_amount");
          }
        }

        // Xóa bid đó
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteBid)) {
          ps.setInt(1, topBidId); ps.executeUpdate();
        }

        // Release hold của bidder
        releaseHold(conn, auctionId, bidderUsername);

        // Recompute leader: duyệt các bidder còn lại theo thứ tự bid giảm dần
        String newLeader = null;
        double newPrice = startPrice;

        try (PreparedStatement ps = conn.prepareStatement(sqlGetBids)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String candidateUser = rs.getString("bidder_username");
              double candidateBid = rs.getDouble("top_bid");
              // Kiểm tra available balance (không tính hold auction này vì đã xóa)
              if (hasEnoughAvailableBalance(conn, candidateUser, auctionId, candidateBid)) {
                newLeader = candidateUser;
                newPrice = candidateBid;
                break;
              }
            }
          }
        }

        // Cập nhật auction
        try (PreparedStatement ps = conn.prepareStatement(sqlUpdateAuction)) {
          ps.setDouble(1, newPrice);
          ps.setString(2, newLeader); // null nếu không có ai
          ps.setInt(3, auctionId);
          ps.executeUpdate();
        }

        // Nếu có leader mới, upsert hold cho họ
        if (newLeader != null) {
          upsertHold(conn, auctionId, newLeader, newPrice);
        }

        conn.commit();
        return "OK";
      } catch (SQLException ex) {
        conn.rollback(); throw ex;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
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

    String normalizedStatus = normalizeStatus(a.getStatus(), a.getStartTime(), a.getEndTime());
    a.setStatus(normalizedStatus);

    LocalDateTime now = LocalDateTime.now();
    if ("ACTIVE".equals(normalizedStatus) && a.getEndTime() != null) {
      LocalDateTime end = parseDateTime(a.getEndTime());
      if (end != null) {
        long secs = java.time.Duration.between(now, end).getSeconds();
        a.setSecondsRemaining(Math.max(0, secs));
      }
    }
    if ("SCHEDULED".equals(normalizedStatus) && a.getStartTime() != null) {
      LocalDateTime start = parseDateTime(a.getStartTime());
      if (start != null) {
        long secs = java.time.Duration.between(now, start).getSeconds();
        a.setSecondsToStart(Math.max(0, secs));
      }
    }
    return a;
  }

  private Connection getConn() throws SQLException {
    DBProperty dbProperty = DBProperty.getInstance();
    return DriverManager.getConnection(
        dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword());
  }

  // -----------------------------------------------------------------------
  // Hold helpers
  // -----------------------------------------------------------------------

  /** Upsert hold và tăng locked_balance tương ứng. */
  private void upsertHold(Connection conn, int auctionId, String username, double amount)
      throws SQLException {
    String sqlGetOld =
        "SELECT hold_amount FROM auction_bid_holds WHERE auction_id = ? AND bidder_username = ?";
    String sqlUpsert =
        "INSERT INTO auction_bid_holds (auction_id, bidder_username, hold_amount) VALUES (?,?,?) "
            + "ON DUPLICATE KEY UPDATE hold_amount = VALUES(hold_amount)";
    String sqlLock =
        "UPDATE user SET locked_balance = locked_balance + ? WHERE username = ?";
    String sqlLockAdjust =
        "UPDATE user SET locked_balance = GREATEST(0, locked_balance + ?) WHERE username = ?";

    double oldHold = 0;
    try (PreparedStatement ps = conn.prepareStatement(sqlGetOld)) {
      ps.setInt(1, auctionId); ps.setString(2, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) oldHold = rs.getDouble("hold_amount");
      }
    }

    try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
      ps.setInt(1, auctionId); ps.setString(2, username); ps.setDouble(3, amount);
      ps.executeUpdate();
    }

    double delta = amount - oldHold;
    if (delta != 0) {
      try (PreparedStatement ps = conn.prepareStatement(sqlLockAdjust)) {
        ps.setDouble(1, delta); ps.setString(2, username);
        ps.executeUpdate();
      }
    }
  }

  /** Xóa hold và giảm locked_balance tương ứng. */
  private void releaseHold(Connection conn, int auctionId, String username) throws SQLException {
    double holdAmount = getHoldAmount(conn, auctionId, username);
    if (holdAmount <= 0) return;

    String sqlDelete =
        "DELETE FROM auction_bid_holds WHERE auction_id = ? AND bidder_username = ?";
    String sqlUnlock =
        "UPDATE user SET locked_balance = GREATEST(0, locked_balance - ?) WHERE username = ?";

    try (PreparedStatement ps = conn.prepareStatement(sqlDelete)) {
      ps.setInt(1, auctionId); ps.setString(2, username); ps.executeUpdate();
    }
    try (PreparedStatement ps = conn.prepareStatement(sqlUnlock)) {
      ps.setDouble(1, holdAmount); ps.setString(2, username); ps.executeUpdate();
    }
  }

  /** Release tất cả holds của 1 auction và giảm locked_balance từng user. */
  private void releaseAllHoldsWithUnlock(Connection conn, int auctionId) throws SQLException {
    String sqlGetAll =
        "SELECT bidder_username, hold_amount FROM auction_bid_holds WHERE auction_id = ?";
    String sqlUnlock =
        "UPDATE user SET locked_balance = GREATEST(0, locked_balance - ?) WHERE username = ?";
    String sqlDeleteAll =
        "DELETE FROM auction_bid_holds WHERE auction_id = ?";

    try (PreparedStatement ps = conn.prepareStatement(sqlGetAll)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          String user = rs.getString("bidder_username");
          double hold = rs.getDouble("hold_amount");
          try (PreparedStatement pu = conn.prepareStatement(sqlUnlock)) {
            pu.setDouble(1, hold); pu.setString(2, user); pu.executeUpdate();
          }
        }
      }
    }
    try (PreparedStatement ps = conn.prepareStatement(sqlDeleteAll)) {
      ps.setInt(1, auctionId); ps.executeUpdate();
    }
  }

  private double getHoldAmount(Connection conn, int auctionId, String username) throws SQLException {
    String sql =
        "SELECT hold_amount FROM auction_bid_holds WHERE auction_id = ? AND bidder_username = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId); ps.setString(2, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getDouble("hold_amount") : 0;
      }
    }
  }

  /**
   * Kiểm tra available balance >= bidAmount.
   * available = balance - sum(holds) + currentHoldForThisAuction (vì sẽ được replace)
   */
  private boolean hasEnoughAvailableBalance(
      Connection conn, String username, int auctionId, double bidAmount) throws SQLException {
    String sqlBalance = "SELECT balance, locked_balance FROM user WHERE username = ?";
    String sqlCurrentHold =
        "SELECT hold_amount FROM auction_bid_holds WHERE auction_id = ? AND bidder_username = ?";

    double balance;
    double lockedBalance;
    try (PreparedStatement ps = conn.prepareStatement(sqlBalance)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) return false;
        balance = rs.getDouble("balance");
        lockedBalance = rs.getDouble("locked_balance");
      }
    }

    double currentHoldForThisAuction = 0;
    try (PreparedStatement ps = conn.prepareStatement(sqlCurrentHold)) {
      ps.setInt(1, auctionId); ps.setString(2, username);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) currentHoldForThisAuction = rs.getDouble("hold_amount");
      }
    }

    double available = balance - lockedBalance + currentHoldForThisAuction;
    return available >= bidAmount;
  }

  // -----------------------------------------------------------------------
  // setAutoBid
  // -----------------------------------------------------------------------
  public String setAutoBid(int auctionId, String bidderUsername, double maxAmount, double bidStep) {
    if (auctionId <= 0 || bidderUsername == null || bidderUsername.isBlank()
        || !Double.isFinite(maxAmount) || maxAmount <= 0
        || !Double.isFinite(bidStep) || bidStep <= 0) {
      return "INVALID_INPUT";
    }

    String checkSql =
        "SELECT status, start_time, end_time, cur_price FROM auctions WHERE id = ? FOR UPDATE";
    String upsertSql =
        "INSERT INTO auction_auto_bids (auction_id, bidder_username, max_amount, bid_step, active) "
            + "VALUES (?, ?, ?, ?, true) "
            + "ON DUPLICATE KEY UPDATE max_amount = VALUES(max_amount), bid_step = VALUES(bid_step), active = true";

    try (Connection conn = getConn()) {
      conn.setAutoCommit(false);
      try {
        double curPrice;
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
          ps.setInt(1, auctionId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) { conn.rollback(); return "AUCTION_NOT_FOUND"; }
            String normalizedStatus = normalizeStatus(
                rs.getString("status"), rs.getString("start_time"), rs.getString("end_time"));
            if (!"ACTIVE".equals(normalizedStatus)) {
              conn.rollback(); return "AUCTION_NOT_ACTIVE";
            }
            curPrice = rs.getDouble("cur_price");
          }
        }

        if (maxAmount <= curPrice) { conn.rollback(); return "PRICE_TOO_LOW"; }

        try (PreparedStatement ps = conn.prepareStatement(upsertSql)) {
          ps.setInt(1, auctionId);
          ps.setString(2, bidderUsername);
          ps.setDouble(3, maxAmount);
          ps.setDouble(4, bidStep);
          ps.executeUpdate();
        }

        processAutoBids(conn, auctionId);

        conn.commit();
        return "OK";
      } catch (Exception e) {
        conn.rollback(); throw new RuntimeException(e);
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  // -----------------------------------------------------------------------
  // Auto-bid helpers
  // -----------------------------------------------------------------------
  private static class CurrentAuction {
    double curPrice; String curLeader;
    CurrentAuction(double p, String l) { curPrice = p; curLeader = l; }
  }

  private static class AutoBidCandidate {
    String username;
    double maxAmount;
    double bidStep;

    AutoBidCandidate(String u, double m, double s) {
      username = u;
      maxAmount = m;
      bidStep = s;
    }
  }

  public boolean hasActiveAutoBids(int auctionId) {
    String sql = "SELECT 1 FROM auction_auto_bids WHERE auction_id = ? AND active = true LIMIT 1";
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
    } catch (SQLException e) { throw new RuntimeException(e); }
  }

  // -----------------------------------------------------------------------
  // getMyAuctionHistory – lấy danh sách auction user đã/đang tham gia
  // -----------------------------------------------------------------------
  public List<com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp.AuctionHistoryRecord>
      getMyAuctionHistory(String username) {
    String sql =
        "SELECT a.id, a.title, a.status, a.cur_price, a.cur_leader, "
            + "a.start_time, a.end_time, MAX(ab.bid_amount) AS my_top_bid "
            + "FROM auction_bids ab "
            + "JOIN auctions a ON a.id = ab.auction_id "
            + "WHERE ab.bidder_username = ? "
            + "GROUP BY a.id, a.title, a.status, a.cur_price, a.cur_leader, a.start_time, a.end_time "
            + "ORDER BY a.id DESC";

    List<com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp.AuctionHistoryRecord> list =
        new java.util.ArrayList<>();
    try (Connection conn = getConn(); PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp.AuctionHistoryRecord rec =
              new com.group15.daugia.shared.JSON.JSONMyAuctionHistoryTemp.AuctionHistoryRecord();
          rec.setAuctionId(rs.getInt("id"));
          rec.setTitle(rs.getString("title"));
          rec.setStatus(
              normalizeStatus(rs.getString("status"), rs.getString("start_time"), rs.getString("end_time")));
          rec.setCurPrice(rs.getDouble("cur_price"));
          rec.setCurLeader(rs.getString("cur_leader"));
          rec.setMyTopBid(rs.getDouble("my_top_bid"));
          rec.setStartTime(rs.getString("start_time"));
          rec.setEndTime(rs.getString("end_time"));
          list.add(rec);
        }
      }
      return list;
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private CurrentAuction getCurrentAuction(Connection conn, int auctionId) throws SQLException {
    String sql = "SELECT cur_price, cur_leader FROM auctions WHERE id = ? FOR UPDATE";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        if (!rs.next()) throw new SQLException("Auction not found: " + auctionId);
        return new CurrentAuction(rs.getDouble("cur_price"), rs.getString("cur_leader"));
      }
    }
  }

  private List<AutoBidCandidate> findTopAutoBidCandidates(
      Connection conn, int auctionId, double currentPrice) throws SQLException {
    String sql =
        "SELECT bidder_username, max_amount, bid_step FROM auction_auto_bids "
            + "WHERE auction_id = ? AND active = true AND max_amount >= ? "
            + "ORDER BY max_amount DESC, updated_at ASC LIMIT 2";
    List<AutoBidCandidate> candidates = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId); ps.setDouble(2, currentPrice);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next())
          candidates.add(
              new AutoBidCandidate(
                  rs.getString("bidder_username"),
                  rs.getDouble("max_amount"),
                  rs.getDouble("bid_step")));
      }
    }
    return candidates;
  }

  private void updateAuctionBid(
      Connection conn, int auctionId, String bidderUsername, double amount) throws SQLException {
    String sql =
        "UPDATE auctions SET cur_price = ?, cur_leader = ?, version = version + 1 WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setDouble(1, amount); ps.setString(2, bidderUsername); ps.setInt(3, auctionId);
      if (ps.executeUpdate() != 1) throw new SQLException("Failed to update auction bid: " + auctionId);
    }
  }

  private void insertAuctionBid(
      Connection conn, int auctionId, String bidderUsername, double amount) throws SQLException {
    String sql = "INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) VALUES (?,?,?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setInt(1, auctionId); ps.setString(2, bidderUsername); ps.setDouble(3, amount);
      ps.executeUpdate();
    }
  }

  /**
   * Chạy auto-bid logic. Trả về AutoBidResult nếu có bid xảy ra, null nếu không.
   */
  public static class AutoBidResult {
    public final String bidderUsername;
    public final double bidAmount;
    AutoBidResult(String u, double a) { bidderUsername = u; bidAmount = a; }
  }

  /**
   * Gọi từ bên ngoài (worker) sau khi transaction commit để biết auto-bid có xảy ra không.
   * Trả về null nếu không có auto-bid.
   */
  private AutoBidResult processAutoBids(Connection conn, int auctionId) throws SQLException {
    CurrentAuction current = getCurrentAuction(conn, auctionId);
    List<AutoBidCandidate> candidates =
        findTopAutoBidCandidates(conn, auctionId, current.curPrice);

    if (candidates.isEmpty()) return null;

    AutoBidCandidate winner = candidates.get(0);
    if (winner.username.equals(current.curLeader) && candidates.size() == 1) return null;

    double winnerStep = winner.bidStep > 0 ? winner.bidStep : DEFAULT_BID_STEP;
    if (winnerStep > (winner.maxAmount - current.curPrice)) return null;
    double minNextBid = current.curPrice + winnerStep;
    double secondLimit = candidates.size() > 1 ? candidates.get(1).maxAmount : current.curPrice;
    double targetAmount = Math.max(minNextBid, secondLimit + winnerStep);
    double bidAmount = Math.min(winner.maxAmount, targetAmount);

    if (bidAmount <= current.curPrice) return null;

    // Check available balance trước khi auto-bid
    if (!hasEnoughAvailableBalance(conn, winner.username, auctionId, bidAmount)) return null;

    updateAuctionBid(conn, auctionId, winner.username, bidAmount);
    insertAuctionBid(conn, auctionId, winner.username, bidAmount);
    upsertHold(conn, auctionId, winner.username, bidAmount);
    if (current.curLeader != null && !current.curLeader.equals(winner.username)) {
      releaseHold(conn, auctionId, current.curLeader);
    }
    return new AutoBidResult(winner.username, bidAmount);
  }
}
