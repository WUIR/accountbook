package com.example.accountbook.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.activity.LoginActivity;
import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.db.UserDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.model.User;
import com.example.accountbook.util.PreferenceUtils;
import com.example.accountbook.util.UserSessionUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MineFragment extends Fragment {

  private AccountDao accountDao;
  private UserDao userDao;
  private LinearLayout accountListContainer;
  private EditText etMonthlyBudget;
  private CheckBox cbBudgetWarnEnabled;
  private TextView btnHomeBudgetMode;
  private boolean homeBudgetModeEnabled;
  private TextView tvAccountTotalBalance;
  private TextView tvAccountCount;
  private TextView tvProfileGreeting;
  private TextView tvProfileName;
  private TextView tvProfileBadge;
  private TextView tvProfileSlogan;
  private View profileCard;
  private View btnProfileSettings;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_mine, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    accountDao = new AccountDao(requireContext());
    userDao = new UserDao(requireContext());
    etMonthlyBudget = view.findViewById(R.id.etMonthlyBudget);
    cbBudgetWarnEnabled = view.findViewById(R.id.cbBudgetWarnEnabled);
    btnHomeBudgetMode = view.findViewById(R.id.btnHomeBudgetMode);
    accountListContainer = view.findViewById(R.id.accountListContainer);
    tvAccountTotalBalance = view.findViewById(R.id.tvAccountTotalBalance);
    tvAccountCount = view.findViewById(R.id.tvAccountCount);
    profileCard = view.findViewById(R.id.profileCard);
    tvProfileGreeting = view.findViewById(R.id.tvProfileGreeting);
    tvProfileName = view.findViewById(R.id.tvProfileName);
    tvProfileBadge = view.findViewById(R.id.tvProfileBadge);
    tvProfileSlogan = view.findViewById(R.id.tvProfileSlogan);
    btnProfileSettings = view.findViewById(R.id.btnProfileSettings);
    btnProfileSettings.setOnClickListener(v -> ((MainActivity) requireActivity()).openToolbox());
    profileCard.setOnClickListener(v -> {
      if (!UserSessionUtils.isLoggedIn(requireContext())) {
        startActivity(new Intent(requireContext(), LoginActivity.class));
      }
    });
    View btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
    btnSaveBudget.setOnClickListener(v -> saveBudgetConfig());
    View entryGeneralSettings = view.findViewById(R.id.entryGeneralSettings);
    entryGeneralSettings.setOnClickListener(v -> ((MainActivity) requireActivity()).openToolbox());
    loadBudgetConfig();
    refreshProfileCard();
    btnHomeBudgetMode.setOnClickListener(v -> toggleHomeBudgetMode());
    refreshAccountBalances();
  }

  @Override
  public void onResume() {
    super.onResume();
    refreshProfileCard();
    if (accountListContainer != null) {
      refreshAccountBalances();
    }
  }

  private void refreshProfileCard() {
    if (tvProfileGreeting != null) {
      tvProfileGreeting.setText(getGreetingText());
    }
    if (tvProfileName == null) {
      return;
    }
    if (!UserSessionUtils.isLoggedIn(requireContext())) {
      tvProfileName.setText(R.string.profile_not_logged_in);
      tvProfileBadge.setText(R.string.profile_not_logged_in);
      tvProfileSlogan.setText(R.string.profile_login_slogan);
      btnProfileSettings.setVisibility(View.GONE);
      return;
    }
    User user = userDao.getUserById(UserSessionUtils.getCurrentUserId(requireContext()));
    if (user == null) {
      UserSessionUtils.clearLoginSession(requireContext());
      tvProfileName.setText(R.string.profile_not_logged_in);
      tvProfileBadge.setText(R.string.profile_not_logged_in);
      tvProfileSlogan.setText(R.string.profile_login_slogan);
      btnProfileSettings.setVisibility(View.GONE);
      return;
    }
    tvProfileName.setText(TextUtils.isEmpty(user.getNickname())
        ? User.DEFAULT_NICKNAME
        : user.getNickname());
    tvProfileBadge.setText(TextUtils.isEmpty(user.getRoleLabel())
        ? User.DEFAULT_ROLE_LABEL
        : user.getRoleLabel());
    if (TextUtils.isEmpty(user.getSignature())) {
      tvProfileSlogan.setText(R.string.profile_default_slogan);
    } else {
      tvProfileSlogan.setText(user.getSignature());
    }
    btnProfileSettings.setVisibility(View.VISIBLE);
  }

  private String getGreetingText() {
    int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    if (hour >= 5 && hour < 12) {
      return getString(R.string.profile_greeting_morning);
    }
    if (hour >= 12 && hour < 18) {
      return getString(R.string.profile_greeting_afternoon);
    }
    return getString(R.string.profile_greeting_evening);
  }

  private void refreshAccountBalances() {
    List<Account> accounts = accountDao.getAllAccounts();
    tvAccountTotalBalance.setText(formatMoney(calculateTotalBalance(accounts)));
    tvAccountCount.setText(getString(R.string.account_count_value, accounts.size()));
    accountListContainer.removeAllViews();
    if (accounts.isEmpty()) {
      TextView emptyView = createAccountTextView(getString(R.string.no_accounts));
      accountListContainer.addView(emptyView);
      return;
    }
    for (Account account : accounts) {
      accountListContainer.addView(createAccountRowView(account));
    }
  }

  private void loadBudgetConfig() {
    double monthlyBudget = PreferenceUtils.getMonthlyBudget(requireContext());
    etMonthlyBudget.setText(monthlyBudget > 0
        ? String.format(Locale.CHINA, "%.2f", monthlyBudget)
        : "");
    cbBudgetWarnEnabled.setChecked(PreferenceUtils.isBudgetWarnEnabled(requireContext()));
    homeBudgetModeEnabled = PreferenceUtils.isHomeBudgetModeEnabled(requireContext());
    updateHomeBudgetModeButton();
  }

  private void saveBudgetConfig() {
    String budgetText = etMonthlyBudget.getText().toString().trim();
    double monthlyBudget = 0;
    if (!TextUtils.isEmpty(budgetText)) {
      try {
        monthlyBudget = Double.parseDouble(budgetText);
      } catch (NumberFormatException e) {
        Toast.makeText(requireContext(), R.string.error_budget_invalid, Toast.LENGTH_SHORT).show();
        return;
      }
      if (monthlyBudget < 0) {
        Toast.makeText(requireContext(), R.string.error_budget_negative, Toast.LENGTH_SHORT).show();
        return;
      }
    }
    PreferenceUtils.saveBudgetConfig(
        requireContext(), monthlyBudget, cbBudgetWarnEnabled.isChecked());
    Toast.makeText(requireContext(), R.string.save_budget_success, Toast.LENGTH_SHORT).show();
  }

  private void toggleHomeBudgetMode() {
    homeBudgetModeEnabled = !homeBudgetModeEnabled;
    PreferenceUtils.saveHomeBudgetModeEnabled(requireContext(), homeBudgetModeEnabled);
    updateHomeBudgetModeButton();
    Toast.makeText(
        requireContext(),
        homeBudgetModeEnabled ? R.string.home_budget_mode_enabled : R.string.home_budget_mode_disabled,
        Toast.LENGTH_SHORT).show();
  }

  private void updateHomeBudgetModeButton() {
    btnHomeBudgetMode.setText(homeBudgetModeEnabled
        ? R.string.home_budget_mode_on
        : R.string.home_budget_mode_off);
    btnHomeBudgetMode.setBackgroundResource(homeBudgetModeEnabled
        ? R.drawable.bg_save_button
        : R.drawable.bg_plain_button);
    btnHomeBudgetMode.setTextColor(homeBudgetModeEnabled
        ? getColor(R.color.white)
        : getColor(R.color.text_primary));
  }

  private TextView createAccountTextView(String text) {
    TextView textView = new TextView(requireContext());
    textView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    textView.setPadding(0, 10, 0, 10);
    textView.setText(text);
    textView.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
    textView.setTextSize(16);
    return textView;
  }

  private View createAccountRowView(Account account) {
    LinearLayout rowView = new LinearLayout(requireContext());
    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    rowView.setLayoutParams(rowParams);
    rowView.setGravity(Gravity.CENTER_VERTICAL);
    rowView.setOrientation(LinearLayout.HORIZONTAL);
    rowView.setPadding(0, dpToPx(10), 0, dpToPx(10));

    TextView markView = new TextView(requireContext());
    markView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)));
    markView.setBackgroundResource(R.drawable.bg_category_dot);
    markView.setGravity(Gravity.CENTER);
    markView.setText(getAccountInitial(account));
    markView.setTextColor(getColor(R.color.brand_green));
    markView.setTextSize(14);

    TextView nameView = new TextView(requireContext());
    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1);
    nameParams.setMargins(dpToPx(12), 0, dpToPx(12), 0);
    nameView.setLayoutParams(nameParams);
    nameView.setText(account.getName());
    nameView.setTextColor(getColor(account.isActive() ? R.color.text_primary : R.color.text_secondary));
    nameView.setTextSize(15);
    nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

    TextView balanceView = new TextView(requireContext());
    balanceView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    balanceView.setGravity(Gravity.END);
    balanceView.setText(formatMoney(account.getBalance()));
    balanceView.setTextColor(getColor(account.getBalance() < 0 ? R.color.expense : R.color.text_primary));
    balanceView.setTextSize(15);

    rowView.addView(markView);
    rowView.addView(nameView);
    rowView.addView(balanceView);
    return rowView;
  }

  private double calculateTotalBalance(List<Account> accounts) {
    double total = 0;
    for (Account account : accounts) {
      total += account.getBalance();
    }
    return total;
  }

  private String formatMoney(double amount) {
    return String.format(Locale.CHINA, "¥%.2f", amount);
  }

  private String getAccountInitial(Account account) {
    String name = account.getName();
    if (TextUtils.isEmpty(name)) {
      return "账";
    }
    return name.substring(0, 1);
  }

  private int getColor(int colorRes) {
    return getResources().getColor(colorRes, requireContext().getTheme());
  }

  private int dpToPx(int dp) {
    return Math.round(dp * getResources().getDisplayMetrics().density);
  }
}
