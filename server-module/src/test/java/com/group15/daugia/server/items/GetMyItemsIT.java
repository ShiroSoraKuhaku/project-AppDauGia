package com.group15.daugia.server.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetMyItemsIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("sellerA", "pass", "tok-sellerA");
        seedUserWithToken("sellerB", "pass", "tok-sellerB");
        seedItem("sellerA", "A-Item1", 10.0);
        seedItem("sellerA", "A-Item2", 20.0);
        seedItem("sellerB", "B-Item1", 30.0);
    }

    @Test
    @Order(1)
    void getMyItemsReturnsOnlyMine() throws Exception {
        String resp = sendCommand("GET-MY-ITEMS", "{\"token\":\"tok-sellerA\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        JsonArray list = obj.getAsJsonArray("itemList");
        assertEquals(2, list.size());
        for (var el : list) {
            assertEquals("sellerA", el.getAsJsonObject().get("sellerUsername").getAsString());
        }
    }

    @Test
    @Order(2)
    void getMyItemsInvalidToken() throws Exception {
        String resp = sendCommand("GET-MY-ITEMS", "{\"token\":\"bad-tok\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void getMyItemsMissingToken() throws Exception {
        String resp = sendCommand("GET-MY-ITEMS", "{}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }
}
