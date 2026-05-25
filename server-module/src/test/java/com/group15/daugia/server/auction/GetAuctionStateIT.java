package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetAuctionStateIT extends BaseTest {

    private int itemId;
    private int activeAuctionId;
    private int endedAuctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("user1", "pass", "tok-user1");
        itemId = seedItem("user1", "Test Item", 100.0);
        activeAuctionId = seedActiveAuction("Active Auction", itemId, 100.0, 3600);
        endedAuctionId = seedEndedAuction("Ended Auction", itemId, 50.0);
    }

    @Test
    @Order(1)
    void getAuctionStateActiveOk() throws Exception {
        String resp = sendCommand("GET-AUCTION-STATE",
                "{\"token\":\"tok-user1\",\"auctionId\":" + activeAuctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals("ACTIVE", obj.get("status").getAsString());
        assertEquals(activeAuctionId, obj.get("auctionId").getAsInt());
        assertTrue(obj.get("secondsRemaining").getAsLong() > 0);
    }

    @Test
    @Order(2)
    void getAuctionStateEndedOk() throws Exception {
        String resp = sendCommand("GET-AUCTION-STATE",
                "{\"token\":\"tok-user1\",\"auctionId\":" + endedAuctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals("ENDED", obj.get("status").getAsString());
    }

    @Test
    @Order(3)
    void getAuctionStateNotFound() throws Exception {
        String resp = sendCommand("GET-AUCTION-STATE",
                "{\"token\":\"tok-user1\",\"auctionId\":99999}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("404 Not Found", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void getAuctionStateInvalidToken() throws Exception {
        String resp = sendCommand("GET-AUCTION-STATE",
                "{\"token\":\"bad-tok\",\"auctionId\":" + activeAuctionId + "}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }
}
