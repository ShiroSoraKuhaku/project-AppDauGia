package com.group15.daugia.shared.JSON;

public class JSONBidTemp extends JSONTemp{
    private int itemId;
    private double price;
    private String token;

    public int getItemId(){
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
