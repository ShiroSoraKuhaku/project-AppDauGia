package com.group15.daugia.server.DAO;

import com.group15.daugia.server.resource.DBProperty;

import java.sql.*;

public class BidDAO {
    private static BidDAO instance;
    private final DBProperty dbProperty = DBProperty.getInstance();

    private BidDAO(){}

    public static BidDAO getBidDao(){
        if (instance == null) {
            instance = new BidDAO();
        }
        return instance;
    }

    public String placeBid(int itemId, String bidderUsername, double bidPrice){
        if (itemId <= 0
                || bidderUsername == null
                || bidderUsername.isBlank()
                || !Double.isFinite(bidPrice)
                || bidPrice <= 0) {
            return "INVALID_INPUT";
        }

        String itemSql = "select seller_username, price from items where id = ? for update";
        String maxBidSql = "select max(price) as max_price from bids where item_id = ?";
        String insertSql = "insert into bids (item_id, bidder_username, price) values (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(
                dbProperty.getDBUrl(), dbProperty.getUsername(), dbProperty.getPassword())) {

            conn.setAutoCommit(false);

            String sellerUsername;
            double startPrice;

            try (PreparedStatement statement = conn.prepareStatement(itemSql)) {
                statement.setInt(1, itemId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return "ITEM_NOT_FOUND";
                    }

                    sellerUsername = rs.getString("seller_username");
                    startPrice = rs.getDouble("price");
                }
            }

            if (sellerUsername.equals(bidderUsername)) {
                conn.rollback();
                return "SELLER_CANNOT_BID";
            }

            double currentPrice = startPrice;

            try (PreparedStatement statement = conn.prepareStatement(maxBidSql)) {
                statement.setInt(1, itemId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next() && rs.getObject("max_price") != null) {
                        currentPrice = rs.getDouble("max_price");
                    }
                }
            }

            if (bidPrice <= currentPrice) {
                conn.rollback();
                return "PRICE_TOO_LOW";
            }

            try (PreparedStatement statement = conn.prepareStatement(insertSql)) {
                statement.setInt(1, itemId);
                statement.setString(2, bidderUsername);
                statement.setDouble(3, bidPrice);

                if (statement.executeUpdate() == 1) {
                    conn.commit();
                    return "OK";
                }

                conn.rollback();
                return "FAILED";
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
