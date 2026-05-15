package com.group15.daugia.shared.JSON;

import java.util.List;

public class JSONItemListTemp extends JSONTemp {
  List<JSONItemTemp> itemList;

  public List<JSONItemTemp> getItemList() {
    return itemList;
  }

  public void setItemList(List<JSONItemTemp> itemList) {
    this.itemList = itemList;
  }
}
