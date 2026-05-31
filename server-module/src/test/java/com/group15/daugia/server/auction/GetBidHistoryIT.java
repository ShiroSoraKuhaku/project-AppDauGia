package com.group15.daugia.server.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetBidHistoryIT extends BaseTest {

  private int auctionId;

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("seller", "pass", "tok-seller");
    seedUserWithToken("b1", "pass", "tok-b1");
    seedUserWithToken("b2", "pass", "tok-b2");
    sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-b1\",\"amount\":10000.0}");
    sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-b2\",\"amount\":10000.0}");
    int itemId = seedItem("seller", "History Item", 100.0);
    auctionId = seedActiveAuction("History Auction", itemId, 100.0, 3600);
  }

  @Test
  void getHistoryReturnsLatestBidsDesc() throws Exception {
    sendCommand("PLACE-BID", "{\"token\":\"tok-b1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
    sendCommand("PLACE-BID", "{\"token\":\"tok-b2\",\"auctionId\":" + auctionId + ",\"bidAmount\":250.0}");

    String resp = sendCommand("GET-BID-HISTORY", "{\"token\":\"tok-b1\",\"auctionId\":" + auctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
    JsonArray bids = obj.getAsJsonArray("bids");
    assertTrue(bids.size() >= 2);
    assertEquals("b2", bids.get(0).getAsJsonObject().get("bidderUsername").getAsString());
    assertEquals(250.0, bids.get(0).getAsJsonObject().get("bidAmount").getAsDouble(), 0.01);
  }

  @Test
  void getHistoryEmptyListWhenNoBid() throws Exception {
    String resp = sendCommand("GET-BID-HISTORY", "{\"token\":\"tok-b1\",\"auctionId\":" + auctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
    assertEquals(0, obj.getAsJsonArray("bids").size());
  }

  @Test
  void getHistoryUnauthorizedWhenMissingToken() throws Exception {
    String resp = sendCommand("GET-BID-HISTORY", "{\"auctionId\":" + auctionId + "}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("401 Unauthorized", obj.get("response").getAsString());
  }

  @Test
  void getHistoryNotFoundWhenAuctionMissing() throws Exception {
    String resp = sendCommand("GET-BID-HISTORY", "{\"token\":\"tok-b1\",\"auctionId\":999999}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("404 Not Found", obj.get("response").getAsString());
  }
}
