package com.group15.daugia.server.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetItemsIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedItem("seller1", "Item A", 50.0);
        seedItem("seller1", "Item B", 75.0);
    }

    @Test
    @Order(1)
    void getItemsReturnsAll() throws Exception {
        String resp = sendCommand("GET-ITEMS", "{}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        JsonArray list = obj.getAsJsonArray("itemList");
        assertEquals(2, list.size());
    }

    @Test
    @Order(2)
    void getItemsSortedByIdDesc() throws Exception {
        String resp = sendCommand("GET-ITEMS", "{}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonArray list = obj.getAsJsonArray("itemList");
        int firstId = list.get(0).getAsJsonObject().get("id").getAsInt();
        int secondId = list.get(1).getAsJsonObject().get("id").getAsInt();
        assertTrue(firstId > secondId, "Items should be sorted by id DESC");
    }

    @Test
    @Order(3)
    void getItemsEmptyReturnsEmptyList() throws Exception {
        cleanAll();
        String resp = sendCommand("GET-ITEMS", "{}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        // itemList có thể null hoặc empty
        if (obj.has("itemList") && !obj.get("itemList").isJsonNull()) {
            assertEquals(0, obj.getAsJsonArray("itemList").size());
        }
    }
}
