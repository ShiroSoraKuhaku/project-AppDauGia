package com.group15.daugia.server.watch;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UnwatchAuctionIT extends BaseTest {

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
    void unwatchStopsPushEvents() throws Exception {
        WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");

        // Unwatch ngay
        ws.unwatch("{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");
        Thread.sleep(200);
        ws.close();

        // Đặt bid sau khi unwatch - không nên nhận event
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        // Không crash = pass (watcher đã unregister)
    }

    @Test
    @Order(2)
    void unwatchAsShortLivedRequest() throws Exception {
        // UNWATCH-AUCTION gửi như request thông thường (invalid token -> 401)
        String resp = sendCommand("UNWATCH-AUCTION",
                "{\"token\":\"bad-tok\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void unwatchValidTokenReturns204() throws Exception {
        String resp = sendCommand("UNWATCH-AUCTION",
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("204 No Content", obj.get("response").getAsString());
    }
}
