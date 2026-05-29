package com.group15.daugia.server.items;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminUpdateItemIT extends BaseTest {

  private int itemId;
  private String startTime;
  private String endTime;

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("admin", "admin", "tok-admin");
    seedUser("seller1", "pass");
    itemId = seedItem("seller1", "Old Item", 100.0);

    LocalDateTime start = LocalDateTime.now().plusMinutes(5);
    LocalDateTime end = start.plusHours(1);
    startTime = start.format(FMT);
    endTime = end.format(FMT);
  }

  @Test
  @Order(1)
  void adminCanUpdateAnyItem() throws Exception {
    String payload =
        "{\"id\":" + itemId + ",\"token\":\"tok-admin\",\"name\":\"Updated Item\","
            + "\"price\":150.0,\"desc\":\"new\",\"startTime\":\"" + startTime + "\","
            + "\"endTime\":\"" + endTime + "\"}";
    String resp = sendCommand("UPDATE-ITEM", payload);
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());

    try (Connection c = getJdbcConn();
        PreparedStatement ps =
            c.prepareStatement("SELECT name, price FROM items WHERE id = ?")) {
      ps.setInt(1, itemId);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next());
        assertEquals("Updated Item", rs.getString("name"));
        assertEquals(150.0, rs.getDouble("price"), 0.001);
      }
    }
  }
}
