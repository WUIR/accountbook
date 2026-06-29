package com.example.accountbook.util;

import java.util.Locale;

public class MoneyUtils {

  private MoneyUtils() {
  }

  public static String format(double amount) {
    return String.format(Locale.CHINA, "¥%.2f", amount);
  }
}
