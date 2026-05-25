package com.group15.daugia.shared.model;

public class BaseItem {
  String id;
  String name;
  double price;
  String description;

  public BaseItem() {}

  public BaseItem(String id, String name, double price, String description) {
    this.id = id;
    this.name = name;
    this.price = price;
    this.description = description;
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
}
