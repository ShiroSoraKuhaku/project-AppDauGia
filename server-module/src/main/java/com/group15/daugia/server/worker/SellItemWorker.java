package com.group15.daugia.server.worker;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;
import com.group15.daugia.server.DAO.UserDAO;
import com.group15.daugia.server.Workable;
import com.group15.daugia.shared.JSONItemTemp;

public class SellItemWorker implements Workable {
    @Override
    public String work(String data){
        Gson gson = new Gson();
        JSONItemTemp item = gson.fromJson(data, JSONItemTemp.class);

        String sellerUsername = UserDAO.getUserDao().getUsernameByToken(item.getToken());

        if (sellerUsername == null){
            return "0";
        }

        if (item.getName() == null || item.getName().isBlank() || item.getPrice() <= 0){
            return "0";
        }

        return ItemDAO.getItemDao().addItem(sellerUsername, item.getName(), item.getPrice(), item.getDesc());
    }
}
