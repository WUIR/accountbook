package com.example.accountbook.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceUtils {

  private static final String FILE_BUDGET_CONFIG = "budget_config";
  private static final String KEY_MONTHLY_BUDGET = "monthly_budget";
  private static final String KEY_BUDGET_WARN_ENABLED = "budget_warn_enabled";
  private static final String KEY_HOME_BUDGET_MODE_ENABLED = "home_budget_mode_enabled";

  private PreferenceUtils() {
  }

  public static void saveBudgetConfig(
      Context context, double monthlyBudget, boolean budgetWarnEnabled) {
    SharedPreferences preferences = getPreferences(context);
    preferences.edit()
        .putFloat(KEY_MONTHLY_BUDGET, (float) monthlyBudget)
        .putBoolean(KEY_BUDGET_WARN_ENABLED, budgetWarnEnabled)
        .apply();
  }

  public static double getMonthlyBudget(Context context) {
    return getPreferences(context).getFloat(KEY_MONTHLY_BUDGET, 0);
  }

  public static boolean isBudgetWarnEnabled(Context context) {
    return getPreferences(context).getBoolean(KEY_BUDGET_WARN_ENABLED, true);
  }

  public static void saveHomeBudgetModeEnabled(Context context, boolean enabled) {
    getPreferences(context)
        .edit()
        .putBoolean(KEY_HOME_BUDGET_MODE_ENABLED, enabled)
        .apply();
  }

  public static boolean isHomeBudgetModeEnabled(Context context) {
    return getPreferences(context).getBoolean(KEY_HOME_BUDGET_MODE_ENABLED, false);
  }

  private static SharedPreferences getPreferences(Context context) {
    return context.getApplicationContext()
        .getSharedPreferences(FILE_BUDGET_CONFIG, Context.MODE_PRIVATE);
  }
}
