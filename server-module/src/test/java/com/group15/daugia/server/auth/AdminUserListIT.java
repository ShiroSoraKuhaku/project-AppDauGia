package com.group15.daugia.server.auth;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminUserListIT extends BaseTest {

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("admin", "admin", "tok-admin");
    seedUserWithToken("bob", "pass", "tok-bob");
    seedUser("carol", "pass");
    execSql("update user set is_banned = true where username = 'carol'");
  }

  @Test
  @Order(1)
  void adminCanGetAllUsers() throws Exception {
    String resp = sendCommand("GET-USERS", "{\"token\":\"tok-admin\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());

    JsonArray arr = obj.getAsJsonArray("userList");
    Map<String, JsonObject> map = new HashMap<>();
    for (int i = 0; i < arr.size(); i++) {
      JsonObject u = arr.get(i).getAsJsonObject();
      map.put(u.get("username").getAsString(), u);
    }

    assertTrue(map.containsKey("admin"));
    assertTrue(map.containsKey("bob"));
    assertTrue(map.containsKey("carol"));
    assertFalse(map.get("bob").get("banned").getAsBoolean());
    assertTrue(map.get("carol").get("banned").getAsBoolean());
  }

  @Test
  @Order(2)
  void nonAdminCannotGetUsers() throws Exception {
    String resp = sendCommand("GET-USERS", "{\"token\":\"tok-bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("403 Forbidden", obj.get("response").getAsString());
  }
}
