package com.group15.daugia.server.money;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoneyIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("user1", "pass", "tok-user1");
    }

    // ── TOPUP-BALANCE ─────────────────────────────────────────────────────

    @Test
    @Order(1)
    void topupSuccess() throws Exception {
        String resp = sendCommand("TOPUP-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":500.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(500.0, obj.get("balance").getAsDouble(), 0.001);
        assertEquals(0.0,   obj.get("lockedBalance").getAsDouble(), 0.001);
        assertEquals(500.0, obj.get("availableBalance").getAsDouble(), 0.001);
    }

    @Test
    @Order(2)
    void topupAccumulates() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":100.0}");
        String resp = sendCommand("TOPUP-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals(300.0, obj.get("balance").getAsDouble(), 0.001);
    }

    @Test
    @Order(3)
    void topupNegativeAmountRejects() throws Exception {
        String resp = sendCommand("TOPUP-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":-50.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertNotEquals("200 OK", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void topupInvalidTokenReturns401() throws Exception {
        String resp = sendCommand("TOPUP-BALANCE",
                "{\"token\":\"bad-tok\",\"amount\":100.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    // ── GET-BALANCE ───────────────────────────────────────────────────────

    @Test
    @Order(5)
    void getBalanceInitiallyZero() throws Exception {
        String resp = sendCommand("GET-BALANCE", "{\"token\":\"tok-user1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(0.0, obj.get("balance").getAsDouble(), 0.001);
        assertEquals(0.0, obj.get("availableBalance").getAsDouble(), 0.001);
    }

    @Test
    @Order(6)
    void getBalanceAfterTopup() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":750.0}");
        String resp = sendCommand("GET-BALANCE", "{\"token\":\"tok-user1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals(750.0, obj.get("balance").getAsDouble(), 0.001);
        assertEquals(750.0, obj.get("availableBalance").getAsDouble(), 0.001);
    }

    @Test
    @Order(7)
    void getBalanceInvalidTokenReturns401() throws Exception {
        String resp = sendCommand("GET-BALANCE", "{\"token\":\"bad-tok\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    // ── Bid với insufficient funds ────────────────────────────────────────

    @Test
    @Order(8)
    void bidWithInsufficientFundsRejected() throws Exception {
        seedUserWithToken("seller1", "pass", "tok-seller1");
        // user1 balance = 0, cố bid 200 -> phải bị từ chối
        int itemId = seedItem("seller1", "Expensive Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        // 409 Conflict hoặc 400 Bad Request (balance check fail)
        assertNotEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(9)
    void bidWithSufficientFundsSucceeds() throws Exception {
        seedUserWithToken("seller1", "pass", "tok-seller1");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":500.0}");
        int itemId = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);

        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }
}
