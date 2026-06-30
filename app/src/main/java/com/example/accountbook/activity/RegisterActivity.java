package com.example.accountbook.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.example.accountbook.util.PasswordUtils;
import com.example.accountbook.util.UserSessionUtils;

public class RegisterActivity extends AppCompatActivity {

  private UserDao userDao;
  private EditText etUsername;
  private EditText etPassword;
  private EditText etConfirmPassword;
  private EditText etNickname;
  private CheckBox cbAgreement;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_register);
    setupSystemBars();
    userDao = new UserDao(this);
    etUsername = findViewById(R.id.etRegisterUsername);
    etPassword = findViewById(R.id.etRegisterPassword);
    etConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
    etNickname = findViewById(R.id.etRegisterNickname);
    cbAgreement = findViewById(R.id.cbAgreement);

    findViewById(R.id.btnRegisterBack).setOnClickListener(v -> finish());
    findViewById(R.id.btnRegister).setOnClickListener(v -> register());
    findViewById(R.id.btnBackToLogin).setOnClickListener(v -> finish());
  }

  private void setupSystemBars() {
    Window window = getWindow();
    window.setStatusBarColor(getColor(R.color.app_background));
    window.setNavigationBarColor(getColor(R.color.app_background));
    View rootView = findViewById(R.id.registerRoot);
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

  private void register() {
    String username = etUsername.getText().toString().trim();
    String password = etPassword.getText().toString();
    String confirmPassword = etConfirmPassword.getText().toString();
    String nickname = etNickname.getText().toString().trim();
    if (TextUtils.isEmpty(username)) {
      Toast.makeText(this, R.string.error_username_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    if (TextUtils.isEmpty(password)) {
      Toast.makeText(this, R.string.error_password_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    if (TextUtils.isEmpty(confirmPassword)) {
      Toast.makeText(this, R.string.error_confirm_password_empty, Toast.LENGTH_SHORT).show();
      return;
    }
    if (password.length() < 6) {
      Toast.makeText(this, R.string.error_password_too_short, Toast.LENGTH_SHORT).show();
      return;
    }
    if (!password.equals(confirmPassword)) {
      Toast.makeText(this, R.string.error_password_not_match, Toast.LENGTH_SHORT).show();
      return;
    }
    if (!cbAgreement.isChecked()) {
      Toast.makeText(this, R.string.error_agreement_required, Toast.LENGTH_SHORT).show();
      return;
    }
    if (userDao.isUsernameExists(username)) {
      Toast.makeText(this, R.string.error_username_exists, Toast.LENGTH_SHORT).show();
      return;
    }
    long now = System.currentTimeMillis();
    String salt = PasswordUtils.generateSalt();
    User user = new User();
    user.setUsername(username);
    user.setPasswordSalt(salt);
    user.setPasswordHash(PasswordUtils.hashPassword(password, salt));
    user.setNickname(TextUtils.isEmpty(nickname) ? User.DEFAULT_NICKNAME : nickname);
    user.setRoleLabel(User.DEFAULT_ROLE_LABEL);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    user.setLastLoginAt(now);

    long userId = userDao.insertUser(user);
    if (userId <= 0) {
      Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
      return;
    }
    UserSessionUtils.saveLoginSession(this, userId, username);
    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
    setResult(Activity.RESULT_OK);
    finish();
  }
}
