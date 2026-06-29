package com.example.accountbook.model;

public class SummaryResult {

  private final double income;
  private final double expense;

  public SummaryResult(double income, double expense) {
    this.income = income;
    this.expense = expense;
  }

  public double getIncome() {
    return income;
  }

  public double getExpense() {
    return expense;
  }

  public double getBalance() {
    return income - expense;
  }
}
