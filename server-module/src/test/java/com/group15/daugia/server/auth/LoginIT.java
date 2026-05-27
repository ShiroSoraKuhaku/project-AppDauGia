package com.group15.daugia.server.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUser("alice", "pass123");
    }

    @Test
    @Order(1)
    void loginSuccess() throws Exception {
        String resp = sendCommand("LOGIN", "{\"username\":\"alice\",\"password\":\"pass123\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
        assertNotNull(obj.get("token"));
        assertFalse(obj.get("token").getAsString().isBlank());
        assertEquals("alice", obj.get("username").getAsString());
    }

    @Test
    @Order(2)
    void loginWrongPassword() throws Exception {
        String resp = sendCommand("LOGIN", "{\"username\":\"alice\",\"password\":\"wrong\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void loginUnknownUser() throws Exception {
        String resp = sendCommand("LOGIN", "{\"username\":\"nobody\",\"password\":\"pass\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }

    @Test
    @Order(4)
    void loginMissingFields() throws Exception {
        // username null -> LoginWorker sẽ ném NPE hoặc trả 401 (user không tồn tại)
        String resp = sendCommand("LOGIN", "{\"password\":\"pass123\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        // Không phải 201
        assertNotEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(5)
    void loginSingleSessionConflict() throws Exception {
        // alice đã có token "tok-alice" -> login phải bị 409
        seedUserWithToken("alice", "pass123", "tok-alice");
        String resp = sendCommand("LOGIN", "{\"username\":\"alice\",\"password\":\"pass123\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("409 Conflict", obj.get("response").getAsString());
    }

    @Test
    @Order(6)
    void loginAfterLogoutSucceeds() throws Exception {
        // Logout trước
        sendCommand("RM-TOKEN", "{\"username\":\"alice\",\"token\":\"tok-alice\"}");
        // Login lại phải thành công
        String resp = sendCommand("LOGIN", "{\"username\":\"alice\",\"password\":\"pass123\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }
}
