package com.example.accountbook.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.accountbook.model.Account;

import java.util.ArrayList;
import java.util.List;

public class AccountDao {

  private final AccountBookDbHelper dbHelper;

  public AccountDao(Context context) {
    dbHelper = new AccountBookDbHelper(context.getApplicationContext());
  }

  public List<Account> getAllAccounts() {
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

  public void updateBalance(SQLiteDatabase db, long accountId, double delta) {
    db.execSQL(
        "UPDATE " + AccountBookDbHelper.TABLE_ACCOUNT
            + " SET " + AccountBookDbHelper.COLUMN_BALANCE + " = "
            + AccountBookDbHelper.COLUMN_BALANCE + " + ? WHERE "
            + AccountBookDbHelper.COLUMN_ID + " = ?",
        new Object[] {delta, accountId});
  }

  private Account readAccount(Cursor cursor) {
    Account account = new Account();
    account.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    account.setName(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_NAME)));
    account.setAccountType(
        cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ACCOUNT_TYPE)));
    account.setBalance(
        cursor.getDouble(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_BALANCE)));
    return account;
  }
}
