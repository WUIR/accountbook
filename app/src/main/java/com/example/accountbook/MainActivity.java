package com.example.accountbook;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.accountbook.fragment.AddBillFragment;
import com.example.accountbook.fragment.HomeFragment;
import com.example.accountbook.fragment.MineFragment;
import com.google.android.material.navigation.NavigationBarView;

import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

  private static final String TAG_HOME = "home";
  private static final String TAG_ADD_BILL = "add_bill";
  private static final String TAG_MINE = "mine";

  private String currentTag = TAG_HOME;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_main);
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
      return insets;
    });
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

  private Fragment createFragment(String tag) {
    if (TAG_ADD_BILL.equals(tag)) {
      return new AddBillFragment();
    } else if (TAG_MINE.equals(tag)) {
      return new MineFragment();
    }
    return new HomeFragment();
  }
}
