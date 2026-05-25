package com.group15.daugia.server.items;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SellItemIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
    }

    @Test
    @Order(1)
    void sellItemSuccess() throws Exception {
        String resp = sendCommand("SELL-ITEM",
                "{\"token\":\"tok-seller1\",\"name\":\"Widget\",\"price\":100.0,\"desc\":\"A widget\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(2)
    void sellItemMissingName() throws Exception {
        String resp = sendCommand("SELL-ITEM",
                "{\"token\":\"tok-seller1\",\"price\":100.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void sellItemPriceZero() throws Exception {
        String resp = sendCommand("SELL-ITEM",
                "{\"token\":\"tok-seller1\",\"name\":\"Widget\",\"price\":0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void sellItemNegativePrice() throws Exception {
        String resp = sendCommand("SELL-ITEM",
                "{\"token\":\"tok-seller1\",\"name\":\"Widget\",\"price\":-5}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void sellItemInvalidToken() throws Exception {
        String resp = sendCommand("SELL-ITEM",
                "{\"token\":\"bad-tok\",\"name\":\"Widget\",\"price\":100.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("400 Bad Request", obj.get("response").getAsString());
    }
}
