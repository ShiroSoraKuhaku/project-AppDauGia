package com.group15.daugia.shared.JSON;

import java.util.List;

public class JSONUserListTemp extends JSONTemp {
  private List<JSONUserInfoTemp> userList;

  public List<JSONUserInfoTemp> getUserList() {
    return userList;
  }

  public void setUserList(List<JSONUserInfoTemp> userList) {
    this.userList = userList;
  }
}
