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
public class AdminAuctionControlIT extends BaseTest {

  private int scheduledAuctionId;
  private int activeAuctionId;

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("admin", "admin", "tok-admin");
    seedUser("seller1", "pass");
    int itemId1 = seedItem("seller1", "Scheduled Item", 100.0);
    int itemId2 = seedItem("seller1", "Active Item", 120.0);
    scheduledAuctionId = seedScheduledAuction("Scheduled Auction", itemId1, 100.0, 3600, 7200);
    activeAuctionId = seedActiveAuction("Active Auction", itemId2, 120.0, 3600);
  }

  @Test
  @Order(1)
  void adminCanOpenScheduledAuction() throws Exception {
    String resp =
        sendCommand(
            "OPEN-AUCTION", "{\"token\":\"tok-admin\",\"auctionId\":" + scheduledAuctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
    assertDbStatus(scheduledAuctionId, "ACTIVE");
  }

  @Test
  @Order(2)
  void adminCanCloseActiveAuction() throws Exception {
    String resp =
        sendCommand(
            "CLOSE-AUCTION", "{\"token\":\"tok-admin\",\"auctionId\":" + activeAuctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
    assertDbStatus(activeAuctionId, "ENDED");
  }

  @Test
  @Order(3)
  void nonAdminCannotOpenAuction() throws Exception {
    seedUserWithToken("seller1", "pass", "tok-seller");
    String resp =
        sendCommand(
            "OPEN-AUCTION", "{\"token\":\"tok-seller\",\"auctionId\":" + scheduledAuctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("403 Forbidden", obj.get("response").getAsString());
  }

  private void assertDbStatus(int auctionId, String expected) throws Exception {
    try (Connection c = getJdbcConn();
        PreparedStatement ps =
            c.prepareStatement("SELECT status FROM auctions WHERE id = ?")) {
      ps.setInt(1, auctionId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals(expected, rs.getString("status"));
      }
    }
  }
}
