package com.group15.daugia.server;

import com.google.gson.Gson;
import com.group15.daugia.server.DAO.ItemDAO;

public class GetItemsWorker implements Workable{
    @Override
    public String work(String data){
        return new Gson().toJson(ItemDAO.getItemDao().getAllItems());
    }
}
