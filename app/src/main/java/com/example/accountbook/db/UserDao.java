package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.accountbook.model.User;

public class UserDao {

  private final AccountBookDbHelper dbHelper;

  public UserDao(Context context) {
    dbHelper = new AccountBookDbHelper(context);
  }

  public long insertUser(User user) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_USERNAME, user.getUsername());
    values.put(AccountBookDbHelper.COLUMN_PASSWORD_HASH, user.getPasswordHash());
    values.put(AccountBookDbHelper.COLUMN_PASSWORD_SALT, user.getPasswordSalt());
    values.put(AccountBookDbHelper.COLUMN_NICKNAME, user.getNickname());
    values.put(AccountBookDbHelper.COLUMN_AVATAR_PATH, user.getAvatarPath());
    values.put(AccountBookDbHelper.COLUMN_SIGNATURE, user.getSignature());
    values.put(AccountBookDbHelper.COLUMN_ROLE_LABEL, user.getRoleLabel());
    values.put(AccountBookDbHelper.COLUMN_CREATED_AT, user.getCreatedAt());
    values.put(AccountBookDbHelper.COLUMN_UPDATED_AT, user.getUpdatedAt());
    values.put(AccountBookDbHelper.COLUMN_LAST_LOGIN_AT, user.getLastLoginAt());
    return db.insert(AccountBookDbHelper.TABLE_USER, null, values);
  }

  public User getUserByUsername(String username) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_USER,
        null,
        AccountBookDbHelper.COLUMN_USERNAME + " = ?",
        new String[] { username },
        null,
        null,
        null);
    try {
      if (cursor.moveToFirst()) {
        return readUser(cursor);
      }
      return null;
    } finally {
      cursor.close();
    }
  }

  public User getUserById(long id) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_USER,
        null,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] { String.valueOf(id) },
        null,
        null,
        null);
    try {
      if (cursor.moveToFirst()) {
        return readUser(cursor);
      }
      return null;
    } finally {
      cursor.close();
    }
  }

  public boolean isUsernameExists(String username) {
    return getUserByUsername(username) != null;
  }

  public void updateLastLoginAt(long userId, long lastLoginAt) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_LAST_LOGIN_AT, lastLoginAt);
    values.put(AccountBookDbHelper.COLUMN_UPDATED_AT, lastLoginAt);
    db.update(
        AccountBookDbHelper.TABLE_USER,
        values,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] { String.valueOf(userId) });
  }

  private User readUser(Cursor cursor) {
    User user = new User();
    user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_USERNAME)));
    user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_PASSWORD_HASH)));
    user.setPasswordSalt(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_PASSWORD_SALT)));
    user.setNickname(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_NICKNAME)));
    user.setAvatarPath(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_AVATAR_PATH)));
    user.setSignature(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_SIGNATURE)));
    user.setRoleLabel(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ROLE_LABEL)));
    user.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_CREATED_AT)));
    user.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_UPDATED_AT)));
    user.setLastLoginAt(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_LAST_LOGIN_AT)));
    return user;
  }
}
