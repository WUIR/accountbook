package com.example.accountbook.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.accountbook.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryDao {

  private final AccountBookDbHelper dbHelper;

  public CategoryDao(Context context) {
    dbHelper = new AccountBookDbHelper(context.getApplicationContext());
  }

  public List<Category> getCategoriesByType(String type) {
    List<Category> categories = new ArrayList<>();
    SQLiteDatabase db = dbHelper.getReadableDatabase();
    try (Cursor cursor = db.query(
        AccountBookDbHelper.TABLE_CATEGORY,
        null,
        AccountBookDbHelper.COLUMN_TYPE + " = ?",
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

  private Category readCategory(Cursor cursor) {
    Category category = new Category();
    category.setId(cursor.getLong(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_ID)));
    category.setName(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_NAME)));
    category.setType(cursor.getString(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_TYPE)));
    category.setSortOrder(
        cursor.getInt(cursor.getColumnIndexOrThrow(AccountBookDbHelper.COLUMN_SORT_ORDER)));
    return category;
  }
}
