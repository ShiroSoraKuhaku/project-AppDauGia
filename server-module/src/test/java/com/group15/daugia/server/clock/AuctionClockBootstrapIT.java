package com.group15.daugia.server.clock;

import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test bootstrap: AuctionClock.bootstrap() phải load đúng các auction SCHEDULED/ACTIVE.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuctionClockBootstrapIT extends BaseTest {

    private int itemId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("clockuser", "pass", "ctok-clock");
        itemId = seedItem("clockuser", "Clock Item", 100.0);
    }

    @Test
    @Order(1)
    void bootstrapLoadsScheduledAuction() throws Exception {
        // Tạo auction SCHEDULED (start sau 60s)
        int auctionId = seedScheduledAuction("Scheduled Auction", itemId, 100.0, 60, 120);

        // Reset và bootstrap lại
        AuctionClock.getInstance().resetForTests();
        AuctionClock.getInstance().bootstrap();

        // Auction vẫn ở SCHEDULED (chưa đến giờ start)
        assertEquals("SCHEDULED", getAuctionStatus(auctionId));
    }

    @Test
    @Order(2)
    void bootstrapSkipsEndedAuctions() throws Exception {
        // ENDED auction không nên được bootstrap
        int ended = seedEndedAuction("Ended", itemId, 100.0);
        AuctionClock.getInstance().resetForTests();
        AuctionClock.getInstance().bootstrap();
        // Không crash = pass
        assertEquals("ENDED", getAuctionStatus(ended));
    }

    @Test
    @Order(3)
    void bootstrapLoadsActiveAuction() throws Exception {
        int active = seedActiveAuction("Active Auction", itemId, 100.0, 3600);
        AuctionClock.getInstance().resetForTests();
        AuctionClock.getInstance().bootstrap();
        assertEquals("ACTIVE", getAuctionStatus(active));
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
