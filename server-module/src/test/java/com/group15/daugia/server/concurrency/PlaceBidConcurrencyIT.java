package com.group15.daugia.server.concurrency;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaceBidConcurrencyIT extends BaseTest {

    private int itemId;
    private int auctionId;

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        for (int i = 1; i <= 5; i++) {
            seedUserWithToken("cbidder" + i, "pass", "ctok" + i);
            sendCommand("TOPUP-BALANCE", "{\"token\":\"ctok" + i + "\",\"amount\":1000.0}");
        }
        itemId = seedItem("cbidder1", "Concurrent Item", 100.0);
        auctionId = seedActiveAuction("Concurrent Auction", itemId, 100.0, 3600);
    }

    @Test
    @Order(1)
    void concurrentBidsSamePriceOnlyOneWins() throws Exception {
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 1; i <= threads; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    String resp = sendCommand("PLACE-BID",
                            "{\"token\":\"ctok" + idx + "\",\"auctionId\":" + auctionId + ",\"bidAmount\":500.0}");
                    JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
                    String response = obj.get("response").getAsString();
                    if ("201 Created".equals(response)) successCount.incrementAndGet();
                    else if ("409 Conflict".equals(response) || "400 Bad Request".equals(response))
                        conflictCount.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // Đúng 1 bid thắng
        assertEquals(1, successCount.get(), "Exactly one bid should succeed");
        assertEquals(threads - 1, conflictCount.get(), "Rest should conflict/fail");

        // Giá cuối là 500
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(500.0, rs.getDouble("cur_price"), 0.01);
            }
        }
    }

    @Test
    @Order(2)
    void concurrentBidsAscendingPricesAllCommit() throws Exception {
        // 5 bid tăng dần, tất cả đều hợp lệ về giá (mỗi cái cao hơn cái trước)
        // Vì sequential trong thực tế, test này kiểm tra không có deadlock/exception
        for (int i = 1; i <= 5; i++) {
            double price = 100.0 + i * 50;
            String resp = sendCommand("PLACE-BID",
                    "{\"token\":\"ctok" + i + "\",\"auctionId\":" + auctionId + ",\"bidAmount\":" + price + "}");
            JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
            assertEquals("201 Created", obj.get("response").getAsString());
        }

        // Giá cuối = 350 (100 + 5*50)
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement("SELECT cur_price FROM auctions WHERE id=?")) {
            ps.setInt(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertEquals(350.0, rs.getDouble("cur_price"), 0.01);
            }
        }
    }
}
