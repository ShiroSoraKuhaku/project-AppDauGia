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
public class AutoBidIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("userA", "pass", "tok-userA");
        seedUserWithToken("userB", "pass", "tok-userB");
        itemId = seedItem("userA", "Auto Item", 100.0);
        auctionId = seedActiveAuction("Auto Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void setAutoBidSuccess() throws Exception {
        String resp = sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":" + auctionId + ",\"maxAmount\":500.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(2)
    void setAutoBidPriceTooLow() throws Exception {
        String resp = sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":" + auctionId + ",\"maxAmount\":50.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("409 Conflict", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void setAutoBidInvalidToken() throws Exception {
        String resp = sendCommand("SET-AUTO-BID",
                "{\"token\":\"bad-tok\",\"auctionId\":" + auctionId + ",\"maxAmount\":500.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void setAutoBidAuctionNotFound() throws Exception {
        String resp = sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":99999,\"maxAmount\":500.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("404 Not Found", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void setAutoBidAuctionEnded() throws Exception {
        int eItem = seedItem("userA", "Ended", 50.0);
        int eAuction = seedEndedAuction("Ended", eItem, 50.0);
        String resp = sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":" + eAuction + ",\"maxAmount\":500.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void autoBidReactsToManualBid() throws Exception {
        // userA đặt auto-bid max=300, userB đặt tay 150
        // Kết quả: userA dẫn ở 151 (second-price + increment)
        sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":" + auctionId + ",\"maxAmount\":300.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-userB\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");

        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("userA", rs.getString("cur_leader"));
                assertTrue(rs.getDouble("cur_price") > 150.0,
                        "Auto-bid should have outbid manual bid");
            }
        }
    }

    @Test
    @Order(7)
    void autoBidTwoCompetitors() throws Exception {
        // userA max=300, userB max=200 -> userA wins at 201
        sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userA\",\"auctionId\":" + auctionId + ",\"maxAmount\":300.0}");
        sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-userB\",\"auctionId\":" + auctionId + ",\"maxAmount\":200.0}");

        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("userA", rs.getString("cur_leader"));
                assertEquals(201.0, rs.getDouble("cur_price"), 0.01);
            }
        }
    }
}
