package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

  private final AccountBookDbHelper dbHelper;

  public CategoryDao(Context context) {
    dbHelper = new AccountBookDbHelper(context.getApplicationContext());
  }

  public List<Category> getCategoriesByType(String type) {
    return getActiveCategoriesByType(type);
  }

  public List<Category> getActiveCategoriesByType(String type) {
    List<Category> categories = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_CATEGORY,
        null,
        AccountBookDbHelper.COLUMN_TYPE + " = ? AND " + AccountBookDbHelper.COLUMN_IS_ACTIVE + " = 1",
        new String[] {type},
        null,
        null,
        AccountBookDbHelper.COLUMN_SORT_ORDER + " ASC")) {
      while (cursor.moveToNext()) {
        categories.add(readCategory(cursor));
      }
    }
    return categories;
  }

  public List<Category> getAllCategories() {
    return getAllCategoriesIncludingInactive();
  }

  public List<Category> getAllCategoriesIncludingInactive() {
    List<Category> categories = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_CATEGORY,
        null,
        null,
        null,
        null,
        null,
        AccountBookDbHelper.COLUMN_TYPE + " ASC, "
            + AccountBookDbHelper.COLUMN_SORT_ORDER + " ASC")) {
      while (cursor.moveToNext()) {
        categories.add(readCategory(cursor));
      }
    }
    return categories;
  }

  public Category getCategoryById(long categoryId) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_CATEGORY,
        null,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(categoryId)},
        null,
        null,
        null)) {
      if (cursor.moveToFirst()) {
        return readCategory(cursor);
      }
    }
    return null;
  }

  public long insertCategory(Category category) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    if (category.getSortOrder() <= 0) {
      category.setSortOrder(getNextSortOrder(category.getType()));
    }
    return db.insert(AccountBookDbHelper.TABLE_CATEGORY, null, toContentValues(category, false));
  }

  public boolean updateCategory(Category category) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.update(
        AccountBookDbHelper.TABLE_CATEGORY,
        toContentValues(category, true),
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(category.getId())});
    return rows == 1;
  }

  public boolean deactivateCategory(long categoryId) {
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_IS_ACTIVE, 0);
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.update(
        AccountBookDbHelper.TABLE_CATEGORY,
        values,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(categoryId)});
    return rows == 1;
  }

  public boolean deleteCategoryIfUnused(long categoryId) {
    SQLiteDatabase db = dbHelper.getWritableDatabase();
    int rows = db.delete(
        AccountBookDbHelper.TABLE_CATEGORY,
        AccountBookDbHelper.COLUMN_ID + " = ?",
        new String[] {String.valueOf(categoryId)});
    return rows == 1;
  }

  public boolean existsActiveCategoryName(String type, String name, long excludeId) {
    if (TextUtils.isEmpty(type) || TextUtils.isEmpty(name)) {
      return false;
    }
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_CATEGORY,
        new String[] {AccountBookDbHelper.COLUMN_ID},
        AccountBookDbHelper.COLUMN_TYPE + " = ? AND "
            + AccountBookDbHelper.COLUMN_NAME + " = ? AND "
            + AccountBookDbHelper.COLUMN_IS_ACTIVE + " = 1 AND "
            + AccountBookDbHelper.COLUMN_ID + " != ?",
        new String[] {type, name, String.valueOf(excludeId)},
        null,
        null,
        null)) {
      return cursor.moveToFirst();
    }
  }

  private int getNextSortOrder(String type) {
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.rawQuery(
        "SELECT MAX(" + AccountBookDbHelper.COLUMN_SORT_ORDER + ") FROM "
            + AccountBookDbHelper.TABLE_CATEGORY + " WHERE "
            + AccountBookDbHelper.COLUMN_TYPE + " = ?",
        new String[] {TextUtils.isEmpty(type) ? BillRecord.TYPE_EXPENSE : type})) {
      if (cursor.moveToFirst() && !cursor.isNull(0)) {
        return cursor.getInt(0) + 1;
      }
    }
    return 1;
  }

  private ContentValues toContentValues(Category category, boolean includeActive) {
    ContentValues values = new ContentValues();
    values.put(AccountBookDbHelper.COLUMN_NAME, category.getName());
    values.put(AccountBookDbHelper.COLUMN_TYPE, category.getType());
    values.put(AccountBookDbHelper.COLUMN_SORT_ORDER, category.getSortOrder());
    values.put(AccountBookDbHelper.COLUMN_IS_ACTIVE, includeActive && !category.isActive() ? 0 : 1);
    return values;
  }

  private Category readCategory(Cursor cursor) {
    Category category = new Category();
    category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    category.setName(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_NAME)));
    category.setType(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_TYPE)));
    category.setSortOrder(
        cursor.getInt(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_SORT_ORDER)));
    int activeIndex = cursor.getColumnIndex(AccountBookDbHelper.COLUMN_IS_ACTIVE);
    category.setActive(activeIndex < 0 || cursor.getInt(activeIndex) == 1);
    return category;
  }
}
