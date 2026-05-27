package com.group15.daugia.server.concurrency;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test anti-sniping (extend) khi bid trong 30s cuối.
 * Tạo auction với end_time = now + 20s để trigger extend path.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExtendNearEndConcurrencyIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("nearend1", "pass", "netok1");
        seedUserWithToken("nearend2", "pass", "netok2");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"netok1\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"netok2\",\"amount\":1000.0}");
        itemId = seedItem("nearend1", "Near End Item", 100.0);
        // Auction kết thúc sau 20s -> trigger pessimistic + anti-sniping
        auctionId = seedActiveAuction("Near End Auction", itemId, 100.0, 20);
    }

    @Test
    @Order(1)
    void bidNearEndTriggersPessimisticPath() throws Exception {
        String resp = sendCommand("PLACE-BID",
                "{\"token\":\"netok1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("201 Created", obj.get("response").getAsString());
    }

    @Test
    @Order(2)
    void bidNearEndExtendsEndTime() throws Exception {
        // Lấy end_time trước
        String endTimeBefore = getEndTime(auctionId);

        sendCommand("PLACE-BID",
                "{\"token\":\"netok1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        Thread.sleep(200);

        String endTimeAfter = getEndTime(auctionId);
        assertNotEquals(endTimeBefore, endTimeAfter,
                "End time should be extended after bid near end");
    }

    @Test
    @Order(3)
    void concurrentBidsNearEndNoDataLoss() throws Exception {
        Thread t1 = new Thread(() -> {
            try {
                sendCommand("PLACE-BID",
                        "{\"token\":\"netok1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        Thread t2 = new Thread(() -> {
            try {
                sendCommand("PLACE-BID",
                        "{\"token\":\"netok2\",\"auctionId\":" + auctionId + ",\"bidAmount\":201.0}");
            } catch (Exception e) { throw new RuntimeException(e); }
        });
        t1.start(); t2.start();
        t1.join(5000); t2.join(5000);

        // DB còn nhất quán: cur_price là giá cao nhất
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double curPrice = rs.getDouble("cur_price");
                assertTrue(curPrice >= 200.0, "cur_price should be at least 200.0");
            }
        }
    }

    private String getEndTime(int id) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT end_time FROM auctions WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString("end_time");
            }
        }
    }
}
