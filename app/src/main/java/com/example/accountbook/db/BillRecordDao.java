package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.accountbook.model.BillRecord;

import java.util.ArrayList;
import java.util.List;

public class BillRecordDao {

  private final AccountBookDbHelper dbHelper;
  private final AccountDao accountDao;

  public BillRecordDao(Context context) {
    Context appContext = context.getApplicationContext();
    dbHelper = new AccountBookDbHelper(appContext);
    accountDao = new AccountDao(appContext);
  }

  public long insertBillRecord(BillRecord record) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      long recordId = db.insert(AccountBookDbHelper.TABLE_BILL_RECORD, null, toContentValues(record));
      double delta = BillRecord.TYPE_INCOME.equals(record.getType())
          ? record.getAmount()
          : -record.getAmount();
      accountDao.updateBalance(db, record.getAccountId(), delta);
      db.setTransactionSuccessful();
      return recordId;
    } finally {
      db.endTransaction();
    }
  }

  public List<BillRecord> getRecentBillRecords(int limit) {
    List<BillRecord> records = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String sql = "SELECT br.*, c." + AccountBookDbHelper.COLUMN_NAME + " AS category_name, "
        + "a." + AccountBookDbHelper.COLUMN_NAME + " AS account_name "
        + "FROM " + AccountBookDbHelper.TABLE_BILL_RECORD + " br "
        + "LEFT JOIN " + AccountBookDbHelper.TABLE_CATEGORY + " c ON br."
        + AccountBookDbHelper.COLUMN_CATEGORY_ID + " = c." + AccountBookDbHelper.COLUMN_ID + " "
        + "LEFT JOIN " + AccountBookDbHelper.TABLE_ACCOUNT + " a ON br."
        + AccountBookDbHelper.COLUMN_ACCOUNT_ID + " = a." + AccountBookDbHelper.COLUMN_ID + " "
        + "ORDER BY br." + AccountBookDbHelper.COLUMN_RECORD_DATE + " DESC, br."
        + AccountBookDbHelper.COLUMN_CREATE_TIME + " DESC LIMIT ?";
    try (Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(limit)})) {
      while (cursor.moveToNext()) {
        records.add(readBillRecord(cursor));
      }
    }
    return records;
  }

  public double getMonthlyTotal(String type, String monthStart, String monthEnd) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String sql = "SELECT SUM(" + AccountBookDbHelper.COLUMN_AMOUNT + ") FROM "
        + AccountBookDbHelper.TABLE_BILL_RECORD
        + " WHERE " + AccountBookDbHelper.COLUMN_TYPE + " = ? AND "
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " >= ? AND "
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " < ?";
    try (Cursor cursor = db.rawQuery(sql, new String[] {type, monthStart, monthEnd})) {
      if (cursor.moveToFirst()) {
        return cursor.isNull(0) ? 0 : cursor.getDouble(0);
      }
    }
    return 0;
  }

  private ContentValues toContentValues(BillRecord record) {
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_TYPE, record.getType());
    values.put(AccountBookDbHelper.COLUMN_AMOUNT, record.getAmount());
    values.put(AccountBookDbHelper.COLUMN_CATEGORY_ID, record.getCategoryId());
    values.put(AccountBookDbHelper.COLUMN_ACCOUNT_ID, record.getAccountId());
    values.put(AccountBookDbHelper.COLUMN_RECORD_DATE, record.getRecordDate());
    values.put(AccountBookDbHelper.COLUMN_REMARK, record.getRemark());
    values.put(AccountBookDbHelper.COLUMN_CREATE_TIME, record.getCreateTime());
    return values;
  }

  private BillRecord readBillRecord(Cursor cursor) {
    BillRecord record = new BillRecord();
    record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    record.setType(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_TYPE)));
    record.setAmount(
        cursor.getDouble(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_AMOUNT)));
    record.setCategoryId(
        cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_CATEGORY_ID)));
    record.setAccountId(
        cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ACCOUNT_ID)));
    record.setRecordDate(
        cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_RECORD_DATE)));
    record.setRemark(
        cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_REMARK)));
    record.setCreateTime(
        cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_CREATE_TIME)));
    record.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_name")));
    record.setAccountName(cursor.getString(cursor.getColumnIndexOrThrow("account_name")));
    return record;
  }
}
