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
public class PlaceBidIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("bidder1", "pass", "tok-bidder1");
        seedUserWithToken("bidder2", "pass", "tok-bidder2");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder2\",\"amount\":1000.0}");
        itemId = seedItem("bidder1", "Auction Item", 100.0);
        auctionId = seedActiveAuction("Test Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void placeBidSuccess() throws Exception {
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
        assertEquals(auctionId, obj.get("auctionId").getAsInt());
    }

    @Test
    @Order(2)
    void placeBidVersionIncrements() throws Exception {
        // Lấy version trước
        int versionBefore = getAuctionVersion(auctionId);
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        int versionAfter = getAuctionVersion(auctionId);
        assertEquals(versionBefore + 1, versionAfter);
    }

    @Test
    @Order(3)
    void placeBidPriceTooLow() throws Exception {
        // Đặt bid cao trước
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        // Đặt thấp hơn
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder2\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void placeBidEqualPriceFails() throws Exception {
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder2\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void placeBidAuctionInactive() throws Exception {
        int endedItem = seedItem("bidder1", "Ended Item", 50.0);
        int endedAuction = seedEndedAuction("Ended Auction", endedItem, 50.0);
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + endedAuction + ",\"bidAmount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void placeBidInvalidToken() throws Exception {
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"bad-tok\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(7)
    void placeBidAuctionNotFound() throws Exception {
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":99999,\"bidAmount\":150.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("404 Not Found", obj.get("response").getAsString());
    }

    @Test
    @Order(8)
    void placeBidInsertsHistoryRecord() throws Exception {
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM auction_bids WHERE auction_id=? AND bid_amount=150.0")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @Order(9)
    void placeBidUpdatesCurLeader() throws Exception {
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder2\",\"auctionId\":" + auctionId + ",\"bidAmount\":300.0}");
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("bidder2", rs.getString("cur_leader"));
                assertEquals(300.0, rs.getDouble("cur_price"), 0.01);
            }
        }
    }

    private int getAuctionVersion(int id) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT version FROM auctions WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
