package com.example.accountbook.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.accountbook.model.Account;
import com.example.accountbook.model.BillRecord;

public class AccountBookDbHelper extends SQLiteOpenHelper {

  public static final String DATABASE_NAME = "account_book.db";
  public static final int DATABASE_VERSION = 4;

  public static final String TABLE_BILL_RECORD = "bill_record";
  public static final String TABLE_CATEGORY = "category";
  public static final String TABLE_ACCOUNT = "account";
  public static final String TABLE_USER = "user";

  public static final String COLUMN_ID = "id";
  public static final String COLUMN_TYPE = "type";
  public static final String COLUMN_AMOUNT = "amount";
  public static final String COLUMN_CATEGORY_ID = "category_id";
  public static final String COLUMN_ACCOUNT_ID = "account_id";
  public static final String COLUMN_RECORD_DATE = "record_date";
  public static final String COLUMN_REMARK = "remark";
  public static final String COLUMN_CREATE_TIME = "create_time";
  public static final String COLUMN_DELETED_AT = "deleted_at";
  public static final String COLUMN_IMAGE_PATH = "image_path";
  public static final String COLUMN_NAME = "name";
  public static final String COLUMN_SORT_ORDER = "sort_order";
  public static final String COLUMN_ACCOUNT_TYPE = "account_type";
  public static final String COLUMN_BALANCE = "balance";
  public static final String COLUMN_IS_ACTIVE = "is_active";
  public static final String COLUMN_USERNAME = "username";
  public static final String COLUMN_PASSWORD_HASH = "password_hash";
  public static final String COLUMN_PASSWORD_SALT = "password_salt";
  public static final String COLUMN_NICKNAME = "nickname";
  public static final String COLUMN_AVATAR_PATH = "avatar_path";
  public static final String COLUMN_SIGNATURE = "signature";
  public static final String COLUMN_ROLE_LABEL = "role_label";
  public static final String COLUMN_CREATED_AT = "created_at";
  public static final String COLUMN_UPDATED_AT = "updated_at";
  public static final String COLUMN_LAST_LOGIN_AT = "last_login_at";

  public AccountBookDbHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE " + TABLE_BILL_RECORD + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_TYPE + " TEXT NOT NULL, "
        + COLUMN_AMOUNT + " REAL NOT NULL, "
        + COLUMN_CATEGORY_ID + " INTEGER NOT NULL, "
        + COLUMN_ACCOUNT_ID + " INTEGER NOT NULL, "
        + COLUMN_RECORD_DATE + " TEXT NOT NULL, "
        + COLUMN_REMARK + " TEXT, "
        + COLUMN_CREATE_TIME + " INTEGER NOT NULL, "
        + COLUMN_DELETED_AT + " INTEGER NOT NULL DEFAULT 0, "
        + COLUMN_IMAGE_PATH + " TEXT"
        + ")");
    db.execSQL("CREATE TABLE " + TABLE_CATEGORY + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_NAME + " TEXT NOT NULL, "
        + COLUMN_TYPE + " TEXT NOT NULL, "
        + COLUMN_SORT_ORDER + " INTEGER NOT NULL, "
        + COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1"
        + ")");
    db.execSQL("CREATE TABLE " + TABLE_ACCOUNT + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_NAME + " TEXT NOT NULL, "
        + COLUMN_ACCOUNT_TYPE + " TEXT NOT NULL, "
        + COLUMN_BALANCE + " REAL NOT NULL DEFAULT 0, "
        + COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1"
        + ")");
    createUserTable(db);
    insertDefaultCategories(db);
    insertDefaultAccounts(db);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      db.execSQL("ALTER TABLE " + TABLE_BILL_RECORD
          + " ADD COLUMN " + COLUMN_DELETED_AT + " INTEGER NOT NULL DEFAULT 0");
    }
    if (oldVersion < 3) {
      db.execSQL("ALTER TABLE " + TABLE_BILL_RECORD
          + " ADD COLUMN " + COLUMN_IMAGE_PATH + " TEXT");
      db.execSQL("ALTER TABLE " + TABLE_ACCOUNT
          + " ADD COLUMN " + COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1");
      db.execSQL("ALTER TABLE " + TABLE_CATEGORY
          + " ADD COLUMN " + COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1");
    }
    if (oldVersion < 4) {
      createUserTable(db);
    }
  }

  private void createUserTable(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER + " ("
        + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
        + COLUMN_USERNAME + " TEXT NOT NULL UNIQUE, "
        + COLUMN_PASSWORD_HASH + " TEXT NOT NULL, "
        + COLUMN_PASSWORD_SALT + " TEXT NOT NULL, "
        + COLUMN_NICKNAME + " TEXT NOT NULL, "
        + COLUMN_AVATAR_PATH + " TEXT, "
        + COLUMN_SIGNATURE + " TEXT, "
        + COLUMN_ROLE_LABEL + " TEXT NOT NULL DEFAULT '普通用户', "
        + COLUMN_CREATED_AT + " INTEGER NOT NULL, "
        + COLUMN_UPDATED_AT + " INTEGER NOT NULL, "
        + COLUMN_LAST_LOGIN_AT + " INTEGER"
        + ")");
    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_user_username ON "
        + TABLE_USER + "(" + COLUMN_USERNAME + ")");
  }

  private void insertDefaultCategories(SQLiteDatabase db) {
    insertCategory(db, "餐饮", BillRecord.TYPE_EXPENSE, 1);
    insertCategory(db, "交通", BillRecord.TYPE_EXPENSE, 2);
    insertCategory(db, "购物", BillRecord.TYPE_EXPENSE, 3);
    insertCategory(db, "娱乐", BillRecord.TYPE_EXPENSE, 4);
    insertCategory(db, "学习", BillRecord.TYPE_EXPENSE, 5);
    insertCategory(db, "其他", BillRecord.TYPE_EXPENSE, 6);
    insertCategory(db, "生活费", BillRecord.TYPE_INCOME, 1);
    insertCategory(db, "工资", BillRecord.TYPE_INCOME, 2);
    insertCategory(db, "兼职", BillRecord.TYPE_INCOME, 3);
    insertCategory(db, "红包", BillRecord.TYPE_INCOME, 4);
    insertCategory(db, "其他", BillRecord.TYPE_INCOME, 5);
  }

  private void insertCategory(SQLiteDatabase db, String name, String type, int sortOrder) {
    ContentValues values = new ContentValues();
    values.put(COLUMN_NAME, name);
    values.put(COLUMN_TYPE, type);
    values.put(COLUMN_SORT_ORDER, sortOrder);
    values.put(COLUMN_IS_ACTIVE, 1);
    db.insert(TABLE_CATEGORY, null, values);
  }

  private void insertDefaultAccounts(SQLiteDatabase db) {
    insertAccount(db, "现金", Account.TYPE_CASH);
    insertAccount(db, "银行卡", Account.TYPE_BANK_CARD);
    insertAccount(db, "信用卡", Account.TYPE_CREDIT_CARD);
  }

  private void insertAccount(SQLiteDatabase db, String name, String accountType) {
    ContentValues values = new ContentValues();
    values.put(COLUMN_NAME, name);
    values.put(COLUMN_ACCOUNT_TYPE, accountType);
    values.put(COLUMN_BALANCE, 0);
    values.put(COLUMN_IS_ACTIVE, 1);
    db.insert(TABLE_ACCOUNT, null, values);
  }
}
