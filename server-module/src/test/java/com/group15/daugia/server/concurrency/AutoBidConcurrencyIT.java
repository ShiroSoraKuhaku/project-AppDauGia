package com.group15.daugia.server.concurrency;

import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AutoBidConcurrencyIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("abuser1", "pass", "abtok1");
        seedUserWithToken("abuser2", "pass", "abtok2");
        seedUserWithToken("abuser3", "pass", "abtok3");
        itemId = seedItem("abuser1", "AutoBid Item", 100.0);
        auctionId = seedActiveAuction("AutoBid Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void twoAutoBiddersConcurrentlyWinnerDetermined() throws Exception {
        // abuser1 max=300, abuser2 max=200 concurrently
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        futures.add(pool.submit(() -> {
            try {
                ready.countDown(); start.await();
                sendCommand("SET-AUTO-BID",
                        "{\"token\":\"abtok1\",\"auctionId\":" + auctionId + ",\"maxAmount\":300.0}");
            } catch (Exception e) { throw new RuntimeException(e); }
        }));
        futures.add(pool.submit(() -> {
            try {
                ready.countDown(); start.await();
                sendCommand("SET-AUTO-BID",
                        "{\"token\":\"abtok2\",\"auctionId\":" + auctionId + ",\"maxAmount\":200.0}");
            } catch (Exception e) { throw new RuntimeException(e); }
        }));

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // abuser1 có max cao hơn nên phải thắng
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT cur_leader FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("abuser1", rs.getString("cur_leader"),
                        "Highest auto-bidder should win");
            }
        }
    }

    @Test
    @Order(2)
    void autoBidAfterManualBidOutbids() throws Exception {
        // abuser3 đặt manual bid 150, abuser1 set auto-bid 300
        sendCommand("PLACE-BID",
                "{\"token\":\"abtok3\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        sendCommand("SET-AUTO-BID",
                "{\"token\":\"abtok1\",\"auctionId\":" + auctionId + ",\"maxAmount\":300.0}");

        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT cur_leader, cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals("abuser1", rs.getString("cur_leader"));
                assertTrue(rs.getDouble("cur_price") > 150.0);
            }
        }
    }
}
