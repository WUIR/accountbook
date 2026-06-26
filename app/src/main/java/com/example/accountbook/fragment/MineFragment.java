package com.example.accountbook.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.model.Account;

import java.util.List;
import java.util.Locale;

public class MineFragment extends Fragment {

  private AccountDao accountDao;
  private LinearLayout accountListContainer;

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
    accountListContainer = view.findViewById(R.id.accountListContainer);
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
