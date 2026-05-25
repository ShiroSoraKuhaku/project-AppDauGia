package com.group15.daugia.server.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LogoutIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("alice", "pass123", "tok-alice");
    }

    @Test
    @Order(1)
    void logoutSuccess() throws Exception {
        String resp = sendCommand("RM-TOKEN", "{\"username\":\"alice\",\"token\":\"tok-alice\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("204 No Content", obj.get("response").getAsString());
    }

    @Test
    @Order(2)
    void logoutInvalidToken() throws Exception {
        // Token sai – DAO.removeLogin trả null, LogoutWorker vẫn trả 204 (theo thiết kế hiện tại)
        String resp = sendCommand("RM-TOKEN", "{\"username\":\"alice\",\"token\":\"bad-token\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("204 No Content", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void logoutThenTokenInvalid() throws Exception {
        // Sau khi logout, token không còn hợp lệ cho các API khác
        sendCommand("RM-TOKEN", "{\"username\":\"alice\",\"token\":\"tok-alice\"}");
        String resp = sendCommand("GET-MY-ITEMS", "{\"token\":\"tok-alice\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }
}
