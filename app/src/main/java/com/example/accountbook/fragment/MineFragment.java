package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.util.PreferenceUtils;

import java.util.List;
import java.util.Locale;

public class MineFragment extends Fragment {

  private AccountDao accountDao;
  private LinearLayout accountListContainer;
  private EditText etMonthlyBudget;
  private CheckBox cbBudgetWarnEnabled;

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
    etMonthlyBudget = view.findViewById(R.id.etMonthlyBudget);
    cbBudgetWarnEnabled = view.findViewById(R.id.cbBudgetWarnEnabled);
    accountListContainer = view.findViewById(R.id.accountListContainer);
    Button btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
    btnSaveBudget.setOnClickListener(v -> saveBudgetConfig());
    View toolRecycleBin = view.findViewById(R.id.toolRecycleBin);
    toolRecycleBin.setOnClickListener(v -> ((MainActivity) requireActivity()).openRecycleBin());
    View toolExport = view.findViewById(R.id.toolExport);
    toolExport.setOnClickListener(v -> ((MainActivity) requireActivity()).openExport());
    View toolAccountManage = view.findViewById(R.id.toolAccountManage);
    toolAccountManage.setOnClickListener(v -> ((MainActivity) requireActivity()).openAccountManage());
    View toolCategoryManage = view.findViewById(R.id.toolCategoryManage);
    toolCategoryManage.setOnClickListener(v -> ((MainActivity) requireActivity()).openCategoryManage());
    loadBudgetConfig();
    refreshAccountBalances();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (accountListContainer != null) {
      refreshAccountBalances();
    }
  }

  private void refreshAccountBalances() {
    List<Account> accounts = accountDao.getAllAccounts();
    accountListContainer.removeAllViews();
    if (accounts.isEmpty()) {
      TextView emptyView = createAccountTextView(getString(R.string.no_accounts));
      accountListContainer.addView(emptyView);
      return;
    }
    for (Account account : accounts) {
      accountListContainer.addView(createAccountTextView(formatAccount(account)));
    }
  }

  private void loadBudgetConfig() {
    double monthlyBudget = PreferenceUtils.getMonthlyBudget(requireContext());
    etMonthlyBudget.setText(monthlyBudget > 0
        ? String.format(Locale.CHINA, "%.2f", monthlyBudget)
        : "");
    cbBudgetWarnEnabled.setChecked(PreferenceUtils.isBudgetWarnEnabled(requireContext()));
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

  private String formatAccount(Account account) {
    return String.format(Locale.CHINA, "%s  ¥%.2f", account.getName(), account.getBalance());
  }
}
