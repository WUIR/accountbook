package com.example.accountbook.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.accountbook.R;
import com.example.accountbook.db.UserDao;
import com.example.accountbook.model.User;
import com.example.accountbook.util.LoginRememberUtils;
import com.example.accountbook.util.PasswordUtils;
import com.example.accountbook.util.UserSessionUtils;

public class LoginActivity extends AppCompatActivity {

  private static final int REQUEST_REGISTER = 1001;

  private UserDao userDao;
  private EditText etUsername;
  private EditText etPassword;
  private CheckBox cbRememberPassword;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    setupSystemBars();
    userDao = new UserDao(this);
    etUsername = findViewById(R.id.etLoginUsername);
    etPassword = findViewById(R.id.etLoginPassword);
    cbRememberPassword = findViewById(R.id.cbRememberPassword);

    findViewById(R.id.btnLoginBack).setOnClickListener(v -> finish());
    findViewById(R.id.btnLogin).setOnClickListener(v -> login());
    findViewById(R.id.btnOpenRegister).setOnClickListener(v ->
        startActivityForResult(new Intent(this, RegisterActivity.class), REQUEST_REGISTER));
    loadRememberConfig();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_REGISTER && resultCode == Activity.RESULT_OK) {
      setResult(Activity.RESULT_OK);
      finish();
    }
  }

  private void setupSystemBars() {
    Window window = getWindow();
    window.setStatusBarColor(getColor(R.color.app_background));
    window.setNavigationBarColor(getColor(R.color.app_background));
    View rootView = findViewById(R.id.loginRoot);
    WindowInsetsControllerCompat controller =
        new WindowInsetsControllerCompat(window, rootView);
    controller.setAppearanceLightStatusBars(true);
    controller.setAppearanceLightNavigationBars(true);

    int rootLeft = rootView.getPaddingLeft();
    int rootTop = rootView.getPaddingTop();
    int rootRight = rootView.getPaddingRight();
    int rootBottom = rootView.getPaddingBottom();
    ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
      Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
      view.setPadding(rootLeft, rootTop + systemBars.top, rootRight, rootBottom + systemBars.bottom);
      return windowInsets;
    });
    ViewCompat.requestApplyInsets(rootView);
  }

  private void loadRememberConfig() {
    if (LoginRememberUtils.isRememberPasswordEnabled(this)) {
      etUsername.setText(LoginRememberUtils.getRememberedUsername(this));
      etPassword.setText(LoginRememberUtils.getRememberedPassword(this));
      cbRememberPassword.setChecked(true);
      return;
    }
    etUsername.setText(UserSessionUtils.getLastLoginUsername(this));
    cbRememberPassword.setChecked(false);
  }

  private void login() {
    String username = etUsername.getText().toString().trim();
    String password = etPassword.getText().toString();
    if (TextUtils.isEmpty(username)) {
      Toast.makeText(this, R.string.error_username_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    if (TextUtils.isEmpty(password)) {
      Toast.makeText(this, R.string.error_password_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    User user = userDao.getUserByUsername(username);
    if (user == null
        || !PasswordUtils.verifyPassword(password, user.getPasswordSalt(), user.getPasswordHash())) {
      Toast.makeText(this, R.string.error_login_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    long now = System.currentTimeMillis();
    userDao.updateLastLoginAt(user.getId(), now);
    UserSessionUtils.saveLoginSession(this, user.getId(), username);
    if (cbRememberPassword.isChecked()) {
      LoginRememberUtils.saveRememberedPassword(this, username, password);
    } else {
      LoginRememberUtils.clearRememberedPassword(this, username);
    }
    Toast.makeText(this, R.string.login_success, Toast.LENGTH_SHORT).show();
    setResult(Activity.RESULT_OK);
    finish();
  }
}
