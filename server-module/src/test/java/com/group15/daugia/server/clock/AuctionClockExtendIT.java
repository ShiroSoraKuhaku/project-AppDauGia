package com.group15.daugia.server.clock;

import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test anti-sniping extension qua AuctionClock.tryExtend().
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionClockExtendIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("extuser1", "pass", "extok1");
        seedUserWithToken("extuser2", "pass", "extok2");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"extok1\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"extok2\",\"amount\":1000.0}");
        itemId = seedItem("extuser1", "Extend Item", 100.0);
        // Auction với 15s còn lại -> trigger anti-sniping
        auctionId = seedActiveAuction("Extend Auction", itemId, 100.0, 15);
        AuctionClock.getInstance().resetForTests();
        AuctionClock.getInstance().bootstrap();
    }

    @Test
    @Order(1)
    void bidNearEndExtendsViaAuctionClock() throws Exception {
        String endBefore = getEndTime(auctionId);

        sendCommand("PLACE-BID",
                "{\"token\":\"extok1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        Thread.sleep(500);

        String endAfter = getEndTime(auctionId);
        assertNotEquals(endBefore, endAfter,
                "AuctionClock.tryExtend() should extend end_time when bid placed near end");
    }

    @Test
    @Order(2)
    void bidFarFromEndDoesNotExtend() throws Exception {
        // Tạo auction với 3600s còn lại -> không extend
        int farItem = seedItem("extuser1", "Far Item", 100.0);
        int farAuction = seedActiveAuction("Far Auction", farItem, 100.0, 3600);
        String endBefore = getEndTime(farAuction);

        sendCommand("PLACE-BID",
                "{\"token\":\"extok2\",\"auctionId\":" + farAuction + ",\"bidAmount\":200.0}");
        Thread.sleep(300);

        String endAfter = getEndTime(farAuction);
        assertEquals(endBefore, endAfter,
                "End time should NOT change when bid placed far from end");
    }

    @Test
    @Order(3)
    void extendedAuctionNotEndedEarly() throws Exception {
        // Bid near end -> extend -> auction còn thêm 30s, không nên ENDED ngay
        sendCommand("PLACE-BID",
                "{\"token\":\"extok1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        Thread.sleep(2000);

        String status = getAuctionStatus(auctionId);
        assertEquals("ACTIVE", status,
                "After extension, auction should still be ACTIVE");
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

    private String getAuctionStatus(int id) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT status FROM auctions WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString("status");
            }
        }
    }
}
