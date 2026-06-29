package com.example.accountbook;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.accountbook.db.AccountBookDbHelper;
import com.example.accountbook.fragment.AddBillFragment;
import com.example.accountbook.fragment.BillDetailFragment;
import com.example.accountbook.fragment.BillListFragment;
import com.example.accountbook.fragment.EditBillFragment;
import com.example.accountbook.fragment.HomeFragment;
import com.example.accountbook.fragment.MineFragment;
import com.example.accountbook.fragment.RecycleBinFragment;
import com.example.accountbook.fragment.StatisticsFragment;
import com.google.android.material.navigation.NavigationBarView;

import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

  private static final String TAG_HOME = "home";
  private static final String TAG_ADD_BILL = "add_bill";
  private static final String TAG_MINE = "mine";
  private static final String TAG_BILL_LIST = "bill_list";
  private static final String TAG_BILL_DETAIL = "bill_detail";
  private static final String TAG_EDIT_BILL = "edit_bill";
  private static final String TAG_STATISTICS = "statistics";
  private static final String TAG_RECYCLE_BIN = "recycle_bin";

  private String currentTag = TAG_HOME;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    new AccountBookDbHelper(this).getWritableDatabase();
    setupBottomNavigation();
    if (savedInstanceState == null) {
      switchToFragment(TAG_HOME);
    }
  }

  private void setupBottomNavigation() {
    NavigationBarView bottomNavigation = findViewById(R.id.bottomNavigation);
    bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
  }

  private boolean onNavigationItemSelected(@NonNull MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.nav_home) {
      switchToFragment(TAG_HOME);
      return true;
    } else if (itemId == R.id.nav_add_bill) {
      switchToFragment(TAG_ADD_BILL);
      return true;
    } else if (itemId == R.id.nav_mine) {
      switchToFragment(TAG_MINE);
      return true;
    }
    return false;
  }

  private void switchToFragment(String tag) {
    if (tag.equals(currentTag) && getSupportFragmentManager().findFragmentByTag(tag) != null) {
      return;
    }
    currentTag = tag;
    Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
    if (fragment == null) {
      fragment = createFragment(tag);
    }
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, fragment, tag)
        .commit();
  }

  public void openBillList() {
    switchToFragment(TAG_BILL_LIST);
  }

  public void openStatistics() {
    switchToFragment(TAG_STATISTICS);
  }

  public void openRecycleBin() {
    switchToFragment(TAG_RECYCLE_BIN);
  }

  public void openBillDetail(long billId) {
    currentTag = TAG_BILL_DETAIL;
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, BillDetailFragment.newInstance(billId), TAG_BILL_DETAIL)
        .commit();
  }

  public void openEditBill(long billId) {
    currentTag = TAG_EDIT_BILL;
    getSupportFragmentManager()
        .beginTransaction()
        .replace(R.id.fragmentContainer, EditBillFragment.newInstance(billId), TAG_EDIT_BILL)
        .commit();
  }

  public void backToHome() {
    switchToFragment(TAG_HOME);
  }

  public void backToMine() {
    switchToFragment(TAG_MINE);
  }

  public void backToBillList() {
    switchToFragment(TAG_BILL_LIST);
  }

  private Fragment createFragment(String tag) {
    if (TAG_ADD_BILL.equals(tag)) {
      return new AddBillFragment();
    } else if (TAG_MINE.equals(tag)) {
      return new MineFragment();
    } else if (TAG_BILL_LIST.equals(tag)) {
      return new BillListFragment();
    } else if (TAG_STATISTICS.equals(tag)) {
      return new StatisticsFragment();
    } else if (TAG_RECYCLE_BIN.equals(tag)) {
      return new RecycleBinFragment();
    }
    return new HomeFragment();
  }
}
