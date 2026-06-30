package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.util.MoneyUtils;

import java.util.List;

public class AccountManageFragment extends Fragment {

  private static final String[] TYPE_LABELS = {"现金", "银行卡", "信用卡", "第三方支付", "其他"};
  private static final String[] TYPE_VALUES = {
      Account.TYPE_CASH,
      Account.TYPE_BANK_CARD,
      Account.TYPE_CREDIT_CARD,
      Account.TYPE_THIRD_PARTY,
      Account.TYPE_OTHER
  };

  private AccountDao accountDao;
  private BillRecordDao billRecordDao;
  private LinearLayout listContainer;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull android.view.LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(root);
    root.addView(createTitleRow());
    Button btnAdd = new Button(requireContext());
    btnAdd.setText("新增账户");
    btnAdd.setOnClickListener(v -> showEditDialog(null));
    root.addView(btnAdd);
    listContainer = new LinearLayout(requireContext());
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setBackgroundResource(R.drawable.bg_panel);
    listContainer.setPadding(dp(18), dp(10), dp(18), dp(10));
    root.addView(listContainer);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    accountDao = new AccountDao(requireContext());
    billRecordDao = new BillRecordDao(requireContext());
    refreshList();
  }

  private void refreshList() {
    listContainer.removeAllViews();
    List<Account> accounts = accountDao.getAllAccountsIncludingInactive();
    if (accounts.isEmpty()) {
      listContainer.addView(createText("暂无账户", 15, R.color.text_secondary));
      return;
    }
    for (Account account : accounts) {
      listContainer.addView(createItem(account));
    }
  }

  private View createItem(Account account) {
    LinearLayout item = new LinearLayout(requireContext());
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(0, dp(8), 0, dp(8));
    item.addView(createText(account.getName() + "  " + typeLabel(account.getAccountType())
        + "  " + MoneyUtils.format(account.getBalance())
        + (account.isActive() ? "" : "  已停用"), 16, R.color.text_primary));
    LinearLayout actions = new LinearLayout(requireContext());
    Button edit = new Button(requireContext());
    edit.setText("编辑");
    edit.setOnClickListener(v -> showEditDialog(account));
    Button remove = new Button(requireContext());
    remove.setText(billRecordDao.hasRecordsByAccountId(account.getId()) ? "停用" : "删除");
    remove.setOnClickListener(v -> confirmRemove(account));
    actions.addView(edit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    actions.addView(remove, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    item.addView(actions);
    return item;
  }

  private void showEditDialog(@Nullable Account account) {
    LinearLayout content = new LinearLayout(requireContext());
    content.setOrientation(LinearLayout.VERTICAL);
    EditText nameInput = new EditText(requireContext());
    nameInput.setHint("账户名称");
    Spinner typeSpinner = new Spinner(requireContext());
    typeSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, TYPE_LABELS));
    EditText balanceInput = new EditText(requireContext());
    balanceInput.setHint("账户余额");
    balanceInput.setInputType(InputType.TYPE_CLASS_NUMBER
        | InputType.TYPE_NUMBER_FLAG_DECIMAL
        | InputType.TYPE_NUMBER_FLAG_SIGNED);
    content.addView(nameInput);
    content.addView(typeSpinner);
    content.addView(balanceInput);
    if (account != null) {
      nameInput.setText(account.getName());
      typeSpinner.setSelection(typeIndex(account.getAccountType()));
      balanceInput.setText(String.valueOf(account.getBalance()));
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(account == null ? "新增账户" : "编辑账户")
        .setView(content)
        .setPositiveButton("保存", (dialog, which) -> saveAccount(account, nameInput, typeSpinner, balanceInput))
        .setNegativeButton("取消", null)
        .show();
  }

  private void saveAccount(
      @Nullable Account source,
      EditText nameInput,
      Spinner typeSpinner,
      EditText balanceInput) {
    String name = nameInput.getText().toString().trim();
    if (TextUtils.isEmpty(name)) {
      Toast.makeText(requireContext(), "账户名称不能为空", Toast.LENGTH_SHORT).show();
      return;
    }
    double balance = 0;
    String balanceText = balanceInput.getText().toString().trim();
    if (!TextUtils.isEmpty(balanceText)) {
      try {
        balance = Double.parseDouble(balanceText);
      } catch (NumberFormatException e) {
        Toast.makeText(requireContext(), "账户余额格式不正确", Toast.LENGTH_SHORT).show();
        return;
      }
    }
    long excludeId = source == null ? -1 : source.getId();
    if (accountDao.existsActiveAccountName(name, excludeId)) {
      Toast.makeText(requireContext(), "账户名称已存在", Toast.LENGTH_SHORT).show();
      return;
    }
    Account account = source == null ? new Account() : source;
    account.setName(name);
    account.setAccountType(TYPE_VALUES[typeSpinner.getSelectedItemPosition()]);
    account.setBalance(balance);
    account.setActive(source == null || source.isActive());
    if (source == null) {
      accountDao.insertAccount(account);
    } else {
      accountDao.updateAccount(account);
    }
    refreshList();
  }

  private void confirmRemove(Account account) {
    boolean used = billRecordDao.hasRecordsByAccountId(account.getId());
    new AlertDialog.Builder(requireContext())
        .setTitle(used ? "停用账户" : "删除账户")
        .setMessage(used ? "该账户已有账单，只能停用。确认停用？" : "该账户没有账单，确认删除？")
        .setPositiveButton("确认", (dialog, which) -> {
          boolean success = used
              ? accountDao.deactivateAccount(account.getId())
              : accountDao.deleteAccountIfUnused(account.getId());
          Toast.makeText(requireContext(), success ? "操作成功" : "操作失败", Toast.LENGTH_SHORT).show();
          refreshList();
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("账户管理", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回上一级");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToToolbox());
    row.addView(button);
    return row;
  }

  private int typeIndex(String value) {
    for (int i = 0; i < TYPE_VALUES.length; i++) {
      if (TYPE_VALUES[i].equals(value)) {
        return i;
      }
    }
    return 0;
  }

  private String typeLabel(String value) {
    return TYPE_LABELS[typeIndex(value)];
  }

  private TextView createText(String text, int sp, int colorRes) {
    TextView textView = new TextView(requireContext());
    textView.setPadding(0, dp(6), 0, dp(6));
    textView.setText(text);
    textView.setTextSize(sp);
    textView.setTextColor(getColor(colorRes));
    return textView;
  }

  private int getColor(int resId) {
    return getResources().getColor(resId, requireContext().getTheme());
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
