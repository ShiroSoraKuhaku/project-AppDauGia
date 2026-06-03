package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra rằng khi manual bid kích hoạt auto-bid, watcher nhận 2 events BID_PLACED riêng biệt:
 * 1 cho manual bidder, 1 cho auto-bidder.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AutoBidBroadcastIT extends BaseTest {

    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedUserWithToken("autoUser", "pass", "tok-autoUser");
        seedUserWithToken("manualUser", "pass", "tok-manualUser");
        seedUserWithToken("watcher", "pass", "tok-watcher");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-autoUser\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-manualUser\",\"amount\":1000.0}");
        int itemId = seedItem("seller1", "Broadcast Item", 100.0);
        auctionId = seedActiveAuction("Broadcast Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void manualBidWithoutAutoBidSendsOneEvent() throws Exception {
        // Không có auto-bid -> chỉ 1 event
        WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-manualUser\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        String event1 = ws.readEvent(2000);
        String event2 = ws.readEvent(500); // không nên có event thứ 2

        assertNotNull(event1, "Should receive first BID_PLACED event");
        JsonObject e1 = JsonParser.parseString(event1).getAsJsonObject();
        assertEquals("BID_PLACED", e1.get("eventType").getAsString());
        assertEquals("manualUser", e1.get("bidderUsername").getAsString());
        assertNull(event2, "Should not receive second event when no auto-bid");

        ws.close();
    }

    @Test
    @Order(2)
    void manualBidTriggeringAutoBidSendsTwoEvents() throws Exception {
        // autoUser đặt auto-bid max=400, manualUser đặt tay 200
        // -> auto-bid phản ứng -> 2 events
        sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-autoUser\",\"auctionId\":" + auctionId + ",\"maxAmount\":400.0}");

        WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-manualUser\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        String event1 = ws.readEvent(2000);
        String event2 = ws.readEvent(2000);

        assertNotNull(event1, "Should receive first event (manual bid)");
        assertNotNull(event2, "Should receive second event (auto-bid)");

        JsonObject e1 = JsonParser.parseString(event1).getAsJsonObject();
        JsonObject e2 = JsonParser.parseString(event2).getAsJsonObject();

        assertEquals("BID_PLACED", e1.get("eventType").getAsString());
        assertEquals("BID_PLACED", e2.get("eventType").getAsString());
        assertEquals("manualUser", e1.get("bidderUsername").getAsString());
        assertEquals("autoUser", e2.get("bidderUsername").getAsString());

        ws.close();
    }

    @Test
    @Order(3)
    void setAutoBidThatTriggersBidSendsEvent() throws Exception {
        // manualUser đặt bid trước, sau đó autoUser set auto-bid cao hơn
        // -> auto-bid xảy ra ngay khi SET-AUTO-BID -> watcher nhận event
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-manualUser\",\"amount\":1000.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-manualUser\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        WatchSession ws = openWatchSession(
                "{\"token\":\"tok-watcher\",\"auctionId\":" + auctionId + "}");

        sendCommand("SET-AUTO-BID",
                "{\"token\":\"tok-autoUser\",\"auctionId\":" + auctionId + ",\"maxAmount\":400.0}");

        String event1 = ws.readEvent(2000);
        assertNotNull(event1, "Auto-bid should trigger broadcast when set");
        JsonObject e1 = JsonParser.parseString(event1).getAsJsonObject();
        assertEquals("BID_PLACED", e1.get("eventType").getAsString());
        assertEquals("autoUser", e1.get("bidderUsername").getAsString());

        ws.close();
    }
}
