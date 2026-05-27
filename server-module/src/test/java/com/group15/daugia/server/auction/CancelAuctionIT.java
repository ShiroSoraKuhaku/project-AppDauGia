package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CancelAuctionIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass",  "tok-seller1");
        seedUserWithToken("bidder1", "pass",  "tok-bidder1");
        seedUserWithToken("admin",   "admin", "tok-admin");
        // Topup bidder để có thể bid
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        itemId    = seedItem("seller1", "Test Item", 100.0);
        auctionId = seedActiveAuction("Test Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void sellerCanCancelOwnAuction() throws Exception {
        String resp = sendCommand("CANCEL-AUCTION",
                "{\"token\":\"tok-seller1\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(auctionId, obj.get("auctionId").getAsInt());
        assertDbStatus("CANCELLED");
    }

    @Test
    @Order(2)
    void bidderCannotCancelOthersAuction() throws Exception {
        String resp = sendCommand("CANCEL-AUCTION",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("403 Forbidden", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void adminCanCancelAnyAuction() throws Exception {
        String resp = sendCommand("CANCEL-AUCTION",
                "{\"token\":\"tok-admin\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertDbStatus("CANCELLED");
    }

    @Test
    @Order(4)
    void cancelAuctionReleasesHolds() throws Exception {
        // Bidder đặt bid trước
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        // Kiểm tra hold đã được tạo
        assertEquals(1, countHolds(auctionId));

        // Seller cancel
        sendCommand("CANCEL-AUCTION",
                "{\"token\":\"tok-seller1\",\"auctionId\":" + auctionId + "}");

        // Hold phải được release
        assertEquals(0, countHolds(auctionId));

        // locked_balance của bidder phải về 0
        assertEquals(0.0, getLockedBalance("bidder1"), 0.001);
    }

    @Test
    @Order(5)
    void cancelNonExistentAuction() throws Exception {
        String resp = sendCommand("CANCEL-AUCTION",
                "{\"token\":\"tok-seller1\",\"auctionId\":99999}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("404 Not Found", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void cancelWithoutTokenReturns401() throws Exception {
        String resp = sendCommand("CANCEL-AUCTION",
                "{\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    // ── JDBC helpers ───────────────────────────────────────────────────────

    private void assertDbStatus(String expected) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT status FROM auctions WHERE id = ?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(expected, rs.getString("status"));
            }
        }
    }

    private int countHolds(int auction) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM auction_bid_holds WHERE auction_id = ?")) {
            ps.setInt(1, auction);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private double getLockedBalance(String username) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT locked_balance FROM user WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("locked_balance");
            }
        }
    }
}
