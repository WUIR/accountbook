package com.example.accountbook.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

  private static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
  private static final SimpleDateFormat PERIOD_DATE_FORMAT =
      new SimpleDateFormat("yyyy.MM.dd", Locale.CHINA);
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
    return getWeekStart(Calendar.getInstance());
  }

  public static String getWeekStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
    calendar.add(Calendar.DAY_OF_MONTH, diffToMonday);
    return formatDate(calendar);
  }

  public static String getNextWeekStart() {
    return getNextWeekStart(Calendar.getInstance());
  }

  public static String getNextWeekStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
    int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
    calendar.add(Calendar.DAY_OF_MONTH, diffToMonday);
    calendar.add(Calendar.DAY_OF_MONTH, 7);
    return formatDate(calendar);
  }

  public static String getMonthStart() {
    return getMonthStart(Calendar.getInstance());
  }

  public static String getMonthStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    return formatDate(calendar);
  }

  public static String getNextMonthStart() {
    return getNextMonthStart(Calendar.getInstance());
  }

  public static String getNextMonthStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MONTH, 1);
    return formatDate(calendar);
  }

  public static String getYearStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    return formatDate(calendar);
  }

  public static String getNextYearStart(Calendar baseCalendar) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(baseCalendar.getTime());
    calendar.set(Calendar.MONTH, Calendar.JANUARY);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.YEAR, 1);
    return formatDate(calendar);
  }

  public static String formatWeekPeriod(Calendar baseCalendar) {
    Calendar start = Calendar.getInstance();
    start.setTime(baseCalendar.getTime());
    int dayOfWeek = start.get(Calendar.DAY_OF_WEEK);
    int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
    start.add(Calendar.DAY_OF_MONTH, diffToMonday);
    Calendar end = Calendar.getInstance();
    end.setTime(start.getTime());
    end.add(Calendar.DAY_OF_MONTH, 6);
    return PERIOD_DATE_FORMAT.format(start.getTime()) + " - " + PERIOD_DATE_FORMAT.format(end.getTime());
  }

  public static String formatMonthPeriod(Calendar baseCalendar) {
    return String.format(
        Locale.CHINA,
        "%04d年%02d月",
        baseCalendar.get(Calendar.YEAR),
        baseCalendar.get(Calendar.MONTH) + 1);
  }

  public static String formatYearPeriod(Calendar baseCalendar) {
    return String.format(Locale.CHINA, "%04d年", baseCalendar.get(Calendar.YEAR));
  }

  public static long getEarliestRecycleDeletedAt() {
    return System.currentTimeMillis() - RECYCLE_BIN_KEEP_MILLIS;
  }

  public static int getRecycleDaysLeft(long deletedAt) {
    long left = deletedAt + RECYCLE_BIN_KEEP_MILLIS - System.currentTimeMillis();
    return Math.max(0, (int) Math.ceil(left / (24D * 60D * 60D * 1000D)));
  }
}
