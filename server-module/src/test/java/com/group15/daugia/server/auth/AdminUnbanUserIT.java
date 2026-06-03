package com.group15.daugia.server.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminUnbanUserIT extends BaseTest {

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("admin", "admin", "tok-admin");
    seedUserWithToken("bob", "pass", "tok-bob");
  }

  @Test
  @Order(1)
  void adminCanUnbanBannedUser() throws Exception {
    // Ban bob trước
    sendCommand("BAN-USER", "{\"token\":\"tok-admin\",\"username\":\"bob\"}");

    // Unban bob
    String resp = sendCommand("UNBAN-USER", "{\"token\":\"tok-admin\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());

    // Bob phải đăng nhập được lại
    String loginResp = sendCommand("LOGIN", "{\"username\":\"bob\",\"password\":\"pass\"}");
    JsonObject loginObj = JsonParser.parseString(loginResp).getAsJsonObject();
    assertEquals("201 Created", loginObj.get("response").getAsString());
  }

  @Test
  @Order(2)
  void unbanUserNotBannedReturns409() throws Exception {
    // bob chưa bị ban
    String resp = sendCommand("UNBAN-USER", "{\"token\":\"tok-admin\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("409 Conflict", obj.get("response").getAsString());
  }

  @Test
  @Order(3)
  void nonAdminCannotUnban() throws Exception {
    sendUserWithToken_seeded("alice");
    sendCommand("BAN-USER", "{\"token\":\"tok-admin\",\"username\":\"bob\"}");

    String resp = sendCommand("UNBAN-USER", "{\"token\":\"tok-alice\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("403 Forbidden", obj.get("response").getAsString());
  }

  @Test
  @Order(4)
  void unbanNonExistentUserReturns404() throws Exception {
    String resp = sendCommand("UNBAN-USER", "{\"token\":\"tok-admin\",\"username\":\"nobody\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("404 Not Found", obj.get("response").getAsString());
  }

  @Test
  @Order(5)
  void unbanInvalidTokenReturns401() throws Exception {
    String resp = sendCommand("UNBAN-USER", "{\"token\":\"bad-tok\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("401 Unauthorized", obj.get("response").getAsString());
  }

  private void sendUserWithToken_seeded(String username) throws Exception {
    seedUserWithToken(username, "pass", "tok-" + username);
  }
}
