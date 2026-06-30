package com.example.accountbook.model;

public class TrendItem {

  private final String label;
  private final double amount;

  public TrendItem(String label, double amount) {
    this.label = label;
    this.amount = amount;
  }

  public String getLabel() {
    return label;
  }

  public double getAmount() {
    return amount;
  }
}
