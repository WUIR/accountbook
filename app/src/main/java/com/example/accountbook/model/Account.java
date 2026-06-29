package com.example.accountbook.model;

public class Account {

  public static final String TYPE_CASH = "cash";
  public static final String TYPE_BANK_CARD = "bank_card";
  public static final String TYPE_CREDIT_CARD = "credit_card";
  public static final String TYPE_THIRD_PARTY = "third_party";
  public static final String TYPE_OTHER = "other";

  private long id;
  private String name;
  private String accountType;
  private double balance;
  private boolean active = true;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAccountType() {
    return accountType;
  }

  public void setAccountType(String accountType) {
    this.accountType = accountType;
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
