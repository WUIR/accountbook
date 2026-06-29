package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.accountbook.model.BillFilter;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.CategorySummary;
import com.example.accountbook.model.SummaryResult;

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
        + "WHERE br." + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0 "
        + "ORDER BY br." + AccountBookDbHelper.COLUMN_RECORD_DATE + " DESC, br."
        + AccountBookDbHelper.COLUMN_CREATE_TIME + " DESC LIMIT ?";
    try (Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(limit)})) {
      while (cursor.moveToNext()) {
        records.add(readBillRecord(cursor));
      }
    }
    return records;
  }

  public List<BillRecord> getBillRecordsByFilter(BillFilter filter) {
    List<BillRecord> records = new ArrayList<>();
    List<String> args = new ArrayList<>();
    StringBuilder sql = new StringBuilder(baseSelectSql())
        .append("WHERE br.")
        .append(AccountBookDbHelper.COLUMN_DELETED_AT)
        .append(" = 0 ");
    if (filter != null) {
      if (!TextUtils.isEmpty(filter.getStartDateInclusive())) {
        sql.append("AND br.").append(AccountBookDbHelper.COLUMN_RECORD_DATE).append(" >= ? ");
        args.add(filter.getStartDateInclusive());
      }
      if (!TextUtils.isEmpty(filter.getEndDateExclusive())) {
        sql.append("AND br.").append(AccountBookDbHelper.COLUMN_RECORD_DATE).append(" < ? ");
        args.add(filter.getEndDateExclusive());
      }
      if (!TextUtils.isEmpty(filter.getType())) {
        sql.append("AND br.").append(AccountBookDbHelper.COLUMN_TYPE).append(" = ? ");
        args.add(filter.getType());
      }
      if (filter.getCategoryId() != null) {
        sql.append("AND br.").append(AccountBookDbHelper.COLUMN_CATEGORY_ID).append(" = ? ");
        args.add(String.valueOf(filter.getCategoryId()));
      }
      if (filter.getAccountId() != null) {
        sql.append("AND br.").append(AccountBookDbHelper.COLUMN_ACCOUNT_ID).append(" = ? ");
        args.add(String.valueOf(filter.getAccountId()));
      }
    }
    sql.append("ORDER BY br.")
        .append(AccountBookDbHelper.COLUMN_RECORD_DATE)
        .append(" DESC, br.")
        .append(AccountBookDbHelper.COLUMN_CREATE_TIME)
        .append(" DESC");
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
      while (cursor.moveToNext()) {
        records.add(readBillRecord(cursor));
      }
    }
    return records;
  }

  public BillRecord getBillRecordById(long id, boolean includeDeleted) {
    String sql = baseSelectSql() + "WHERE br." + AccountBookDbHelper.COLUMN_ID + " = ?";
    List<String> args = new ArrayList<>();
    args.add(String.valueOf(id));
    if (!includeDeleted) {
      sql += " AND br." + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0";
    }
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
      if (cursor.moveToFirst()) {
        return readBillRecord(cursor);
      }
    }
    return null;
  }

  public boolean updateBillRecord(BillRecord newRecord) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      BillRecord oldRecord = getBillRecordById(db, newRecord.getId(), false);
      if (oldRecord == null) {
        return false;
      }
      accountDao.updateBalance(db, oldRecord.getAccountId(), -getBalanceDelta(oldRecord));
      ContentValues values = toContentValues(newRecord);
      int rows = db.update(
          AccountBookDbHelper.TABLE_BILL_RECORD,
          values,
          AccountBookDbHelper.COLUMN_ID + " = ? AND "
              + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0",
          new String[] {String.valueOf(newRecord.getId())});
      if (rows == 1) {
        accountDao.updateBalance(db, newRecord.getAccountId(), getBalanceDelta(newRecord));
        db.setTransactionSuccessful();
        return true;
      }
      return false;
    } finally {
      db.endTransaction();
    }
  }

  public boolean moveToRecycleBin(long id, long deletedAt) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      BillRecord record = getBillRecordById(db, id, false);
      if (record == null) {
        return false;
      }
      ContentValues values = new ContentValues();
      values.put(AccountBookDbHelper.COLUMN_DELETED_AT, deletedAt);
      int rows = db.update(
          AccountBookDbHelper.TABLE_BILL_RECORD,
          values,
          AccountBookDbHelper.COLUMN_ID + " = ? AND "
              + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0",
          new String[] {String.valueOf(id)});
      if (rows == 1) {
        accountDao.updateBalance(db, record.getAccountId(), -getBalanceDelta(record));
        db.setTransactionSuccessful();
        return true;
      }
      return false;
    } finally {
      db.endTransaction();
    }
  }

  public boolean restoreFromRecycleBin(long id) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    db.beginTransaction();
    try {
      BillRecord record = getBillRecordById(db, id, true);
      if (record == null || record.getDeletedAt() <= 0) {
        return false;
      }
      ContentValues values = new ContentValues();
      values.put(AccountBookDbHelper.COLUMN_DELETED_AT, 0);
      int rows = db.update(
          AccountBookDbHelper.TABLE_BILL_RECORD,
          values,
          AccountBookDbHelper.COLUMN_ID + " = ? AND "
              + AccountBookDbHelper.COLUMN_DELETED_AT + " > 0",
          new String[] {String.valueOf(id)});
      if (rows == 1) {
        accountDao.updateBalance(db, record.getAccountId(), getBalanceDelta(record));
        db.setTransactionSuccessful();
        return true;
      }
      return false;
    } finally {
      db.endTransaction();
    }
  }

  public boolean permanentlyDelete(long id) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.delete(
        AccountBookDbHelper.TABLE_BILL_RECORD,
        AccountBookDbHelper.COLUMN_ID + " = ? AND "
            + AccountBookDbHelper.COLUMN_DELETED_AT + " > 0",
        new String[] {String.valueOf(id)});
    return rows == 1;
  }

  public boolean hasRecordsByAccountId(long accountId) {
    return hasRecordsByColumn(AccountBookDbHelper.COLUMN_ACCOUNT_ID, accountId);
  }

  public boolean hasRecordsByCategoryId(long categoryId) {
    return hasRecordsByColumn(AccountBookDbHelper.COLUMN_CATEGORY_ID, categoryId);
  }

  public List<BillRecord> getNormalBillRecordsByDateRange(
      String startDateInclusive,
      String endDateExclusive) {
    BillFilter filter = new BillFilter();
    filter.setStartDateInclusive(startDateInclusive);
    filter.setEndDateExclusive(endDateExclusive);
    return getBillRecordsByFilter(filter);
  }

  public int cleanupExpiredRecycleBinRecords(long expiredBefore) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    return db.delete(
        AccountBookDbHelper.TABLE_BILL_RECORD,
        AccountBookDbHelper.COLUMN_DELETED_AT + " > 0 AND "
            + AccountBookDbHelper.COLUMN_DELETED_AT + " < ?",
        new String[] {String.valueOf(expiredBefore)});
  }

  public List<BillRecord> getRecycleBinRecords(long earliestDeletedAt) {
    List<BillRecord> records = new ArrayList<>();
    String sql = baseSelectSql()
        + "WHERE br." + AccountBookDbHelper.COLUMN_DELETED_AT + " >= ? "
        + "ORDER BY br." + AccountBookDbHelper.COLUMN_DELETED_AT + " DESC";
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(earliestDeletedAt)})) {
      while (cursor.moveToNext()) {
        records.add(readBillRecord(cursor));
      }
    }
    return records;
  }

  public List<BillRecord> getExpiredRecycleBinRecords(long expiredBefore) {
    List<BillRecord> records = new ArrayList<>();
    String sql = baseSelectSql()
        + "WHERE br." + AccountBookDbHelper.COLUMN_DELETED_AT + " > 0 AND br."
        + AccountBookDbHelper.COLUMN_DELETED_AT + " < ? "
        + "ORDER BY br." + AccountBookDbHelper.COLUMN_DELETED_AT + " DESC";
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.rawQuery(sql, new String[] {String.valueOf(expiredBefore)})) {
      while (cursor.moveToNext()) {
        records.add(readBillRecord(cursor));
      }
    }
    return records;
  }

  public double getMonthlyTotal(String type, String monthStart, String monthEnd) {
    return getTotal(type, monthStart, monthEnd);
  }

  public double getTotal(String type, String startDateInclusive, String endDateExclusive) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String sql = "SELECT SUM(" + AccountBookDbHelper.COLUMN_AMOUNT + ") FROM "
        + AccountBookDbHelper.TABLE_BILL_RECORD
        + " WHERE " + AccountBookDbHelper.COLUMN_TYPE + " = ? AND "
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " >= ? AND "
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " < ? AND "
        + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0";
    try (Cursor cursor = db.rawQuery(sql, new String[] {type, startDateInclusive, endDateExclusive})) {
      if (cursor.moveToFirst()) {
        return cursor.isNull(0) ? 0 : cursor.getDouble(0);
      }
    }
    return 0;
  }

  public SummaryResult getSummary(String startDateInclusive, String endDateExclusive) {
    return new SummaryResult(
        getTotal(BillRecord.TYPE_INCOME, startDateInclusive, endDateExclusive),
        getTotal(BillRecord.TYPE_EXPENSE, startDateInclusive, endDateExclusive));
  }

  public List<CategorySummary> getExpenseCategorySummary(
      String startDateInclusive,
      String endDateExclusive) {
    List<CategorySummary> summaries = new ArrayList<>();
    double totalExpense = getTotal(BillRecord.TYPE_EXPENSE, startDateInclusive, endDateExclusive);
    if (totalExpense <= 0) {
      return summaries;
    }
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    String sql = "SELECT c." + AccountBookDbHelper.COLUMN_NAME + " AS category_name, "
        + "SUM(br." + AccountBookDbHelper.COLUMN_AMOUNT + ") AS total_amount "
        + "FROM " + AccountBookDbHelper.TABLE_BILL_RECORD + " br "
        + "LEFT JOIN " + AccountBookDbHelper.TABLE_CATEGORY + " c ON br."
        + AccountBookDbHelper.COLUMN_CATEGORY_ID + " = c." + AccountBookDbHelper.COLUMN_ID + " "
        + "WHERE br." + AccountBookDbHelper.COLUMN_TYPE + " = ? AND br."
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " >= ? AND br."
        + AccountBookDbHelper.COLUMN_RECORD_DATE + " < ? AND br."
        + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0 "
        + "GROUP BY br." + AccountBookDbHelper.COLUMN_CATEGORY_ID + " "
        + "HAVING total_amount > 0 "
        + "ORDER BY total_amount DESC";
    try (Cursor cursor = db.rawQuery(
        sql,
        new String[] {BillRecord.TYPE_EXPENSE, startDateInclusive, endDateExclusive})) {
      while (cursor.moveToNext()) {
        String categoryName = cursor.getString(cursor.getColumnIndexOrThrow("category_name"));
        double amount = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
        summaries.add(new CategorySummary(categoryName, amount, amount / totalExpense));
      }
    }
    return summaries;
  }

  private BillRecord getBillRecordById(SQLiteDatabase db, long id, boolean includeDeleted) {
    String sql = baseSelectSql() + "WHERE br." + AccountBookDbHelper.COLUMN_ID + " = ?";
    List<String> args = new ArrayList<>();
    args.add(String.valueOf(id));
    if (!includeDeleted) {
      sql += " AND br." + AccountBookDbHelper.COLUMN_DELETED_AT + " = 0";
    }
    try (Cursor cursor = db.rawQuery(sql, args.toArray(new String[0]))) {
      if (cursor.moveToFirst()) {
        return readBillRecord(cursor);
      }
    }
    return null;
  }

  private String baseSelectSql() {
    return "SELECT br.*, c." + AccountBookDbHelper.COLUMN_NAME + " AS category_name, "
        + "a." + AccountBookDbHelper.COLUMN_NAME + " AS account_name "
        + "FROM " + AccountBookDbHelper.TABLE_BILL_RECORD + " br "
        + "LEFT JOIN " + AccountBookDbHelper.TABLE_CATEGORY + " c ON br."
        + AccountBookDbHelper.COLUMN_CATEGORY_ID + " = c." + AccountBookDbHelper.COLUMN_ID + " "
        + "LEFT JOIN " + AccountBookDbHelper.TABLE_ACCOUNT + " a ON br."
        + AccountBookDbHelper.COLUMN_ACCOUNT_ID + " = a." + AccountBookDbHelper.COLUMN_ID + " ";
  }

  private double getBalanceDelta(BillRecord record) {
    return BillRecord.TYPE_INCOME.equals(record.getType()) ? record.getAmount() : -record.getAmount();
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
    values.put(AccountBookDbHelper.COLUMN_DELETED_AT, record.getDeletedAt());
    values.put(AccountBookDbHelper.COLUMN_IMAGE_PATH, record.getImagePath());
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
    record.setDeletedAt(
        cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_DELETED_AT)));
    int imagePathIndex = cursor.getColumnIndex(AccountBookDbHelper.COLUMN_IMAGE_PATH);
    if (imagePathIndex >= 0) {
      record.setImagePath(cursor.getString(imagePathIndex));
    }
    record.setCategoryName(cursor.getString(cursor.getColumnIndexOrThrow("category_name")));
    record.setAccountName(cursor.getString(cursor.getColumnIndexOrThrow("account_name")));
    return record;
  }

  private boolean hasRecordsByColumn(String columnName, long id) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_BILL_RECORD,
        new String[] {AccountBookDbHelper.COLUMN_ID},
        columnName + " = ?",
        new String[] {String.valueOf(id)},
        null,
        null,
        null,
        "1")) {
      return cursor.moveToFirst();
    }
  }
}
