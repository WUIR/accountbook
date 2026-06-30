package com.example.accountbook.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class LoginRememberUtils {

  private static final String FILE_LOGIN_REMEMBER_CONFIG = "login_remember_config";
  private static final String KEY_REMEMBER_PASSWORD_ENABLED = "remember_password_enabled";
  private static final String KEY_REMEMBER_USERNAME = "remember_username";
  private static final String KEY_REMEMBER_PASSWORD_VALUE = "remember_password_value";
  private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
  private static final String AES_KEY = "AccountBookKey16";
  private static final String AES_IV = "AccountBookIv_16";

  private LoginRememberUtils() {
  }

  public static void saveRememberedPassword(Context context, String username, String password) {
    getPreferences(context)
        .edit()
        .putBoolean(KEY_REMEMBER_PASSWORD_ENABLED, true)
        .putString(KEY_REMEMBER_USERNAME, username)
        .putString(KEY_REMEMBER_PASSWORD_VALUE, encrypt(password))
        .apply();
  }

  public static void clearRememberedPassword(Context context, String username) {
    getPreferences(context)
        .edit()
        .putBoolean(KEY_REMEMBER_PASSWORD_ENABLED, false)
        .putString(KEY_REMEMBER_USERNAME, username)
        .remove(KEY_REMEMBER_PASSWORD_VALUE)
        .apply();
  }

  public static boolean isRememberPasswordEnabled(Context context) {
    return getPreferences(context).getBoolean(KEY_REMEMBER_PASSWORD_ENABLED, false);
  }

  public static String getRememberedUsername(Context context) {
    return getPreferences(context).getString(KEY_REMEMBER_USERNAME, "");
  }

  public static String getRememberedPassword(Context context) {
    return decrypt(getPreferences(context).getString(KEY_REMEMBER_PASSWORD_VALUE, ""));
  }

  private static SharedPreferences getPreferences(Context context) {
    return context.getApplicationContext()
        .getSharedPreferences(FILE_LOGIN_REMEMBER_CONFIG, Context.MODE_PRIVATE);
  }

  private static String encrypt(String plainText) {
    try {
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
          new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8)));
      return Base64.encodeToString(
          cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)),
          Base64.NO_WRAP);
    } catch (Exception e) {
      return "";
    }
  }

  private static String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isEmpty()) {
      return "";
    }
    try {
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES"),
          new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8)));
      byte[] decrypted = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP));
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      return "";
    }
  }
}
