package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoBidStepIT extends BaseTest {
  private int auctionId;

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("u1", "pass", "tok-u1");
    seedUserWithToken("u2", "pass", "tok-u2");
    sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-u1\",\"amount\":10000.0}");
    sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-u2\",\"amount\":10000.0}");
    int itemId = seedItem("u1", "Step Item", 100.0);
    auctionId = seedActiveAuction("Step Auction", itemId, 100.0, 3600);
  }

  @Test
  void customBidStepStored() throws Exception {
    String resp =
        sendCommand(
            "SET-AUTO-BID",
            "{\"token\":\"tok-u1\",\"auctionId\":" + auctionId + ",\"maxAmount\":500.0,\"bidStep\":25.0}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("201 Created", obj.get("response").getAsString());

    try (Connection c = getJdbcConn();
        PreparedStatement ps =
            c.prepareStatement(
                "SELECT bid_step FROM auction_auto_bids WHERE auction_id=? AND bidder_username='u1'")) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        assertEquals(25.0, rs.getDouble("bid_step"), 0.01);
      }
    }
  }

  @Test
  void winnerStepControlsTargetAmount() throws Exception {
    sendCommand("SET-AUTO-BID", "{\"token\":\"tok-u1\",\"auctionId\":" + auctionId + ",\"maxAmount\":1000.0,\"bidStep\":100.0}");
    sendCommand("SET-AUTO-BID", "{\"token\":\"tok-u2\",\"auctionId\":" + auctionId + ",\"maxAmount\":700.0,\"bidStep\":1.0}");

    try (Connection c = getJdbcConn();
         PreparedStatement ps = c.prepareStatement("SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        assertEquals("u1", rs.getString("cur_leader"));
        assertEquals(800.0, rs.getDouble("cur_price"), 0.01);
      }
    }
  }

  @Test
  void invalidBidStepRejected() throws Exception {
    String resp = sendCommand("SET-AUTO-BID", "{\"token\":\"tok-u1\",\"auctionId\":" + auctionId + ",\"maxAmount\":500.0,\"bidStep\":0}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("400 Bad Request", obj.get("response").getAsString());
  }

  @Test
  void tooLargeStepDoesNotFireAutoBid() throws Exception {
    sendCommand("SET-AUTO-BID", "{\"token\":\"tok-u1\",\"auctionId\":" + auctionId + ",\"maxAmount\":150.0,\"bidStep\":100.0}");
    try (Connection c = getJdbcConn();
         PreparedStatement ps = c.prepareStatement("SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        assertNull(rs.getString("cur_leader"));
        assertEquals(100.0, rs.getDouble("cur_price"), 0.01);
      }
    }
  }
}
