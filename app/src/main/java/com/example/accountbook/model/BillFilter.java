package com.example.accountbook.model;

public class BillFilter {

  private String startDateInclusive;
  private String endDateExclusive;
  private String type;
  private Long categoryId;
  private Long accountId;

  public String getStartDateInclusive() {
    return startDateInclusive;
  }

  public void setStartDateInclusive(String startDateInclusive) {
    this.startDateInclusive = startDateInclusive;
  }

  public String getEndDateExclusive() {
    return endDateExclusive;
  }

  public void setEndDateExclusive(String endDateExclusive) {
    this.endDateExclusive = endDateExclusive;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }
}
