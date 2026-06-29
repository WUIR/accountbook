package com.example.accountbook.model;

public class CategorySummary {

  private String categoryName;
  private double amount;
  private double ratio;

  public CategorySummary(String categoryName, double amount, double ratio) {
    this.categoryName = categoryName;
    this.amount = amount;
    this.ratio = ratio;
  }

  public String getCategoryName() {
    return categoryName;
  }

  public double getAmount() {
    return amount;
  }

  public double getRatio() {
    return ratio;
  }
}
