package com.group15.daugia.server.clock;

import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test vòng đời SCHEDULED -> ACTIVE -> ENDED thông qua AuctionClock.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionClockStartEndIT extends BaseTest {

    private int itemId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("clockuser2", "pass", "ctok-clock2");
        itemId = seedItem("clockuser2", "Clock Item2", 100.0);
        AuctionClock.getInstance().resetForTests();
    }

    @Test
    @Order(1)
    void auctionTransitionsFromScheduledToActive() throws Exception {
        // start sau 2s, end sau 60s
        int auctionId = seedScheduledAuction("Fast Start", itemId, 100.0, 2, 62);
        AuctionClock.getInstance().bootstrap();

        Thread.sleep(3500); // chờ start job chạy

        assertEquals("ACTIVE", getAuctionStatus(auctionId),
                "Auction should transition to ACTIVE after start_time");
    }

    @Test
    @Order(2)
    void auctionTransitionsFromActiveToEnded() throws Exception {
        // Auction đã ACTIVE, end sau 3s
        int auctionId = seedActiveAuction("Fast End", itemId, 100.0, 3);
        AuctionClock.getInstance().bootstrap();

        Thread.sleep(4500); // chờ end job chạy

        assertEquals("ENDED", getAuctionStatus(auctionId),
                "Auction should transition to ENDED after end_time");
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
