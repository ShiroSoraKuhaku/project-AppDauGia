package com.group15.daugia.server.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminBanUserIT extends BaseTest {

  @BeforeEach
  void setup() throws Exception {
    cleanAll();
    seedUserWithToken("admin", "admin", "tok-admin");
    seedUserWithToken("bob", "pass", "tok-bob");
    seedUserWithToken("alice", "pass", "tok-alice");
  }

  @Test
  @Order(1)
  void adminCanBanUserAndRevokeAccess() throws Exception {
    String resp = sendCommand("BAN-USER", "{\"token\":\"tok-admin\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("200 OK", obj.get("response").getAsString());

    String balanceResp = sendCommand("GET-BALANCE", "{\"token\":\"tok-bob\"}");
    JsonObject balanceObj = JsonParser.parseString(balanceResp).getAsJsonObject();
    assertEquals("401 Unauthorized", balanceObj.get("response").getAsString());

    String loginResp = sendCommand("LOGIN", "{\"username\":\"bob\",\"password\":\"pass\"}");
    JsonObject loginObj = JsonParser.parseString(loginResp).getAsJsonObject();
    assertEquals("403 Forbidden", loginObj.get("response").getAsString());
  }

  @Test
  @Order(2)
  void nonAdminCannotBanUser() throws Exception {
    String resp = sendCommand("BAN-USER", "{\"token\":\"tok-alice\",\"username\":\"bob\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("403 Forbidden", obj.get("response").getAsString());
  }

  @Test
  @Order(3)
  void cannotBanAdminAccount() throws Exception {
    String resp = sendCommand("BAN-USER", "{\"token\":\"tok-admin\",\"username\":\"admin\"}");
    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
    assertEquals("403 Forbidden", obj.get("response").getAsString());
  }
}
