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
  void getItemsEmptyReturnsEmptyList() throws Exception {
    cleanAll();
    seedUserWithToken("seller1", "pass", "tok-seller1");
    String resp = sendCommand("GET-ITEMS", "{\"token\":\"tok-seller1\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
    if (obj.has("itemList") && !obj.get("itemList").isJsonNull()) {
      assertEquals(0, obj.getAsJsonArray("itemList").size());
    }
  }

  @Test
  @Order(2)
  void getItemsNoTokenReturns401() throws Exception {
    String resp = sendCommand("GET-ITEMS", "{}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("401 Unauthorized", obj.get("response").getAsString());
  }

  @Test
  @Order(3)
  void getItemsInvalidTokenReturns401() throws Exception {
    String resp = sendCommand("GET-ITEMS", "{\"token\":\"bad-tok\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("401 Unauthorized", obj.get("response").getAsString());
  }

  @Test
  @Order(4)
  void getItemsAliasGetItem() throws Exception {
    String resp = sendCommand("GET-ITEM", "{\"token\":\"tok-seller1\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());
  }
}
