package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSON.JSONItemListTemp;
import com.group15.daugia.shared.JSON.JSONItemTemp;

/**
 * GET-MY-ITEMS: lấy danh sách item của người bán hiện tại.
 *
 * <p>Request JSON: { "token": "..." }
 * <p>Response JSON: { "response": "200 OK", "itemList": [...] }
 *   { "response": "400 Bad Request" } nếu thiếu / sai payload
 *   { "response": "401 Unauthorized" } nếu token sai
 *
 * <p>Mỗi phần tử trong itemList có các field: id, sellerUsername, name, price, desc, startTime,
 * endTime.
 */
public class GetMyItemsWorker implements Workable {
    @Override
    public String work(String data){
        Gson gson = new Gson();
        JSONItemListTemp ans = new JSONItemListTemp();
        JSONItemTemp request;

        try {
            request = gson.fromJson(data, JSONItemTemp.class);
        } catch (JsonSyntaxException e) {
            ans.setResponse("400 Bad Request");
            return gson.toJson(ans);
        }

        if(request == null || request.getToken() == null || request.getToken().isBlank()){
            ans.setResponse("400 Bad Request");
            return gson.toJson(ans);
        }

        String sellerUsername = UserDAO.getUserDao().getUsernameByToken(request.getToken());

        if(sellerUsername == null){
            ans.setResponse("401 Unauthorized");
            return gson.toJson(ans);
        }

        ans.setResponse("200 OK");
        ans.setItemList(ItemDAO.getItemDao().getItemsBySeller(sellerUsername));
        return gson.toJson(ans);


    }
}
