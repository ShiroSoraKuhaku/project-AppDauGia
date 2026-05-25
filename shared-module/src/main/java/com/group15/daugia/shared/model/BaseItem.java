package com.group15.daugia.shared.model;

public class BaseItem {
  String id;
  String name;
  double price;
  String description;
  String startTime;
  String endTime;

  public BaseItem() {}

  public BaseItem(String id, String name, double price, String description) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.description = description;
  }

  public BaseItem(
      String id, String name, double price, String description, String startTime, String endTime) {
    this(id, name, price, description);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public double getPrice() {
    return price;
  }

  public String getName() {
    return name;
  }

  public String getStartTime() {
    return startTime;
  }

  public String getEndTime() {
    return endTime;
  }
}
