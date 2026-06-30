package com.example.accountbook.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class UserSessionUtils {

  private static final String FILE_USER_SESSION = "user_session";
  private static final String KEY_IS_LOGGED_IN = "is_logged_in";
  private static final String KEY_CURRENT_USER_ID = "current_user_id";
  private static final String KEY_LAST_LOGIN_USERNAME = "last_login_username";
  private static final long NO_USER_ID = -1;

  private UserSessionUtils() {
  }

  public static void saveLoginSession(Context context, long userId, String username) {
    getPreferences(context)
        .edit()
        .putBoolean(KEY_IS_LOGGED_IN, true)
        .putLong(KEY_CURRENT_USER_ID, userId)
        .putString(KEY_LAST_LOGIN_USERNAME, username)
        .apply();
  }

  public static void clearLoginSession(Context context) {
    SharedPreferences preferences = getPreferences(context);
    String lastLoginUsername = preferences.getString(KEY_LAST_LOGIN_USERNAME, "");
    preferences
        .edit()
        .putBoolean(KEY_IS_LOGGED_IN, false)
        .putLong(KEY_CURRENT_USER_ID, NO_USER_ID)
        .putString(KEY_LAST_LOGIN_USERNAME, lastLoginUsername)
        .apply();
  }

  public static boolean isLoggedIn(Context context) {
    return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
        && getCurrentUserId(context) != NO_USER_ID;
  }

  public static long getCurrentUserId(Context context) {
    return getPreferences(context).getLong(KEY_CURRENT_USER_ID, NO_USER_ID);
  }

  public static String getLastLoginUsername(Context context) {
    return getPreferences(context).getString(KEY_LAST_LOGIN_USERNAME, "");
  }

  private static SharedPreferences getPreferences(Context context) {
    return context.getApplicationContext()
        .getSharedPreferences(FILE_USER_SESSION, Context.MODE_PRIVATE);
  }
}
