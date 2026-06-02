package com.group15.daugia.server.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra GET-ITEMS chỉ trả về SCHEDULED/ACTIVE, loại trừ item của chính user,
 * và hỗ trợ lọc theo tên.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetActiveItemsIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedUserWithToken("bidder1", "pass", "tok-bidder1");
    }

    @Test
    @Order(1)
    void returnsOnlyActiveAndScheduledAuctions() throws Exception {
        int item1 = seedItem("seller1", "Active Item", 100.0);
        int item2 = seedItem("seller1", "Ended Item", 100.0);
        seedActiveAuction("Active Auction", item1, 100.0, 3600);
        seedEndedAuction("Ended Auction", item2, 100.0);

        String resp = sendCommand("GET-ITEMS", "{\"token\":\"tok-bidder1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());

        JsonArray list = obj.get("itemList").getAsJsonArray();
        assertEquals(1, list.size());
        assertEquals("Active Auction", list.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @Order(2)
    void excludesItemsOwnedByCurrentUser() throws Exception {
        // seller1 tạo item và auction
        int item1 = seedItem("seller1", "Seller Own Item", 100.0);
        seedActiveAuction("Own Auction", item1, 100.0, 3600);

        // bidder1 cũng tạo item và auction
        int item2 = seedItem("bidder1", "Bidder Own Item", 200.0);
        seedActiveAuction("Bidder Auction", item2, 200.0, 3600);

        // bidder1 gọi GET-ITEMS -> chỉ thấy item của seller1, không thấy item của mình
        String resp = sendCommand("GET-ITEMS", "{\"token\":\"tok-bidder1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray list = obj.get("itemList").getAsJsonArray();

        assertEquals(1, list.size());
        assertEquals("Own Auction", list.get(0).getAsJsonObject().get("name").getAsString());
    }

    @Test
    @Order(3)
    void filterByNameReturnsMatchingItems() throws Exception {
        int item1 = seedItem("seller1", "Apple Watch", 100.0);
        int item2 = seedItem("seller1", "Samsung Phone", 200.0);
        seedActiveAuction("Apple Watch", item1, 100.0, 3600);
        seedActiveAuction("Samsung Phone", item2, 200.0, 3600);

        String resp = sendCommand("GET-ITEMS",
                "{\"token\":\"tok-bidder1\",\"nameFilter\":\"Apple\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray list = obj.get("itemList").getAsJsonArray();

        assertEquals(1, list.size());
        assertTrue(list.get(0).getAsJsonObject().get("name").getAsString().contains("Apple"));
    }

    @Test
    @Order(4)
    void filterByNameCaseInsensitive() throws Exception {
        int item1 = seedItem("seller1", "Laptop Pro", 1000.0);
        seedActiveAuction("Laptop Pro", item1, 1000.0, 3600);

        String resp = sendCommand("GET-ITEMS",
                "{\"token\":\"tok-bidder1\",\"nameFilter\":\"laptop\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray list = obj.get("itemList").getAsJsonArray();
        assertEquals(1, list.size());
    }

    @Test
    @Order(5)
    void emptyNameFilterReturnsAll() throws Exception {
        int item1 = seedItem("seller1", "Item A", 100.0);
        int item2 = seedItem("seller1", "Item B", 200.0);
        seedActiveAuction("Item A", item1, 100.0, 3600);
        seedActiveAuction("Item B", item2, 200.0, 3600);

        String resp = sendCommand("GET-ITEMS",
                "{\"token\":\"tok-bidder1\",\"nameFilter\":\"\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray list = obj.get("itemList").getAsJsonArray();
        assertEquals(2, list.size());
    }

    @Test
    @Order(6)
    void invalidTokenReturns401() throws Exception {
        String resp = sendCommand("GET-ITEMS", "{\"token\":\"bad-tok\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }
}
