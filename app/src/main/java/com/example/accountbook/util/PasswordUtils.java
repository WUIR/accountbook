package com.example.accountbook.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class PasswordUtils {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private PasswordUtils() {
  }

  public static String generateSalt() {
    byte[] salt = new byte[16];
    SECURE_RANDOM.nextBytes(salt);
    return toHex(salt);
  }

  public static String hashPassword(String password, String salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest((salt + password).getBytes(StandardCharsets.UTF_8));
      return toHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }

  public static boolean verifyPassword(String password, String salt, String expectedHash) {
    return hashPassword(password, salt).equals(expectedHash);
  }

  private static String toHex(byte[] bytes) {
    StringBuilder builder = new StringBuilder(bytes.length * 2);
    for (byte value : bytes) {
      builder.append(String.format("%02x", value));
    }
    return builder.toString();
  }
}
