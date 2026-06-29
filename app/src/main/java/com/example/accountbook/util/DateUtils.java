package com.example.accountbook.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
  public static final long RECYCLE_BIN_KEEP_MILLIS = 7L * 24L * 60L * 60L * 1000L;

  private DateUtils() {
  }

  public static String today() {
    return formatDate(Calendar.getInstance());
  }

  public static String formatDate(Calendar calendar) {
    return DATE_FORMAT.format(calendar.getTime());
  }

  public static String getWeekStart() {
    Calendar calendar = Calendar.getInstance();
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
    calendar.add(Calendar.DAY_OF_MONTH, diffToMonday);
    return formatDate(calendar);
  }

  public static String getNextWeekStart() {
    Calendar calendar = Calendar.getInstance();
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
    calendar.add(Calendar.DAY_OF_MONTH, diffToMonday);
    calendar.add(Calendar.DAY_OF_MONTH, 7);
    return formatDate(calendar);
  }

  public static String getMonthStart() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    return formatDate(calendar);
  }

  public static String getNextMonthStart() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MONTH, 1);
    return formatDate(calendar);
  }

  public static long getEarliestRecycleDeletedAt() {
    return System.currentTimeMillis() - RECYCLE_BIN_KEEP_MILLIS;
  }

  public static int getRecycleDaysLeft(long deletedAt) {
    long left = deletedAt + RECYCLE_BIN_KEEP_MILLIS - System.currentTimeMillis();
    return Math.max(0, (int) Math.ceil(left / (24D * 60D * 60D * 1000D)));
  }
}
