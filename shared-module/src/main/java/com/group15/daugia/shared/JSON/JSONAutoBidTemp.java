package com.group15.daugia.shared.JSON;

import com.google.gson.annotations.SerializedName;

public class JSONAutoBidTemp extends JSONTemp{
    @SerializedName(value = "auctionId", alternate = {"auctionID"})
    private int auctionId;
    private String token;
    private double maxAmount;

    private String bidderUsername;

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public int getAuctionID() {
        return getAuctionId();
    }

    public void setAuctionID(int auctionID) {
        setAuctionId(auctionID);
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
