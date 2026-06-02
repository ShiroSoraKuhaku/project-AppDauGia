package com.group15.daugia.server.money;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class WithdrawBalanceIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("user1", "pass", "tok-user1");
    }

    @Test
    @Order(1)
    void withdrawSuccessfully() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":500.0}");
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(300.0, obj.get("balance").getAsDouble(), 0.001);
        assertEquals(300.0, obj.get("availableBalance").getAsDouble(), 0.001);
    }

    @Test
    @Order(2)
    void withdrawExactAvailableBalance() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":300.0}");
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":300.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(0.0, obj.get("balance").getAsDouble(), 0.001);
    }

    @Test
    @Order(3)
    void withdrawMoreThanAvailableRejects() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":100.0}");
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void withdrawNegativeAmountRejects() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":500.0}");
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":-50.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertNotEquals("200 OK", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void withdrawWithInvalidTokenReturns401() throws Exception {
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"bad-tok\",\"amount\":100.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void withdrawCannotExceedAvailableBalanceWhenLockedExists() throws Exception {
        // Setup: user1 có balance=500, locked bởi bid
        seedUserWithToken("seller1", "pass", "tok-seller1");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-user1\",\"amount\":500.0}");
        int itemId = seedItem("seller1", "Item", 100.0);
        int auctionId = seedActiveAuction("Auction", itemId, 100.0, 3600);
        // Đặt bid 300 -> locked 300
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-user1\",\"auctionId\":" + auctionId + ",\"bidAmount\":300.0}");

        // Cố rút 300 (chỉ còn available = 200)
        String resp = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":300.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());

        // Rút đúng available = 200
        String resp2 = sendCommand("WITHDRAW-BALANCE",
                "{\"token\":\"tok-user1\",\"amount\":200.0}");
        JsonObject obj2 = JsonParser.parseString(resp2).getAsJsonObject();
        assertEquals("200 OK", obj2.get("response").getAsString());
        assertEquals(200.0, obj2.get("availableBalance").getAsDouble(), 0.001);
    }
}
