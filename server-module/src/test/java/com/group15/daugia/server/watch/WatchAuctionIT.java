package com.group15.daugia.server.watch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WatchAuctionIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("watcher", "pass", "tok-watcher");
        seedUserWithToken("bidder", "pass", "tok-bidder");
        itemId = seedItem("watcher", "Watch Item", 100.0);
        auctionId = seedActiveAuction("Watch Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void watchAuctionReceivesAck() throws Exception {
        try (WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}")) {
            assertNotNull(ws.ack);
            JsonObject ack = JsonParser.parseString(ws.ack).getAsJsonObject();
            assertEquals("200 OK", ack.get("response").getAsString());
            assertEquals(auctionId, ack.get("auctionId").getAsInt());
            assertEquals("ACTIVE", ack.get("status").getAsString());
        }
    }

    @Test
    @Order(2)
    void watchAuctionInvalidToken() throws Exception {
        try (WatchSession ws = openWatchSession(
                "{\"token\":\"bad-tok\",\"auctionId\":" + auctionId + "}")) {
            JsonObject ack = JsonParser.parseString(ws.ack).getAsJsonObject();
            assertEquals("401 Unauthorized", ack.get("response").getAsString());
        }
    }

    @Test
    @Order(3)
    void watchAuctionNotFound() throws Exception {
        try (WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":99999}")) {
            JsonObject ack = JsonParser.parseString(ws.ack).getAsJsonObject();
            assertEquals("404 Not Found", ack.get("response").getAsString());
        }
    }

    @Test
    @Order(4)
    void watchReceivesBidPlacedEvent() throws Exception {
        try (WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}")) {
            // Đặt bid từ thread khác
            Thread bidThread = new Thread(() -> {
                try {
                    sendCommand("PLACE-BID",
                            "{\"token\":\"tok-bidder\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            bidThread.start();
            bidThread.join(3000);

            String event = ws.readEvent(3000);
            assertNotNull(event, "Should receive BID_PLACED event");
            JsonObject evObj = JsonParser.parseString(event).getAsJsonObject();
            assertEquals("BID_PLACED", evObj.get("eventType").getAsString());
            assertEquals(auctionId, evObj.get("auctionId").getAsInt());
            assertEquals(200.0, evObj.get("curPrice").getAsDouble(), 0.01);
        }
    }
}
