package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.group15.daugia.server.DAO.BidDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONBidTemp;

public class BidWorker implements Workable {
    @Override
    public String work(String data){
        Gson gson = new Gson();
        JSONBidTemp ans = new JSONBidTemp();
        JSONBidTemp bid;

        try{
            bid = gson.fromJson(data, JSONBidTemp.class);
        }catch (JsonSyntaxException e){
            ans.setResponse("400 Bad Request");
            return gson.toJson(ans);
        }


        if (bid == null
                || bid.getToken() == null
                || bid.getToken().isBlank()
                || bid.getItemId() <= 0
                || !Double.isFinite(bid.getPrice())
                || bid.getPrice() <= 0){
            ans.setResponse("400 Bad Request");
            return gson.toJson(ans);
        }
        String username = UserDAO.getUserDao().getUsernameByToken(bid.getToken());

        if (username == null){
            ans.setResponse("400 Bad Request");
            return gson.toJson(ans);
        }

        String result = BidDAO.getBidDao().placeBid(bid.getItemId(), username, bid.getPrice());

        switch (result){
            case "OK" -> ans.setResponse("201 Created");
            case "INVALID_INPUT" -> ans.setResponse("400 Bad Request");
            case "ITEM_NOT_FOUND" -> ans.setResponse("404 Not Found");
            case "SELLER_CANNOT_BID" -> ans.setResponse("403 Forbidden");
            case "PRICE_TOO_LOW" -> ans.setResponse("409 Conflict");
            default -> ans.setResponse("500 Internal Server Error");
        }

        return gson.toJson(ans);
    }
}
