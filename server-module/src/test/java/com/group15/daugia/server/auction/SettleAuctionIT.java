package com.group15.daugia.server.auction;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.AuctionClock;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm tra settleAuction: trừ tiền winner, cộng tiền seller, release holds.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SettleAuctionIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedUserWithToken("bidder1", "pass", "tok-bidder1");
        seedUserWithToken("bidder2", "pass", "tok-bidder2");
        AuctionClock.getInstance().resetForTests();
    }

    @Test
    @Order(1)
    void winnerBalanceDeductedAndSellerCredited() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        int itemId = seedItem("seller1", "Laptop", 100.0);
        int auctionId = seedActiveAuction("Laptop Auction", itemId, 100.0, 3);

        // bidder1 đặt bid 500
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":500.0}");

        AuctionClock.getInstance().bootstrap();
        Thread.sleep(4500); // chờ auction kết thúc + settle

        // bidder1 balance: 1000 - 500 = 500
        double bidder1Balance = getBalance("bidder1");
        assertEquals(500.0, bidder1Balance, 1.0);

        // seller1 balance: 0 + 500 = 500
        double seller1Balance = getBalance("seller1");
        assertEquals(500.0, seller1Balance, 1.0);
    }

    @Test
    @Order(2)
    void losingBidderHoldsReleasedAfterSettle() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder2\",\"amount\":1000.0}");
        int itemId = seedItem("seller1", "Phone", 100.0);
        int auctionId = seedActiveAuction("Phone Auction", itemId, 100.0, 3);

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":300.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder2\",\"auctionId\":" + auctionId + ",\"bidAmount\":400.0}");

        AuctionClock.getInstance().bootstrap();
        Thread.sleep(4500);

        // bidder1 thua -> locked phải = 0
        double bidder1Locked = getLockedBalance("bidder1");
        assertEquals(0.0, bidder1Locked, 0.001);

        // bidder1 balance không đổi (không trừ tiền)
        double bidder1Balance = getBalance("bidder1");
        assertEquals(1000.0, bidder1Balance, 1.0);
    }

    @Test
    @Order(3)
    void noWinnerAuctionSellerNotCredited() throws Exception {
        int itemId = seedItem("seller1", "No Bids Item", 100.0);
        seedActiveAuction("No Bids Auction", itemId, 100.0, 3);

        AuctionClock.getInstance().bootstrap();
        Thread.sleep(4500);

        // seller1 balance vẫn = 0 (không có winner)
        double seller1Balance = getBalance("seller1");
        assertEquals(0.0, seller1Balance, 0.001);
    }

    private double getBalance(String username) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT balance FROM user WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
                return -1;
            }
        }
    }

    private double getLockedBalance(String username) throws Exception {
        try (Connection c = getJdbcConn();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT locked_balance FROM user WHERE username=?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("locked_balance");
                return -1;
            }
        }
    }
}
