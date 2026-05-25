package com.group15.daugia.server.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SignupIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
    }

    @Test
    @Order(1)
    void signupSuccess() throws Exception {
        String resp = sendCommand("SIGNUP", "{\"username\":\"newuser\",\"password\":\"pass\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(2)
    void signupDuplicate() throws Exception {
        sendCommand("SIGNUP", "{\"username\":\"bob\",\"password\":\"pass\"}");
        String resp = sendCommand("SIGNUP", "{\"username\":\"bob\",\"password\":\"pass\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("409 Conflict", obj.get("response").getAsString());
    }

    @Test
    @Order(3)
    void signupThenLogin() throws Exception {
        sendCommand("SIGNUP", "{\"username\":\"carol\",\"password\":\"secret\"}");
        String resp = sendCommand("LOGIN", "{\"username\":\"carol\",\"password\":\"secret\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }
}
