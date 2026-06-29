package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.accountbook.model.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountDao {

  private final AccountBookDbHelper dbHelper;

  public AccountDao(Context context) {
    dbHelper = new AccountBookDbHelper(context.getApplicationContext());
  }

  public List<Account> getAllAccounts() {
    return getAllAccountsIncludingInactive();
  }

  public List<Account> getActiveAccounts() {
    List<Account> accounts = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_ACCOUNT,
        null,
        AccountBookDbHelper.COLUMN_IS_ACTIVE + " = 1",
        null,
        null,
        null,
        AccountBookDbHelper.COLUMN_ID + " ASC")) {
      while (cursor.moveToNext()) {
        accounts.add(readAccount(cursor));
      }
    }
    return accounts;
  }

  public List<Account> getAllAccountsIncludingInactive() {
    List<Account> accounts = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_ACCOUNT,
        null,
        null,
        null,
        null,
        null,
        AccountBookDbHelper.COLUMN_ID + " ASC")) {
      while (cursor.moveToNext()) {
        accounts.add(readAccount(cursor));
      }
    }
    return accounts;
  }

  public Account getAccountById(long accountId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_ACCOUNT,
        null,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(accountId)},
        null,
        null,
        null)) {
      if (cursor.moveToFirst()) {
        return readAccount(cursor);
      }
    }
    return null;
  }

  public long insertAccount(Account account) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    return db.insert(AccountBookDbHelper.TABLE_ACCOUNT, null, toContentValues(account, false));
  }

  public boolean updateAccount(Account account) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.update(
        AccountBookDbHelper.TABLE_ACCOUNT,
        toContentValues(account, true),
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(account.getId())});
    return rows == 1;
  }

  public boolean deactivateAccount(long accountId) {
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_IS_ACTIVE, 0);
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.update(
        AccountBookDbHelper.TABLE_ACCOUNT,
        values,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(accountId)});
    return rows == 1;
  }

  public boolean deleteAccountIfUnused(long accountId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.delete(
        AccountBookDbHelper.TABLE_ACCOUNT,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(accountId)});
    return rows == 1;
  }

  public boolean existsActiveAccountName(String name, long excludeId) {
    if (TextUtils.isEmpty(name)) {
      return false;
    }
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_ACCOUNT,
        new String[] {AccountBookDbHelper.COLUMN_ID},
        AccountBookDbHelper.COLUMN_NAME + " = ? AND "
            + AccountBookDbHelper.COLUMN_IS_ACTIVE + " = 1 AND "
            + AccountBookDbHelper.COLUMN_ID + " != ?",
        new String[] {name, String.valueOf(excludeId)},
        null,
        null,
        null)) {
      return cursor.moveToFirst();
    }
  }

  public void updateBalance(SQLiteDatabase db, long accountId, double delta) {
    db.execSQL(
        "UPDATE " + AccountBookDbHelper.TABLE_ACCOUNT
            + " SET " + AccountBookDbHelper.COLUMN_BALANCE + " = "
            + AccountBookDbHelper.COLUMN_BALANCE + " + ? WHERE "
            + AccountBookDbHelper.COLUMN_ID + " = ?",
        new Object[] {delta, accountId});
  }

  private ContentValues toContentValues(Account account, boolean includeActive) {
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_NAME, account.getName());
    values.put(AccountBookDbHelper.COLUMN_ACCOUNT_TYPE, account.getAccountType());
    values.put(AccountBookDbHelper.COLUMN_BALANCE, account.getBalance());
    if (includeActive) {
      values.put(AccountBookDbHelper.COLUMN_IS_ACTIVE, account.isActive() ? 1 : 0);
    } else {
      values.put(AccountBookDbHelper.COLUMN_IS_ACTIVE, 1);
    }
    return values;
  }

  private Account readAccount(Cursor cursor) {
    Account account = new Account();
    account.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    account.setName(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_NAME)));
    account.setAccountType(
        cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ACCOUNT_TYPE)));
    account.setBalance(
        cursor.getDouble(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_BALANCE)));
    int activeIndex = cursor.getColumnIndex(AccountBookDbHelper.COLUMN_IS_ACTIVE);
    account.setActive(activeIndex < 0 || cursor.getInt(activeIndex) == 1);
    return account;
  }
}
