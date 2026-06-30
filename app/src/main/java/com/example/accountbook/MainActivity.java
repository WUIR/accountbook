package com.example.accountbook;

import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.example.accountbook.db.AccountBookDbHelper;
import com.example.accountbook.fragment.AddBillFragment;
import com.example.accountbook.fragment.BillDetailFragment;
import com.example.accountbook.fragment.BillListFragment;
import com.example.accountbook.fragment.EditBillFragment;
import com.example.accountbook.fragment.ExportFragment;
import com.example.accountbook.fragment.HomeFragment;
import com.example.accountbook.fragment.MineFragment;
import com.example.accountbook.fragment.RecycleBinFragment;
import com.example.accountbook.fragment.StatisticsFragment;
import com.example.accountbook.fragment.AccountManageFragment;
import com.example.accountbook.fragment.CategoryManageFragment;
import com.example.accountbook.fragment.ToolboxFragment;
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
  private static final String TAG_EXPORT = "export";
  private static final String TAG_ACCOUNT_MANAGE = "account_manage";
  private static final String TAG_CATEGORY_MANAGE = "category_manage";
  private static final String TAG_TOOLBOX = "toolbox";

  private String currentTag = TAG_HOME;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    setupSystemBars();
    new AccountBookDbHelper(this).getWritableDatabase();
    setupBottomNavigation();
    if (savedInstanceState == null) {
      switchToFragment(TAG_HOME);
    }
  }

  private void setupSystemBars() {
    Window window = getWindow();
    window.setStatusBarColor(getColor(R.color.app_background));
    window.setNavigationBarColor(getColor(R.color.surface));

    View rootView = findViewById(R.id.main);
    View fragmentContainer = findViewById(R.id.fragmentContainer);
    View bottomNavigation = findViewById(R.id.bottomNavigation);
    WindowInsetsControllerCompat controller =
        new WindowInsetsControllerCompat(window, rootView);
    controller.setAppearanceLightStatusBars(true);
    controller.setAppearanceLightNavigationBars(true);

    int fragmentLeft = fragmentContainer.getPaddingLeft();
    int fragmentTop = fragmentContainer.getPaddingTop();
    int fragmentRight = fragmentContainer.getPaddingRight();
    int fragmentBottom = fragmentContainer.getPaddingBottom();
    int navLeft = bottomNavigation.getPaddingLeft();
    int navTop = bottomNavigation.getPaddingTop();
    int navRight = bottomNavigation.getPaddingRight();
    int navBottom = bottomNavigation.getPaddingBottom();

    ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
      Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      fragmentContainer.setPadding(
          fragmentLeft,
          fragmentTop + systemBars.top,
          fragmentRight,
          fragmentBottom);
      bottomNavigation.setPadding(
          navLeft,
          navTop,
          navRight,
          navBottom + systemBars.bottom);
      return windowInsets;
    });
    ViewCompat.requestApplyInsets(rootView);
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

  public void openExport() {
    switchToFragment(TAG_EXPORT);
  }

  public void openToolbox() {
    switchToFragment(TAG_TOOLBOX);
  }

  public void openAccountManage() {
    switchToFragment(TAG_ACCOUNT_MANAGE);
  }

  public void openCategoryManage() {
    switchToFragment(TAG_CATEGORY_MANAGE);
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

  public void backToToolbox() {
    switchToFragment(TAG_TOOLBOX);
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
    } else if (TAG_EXPORT.equals(tag)) {
      return new ExportFragment();
    } else if (TAG_ACCOUNT_MANAGE.equals(tag)) {
      return new AccountManageFragment();
    } else if (TAG_CATEGORY_MANAGE.equals(tag)) {
      return new CategoryManageFragment();
    } else if (TAG_TOOLBOX.equals(tag)) {
      return new ToolboxFragment();
    }
    return new HomeFragment();
  }
}
