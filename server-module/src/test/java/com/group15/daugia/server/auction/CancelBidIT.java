package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * IT cho CANCEL-BID, bao gồm scenario từ AI_PLAN:
 *   user1 cancel bid, user2 over-committed elsewhere -> leader recompute skip user2
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CancelBidIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedUserWithToken("user1",   "pass", "tok-user1");
        seedUserWithToken("user2",   "pass", "tok-user2");
        // Topup
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user2\",\"amount\":500.0}");
    }

    @Test
    @Order(1)
    void cancelBidResetsLeaderToNone() throws Exception {
        int itemId    = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        assertLeader(auctionId, "user1");

        String resp = sendCommand("CANCEL-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());

        // Không còn leader
        assertLeaderNull(auctionId);
        // Hold của user1 phải được release
        assertEquals(0.0, getLockedBalance("user1"), 0.001);
    }

    @Test
    @Order(2)
    void cancelBidPromotesNextBidder() throws Exception {
        int itemId    = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user2\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        assertLeader(auctionId, "user1");

        // user1 cancel -> user2 là leader mới
        sendCommand("CANCEL-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + "}");
        assertLeader(auctionId, "user2");
    }

    @Test
    @Order(3)
    void cancelBidScenarioSkipsOverCommittedBidder() throws Exception {
        // Tình huống AI_PLAN: user2 đã lock hết balance ở auction khác
        // -> recompute phải skip user2, chọn user1 (nếu còn bid)
        // hoặc reset nếu không ai đủ tiền.

        int item1 = seedItem("seller1", "Item1", 100.0);
        int item2 = seedItem("seller1", "Item2", 100.0);
        int auction1 = seedActiveAuction("A1", item1, 100.0, 3600);
        int auction2 = seedActiveAuction("A2", item2, 100.0, 3600);

        // user2 (500) bid auction2 = 400 -> locked 400, available = 100
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user2\",\"auctionId\":" + auction2 + ",\"bidAmount\":400.0}");

        // Cả hai bid vào auction1
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user2\",\"auctionId\":" + auction1 + ",\"bidAmount\":150.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auction1 + ",\"bidAmount\":200.0}");
        assertLeader(auction1, "user1");

        // user1 cancel bid khỏi auction1
        sendCommand("CANCEL-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auction1 + "}");

        // user2 available = 500 - 400(hold auction2) = 100 < 150 (top bid cũ)
        // -> recompute phải skip user2 -> không ai đủ -> cur_leader = null
        assertLeaderNull(auction1);
    }

    @Test
    @Order(4)
    void cancelBidOnNonActiveAuction() throws Exception {
        int itemId    = seedItem("seller1", "Item", 100.0);
        int auctionId = seedEndedAuction("Ended", itemId, 100.0);

        String resp = sendCommand("CANCEL-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("409 Conflict", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void cancelBidNoBidReturns404() throws Exception {
        int itemId    = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        String resp = sendCommand("CANCEL-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("404 Not Found", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void cancelBidInvalidTokenReturns401() throws Exception {
        int itemId    = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        String resp = sendCommand("CANCEL-BID",
                "{\"token\":\"bad-tok\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    // ── JDBC helpers ───────────────────────────────────────────────────────

    private void assertLeader(int auctionId, String expected) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cur_leader FROM auctions WHERE id = ?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(expected, rs.getString("cur_leader"));
            }
        }
    }

    private void assertLeaderNull(int auctionId) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cur_leader FROM auctions WHERE id = ?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertNull(rs.getString("cur_leader"));
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
