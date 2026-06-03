package com.group15.daugia.server.auction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.group15.daugia.server.base.BaseTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetMyAuctionHistoryIT extends BaseTest {

    @BeforeEach
    void setup() throws Exception {
        cleanAll();
        seedUserWithToken("seller1", "pass", "tok-seller1");
        seedUserWithToken("bidder1", "pass", "tok-bidder1");
        seedUserWithToken("bidder2", "pass", "tok-bidder2");
    }

    @Test
    @Order(1)
    void returnsAuctionsWhereUserHasBid() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        int item1 = seedItem("seller1", "Item A", 100.0);
        int auctionId = seedActiveAuction("Auction A", item1, 100.0, 3600);

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");

        String resp = sendCommand("GET-MY-AUCTION-HISTORY",
                "{\"token\":\"tok-bidder1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());

        JsonArray auctions = obj.get("auctions").getAsJsonArray();
        assertEquals(1, auctions.size());
        assertEquals(auctionId, auctions.get(0).getAsJsonObject().get("auctionId").getAsInt());
    }

    @Test
    @Order(2)
    void returnsEmptyForUserWithNoBids() throws Exception {
        String resp = sendCommand("GET-MY-AUCTION-HISTORY",
                "{\"token\":\"tok-bidder2\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        assertEquals(0, obj.get("auctions").getAsJsonArray().size());
    }

    @Test
    @Order(3)
    void myTopBidIsCorrect() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        int item1 = seedItem("seller1", "Item B", 100.0);
        int auctionId = seedActiveAuction("Auction B", item1, 100.0, 3600);

        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":150.0}");
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder2\",\"amount\":1000.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder2\",\"auctionId\":" + auctionId + ",\"bidAmount\":200.0}");
        sendCommand("PLACE-BID",
                "{\"token\":\"tok-bidder1\",\"auctionId\":" + auctionId + ",\"bidAmount\":250.0}");

        String resp = sendCommand("GET-MY-AUCTION-HISTORY",
                "{\"token\":\"tok-bidder1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        JsonObject auction = obj.get("auctions").getAsJsonArray().get(0).getAsJsonObject();
        assertEquals(250.0, auction.get("myTopBid").getAsDouble(), 0.001);
    }

    @Test
    @Order(4)
    void includesEndedAuctions() throws Exception {
        sendCommand("TOPUP-BALANCE", "{\"token\":\"tok-bidder1\",\"amount\":1000.0}");
        int item1 = seedItem("seller1", "Item C", 100.0);
        int auctionId = seedEndedAuction("Ended Auction", item1, 100.0);
        // Seed bid trực tiếp vào DB vì auction đã ENDED
        execSql("INSERT INTO auction_bids (auction_id, bidder_username, bid_amount) VALUES ("
                + auctionId + ", 'bidder1', 150.0)");

        String resp = sendCommand("GET-MY-AUCTION-HISTORY",
                "{\"token\":\"tok-bidder1\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("200 OK", obj.get("response").getAsString());
        JsonArray auctions = obj.get("auctions").getAsJsonArray();
        assertEquals(1, auctions.size());
        assertEquals("ENDED", auctions.get(0).getAsJsonObject().get("status").getAsString());
    }

    @Test
    @Order(5)
    void invalidTokenReturns401() throws Exception {
        String resp = sendCommand("GET-MY-AUCTION-HISTORY",
                "{\"token\":\"bad-tok\"}");
        JsonObject obj = JsonParser.parseString(resp).getAsJsonObject();
        assertEquals("401 Unauthorized", obj.get("response").getAsString());
    }
}
