package com.example.accountbook.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.db.CategoryDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.model.BillFilter;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.Category;
import com.example.accountbook.util.DateUtils;
import com.example.accountbook.util.MoneyUtils;

import java.util.ArrayList;
import java.util.List;

public class BillListFragment extends Fragment {

  private BillRecordDao billRecordDao;
  private CategoryDao categoryDao;
  private AccountDao accountDao;
  private Spinner spDateRange;
  private Spinner spType;
  private Spinner spCategory;
  private Spinner spAccount;
  private LinearLayout listContainer;
  private List<Category> categories = new ArrayList<>();
  private List<Account> accounts = new ArrayList<>();

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setFillViewport(true);
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(root);

    root.addView(createTitleRow("全部账单", "返回首页", v -> ((MainActivity) requireActivity()).backToHome()));
    root.addView(createSpinner("日期", new String[] {"全部", "本周", "本月"}, view -> spDateRange = view));
    root.addView(createSpinner("类型", new String[] {"全部", "收入", "支出"}, view -> spType = view));
    root.addView(createSpinner("分类", new String[] {"全部"}, view -> spCategory = view));
    root.addView(createSpinner("账户", new String[] {"全部"}, view -> spAccount = view));
    Button btnApply = new Button(requireContext());
    btnApply.setText("应用筛选");
    btnApply.setOnClickListener(v -> refreshList());
    root.addView(btnApply);

    listContainer = new LinearLayout(requireContext());
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setBackgroundResource(R.drawable.bg_panel);
    listContainer.setPadding(dp(18), dp(10), dp(18), dp(10));
    LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    listParams.setMargins(0, dp(14), 0, 0);
    root.addView(listContainer, listParams);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billRecordDao = new BillRecordDao(requireContext());
    categoryDao = new CategoryDao(requireContext());
    accountDao = new AccountDao(requireContext());
    loadFilterData();
    refreshList();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (billRecordDao != null) {
      refreshList();
    }
  }

  private void loadFilterData() {
    categories = categoryDao.getAllCategories();
    List<String> categoryNames = new ArrayList<>();
    categoryNames.add("全部");
    for (Category category : categories) {
      categoryNames.add(category.getName());
    }
    spCategory.setAdapter(new ArrayAdapter<>(
        requireContext(), android.R.layout.simple_spinner_dropdown_item, categoryNames));

    accounts = accountDao.getAllAccounts();
    List<String> accountNames = new ArrayList<>();
    accountNames.add("全部");
    for (Account account : accounts) {
      accountNames.add(account.getName());
    }
    spAccount.setAdapter(new ArrayAdapter<>(
        requireContext(), android.R.layout.simple_spinner_dropdown_item, accountNames));
  }

  private void refreshList() {
    List<BillRecord> records = billRecordDao.getBillRecordsByFilter(buildFilter());
    listContainer.removeAllViews();
    if (records.isEmpty()) {
      TextView emptyView = createText("没有符合条件的账单", 15, R.color.text_secondary);
      emptyView.setGravity(Gravity.CENTER);
      emptyView.setMinHeight(dp(96));
      listContainer.addView(emptyView);
      return;
    }
    for (BillRecord record : records) {
      listContainer.addView(createBillItem(record));
    }
  }

  private BillFilter buildFilter() {
    BillFilter filter = new BillFilter();
    int dateIndex = spDateRange.getSelectedItemPosition();
    if (dateIndex == 1) {
      filter.setStartDateInclusive(DateUtils.getWeekStart());
      filter.setEndDateExclusive(DateUtils.getNextWeekStart());
    } else if (dateIndex == 2) {
      filter.setStartDateInclusive(DateUtils.getMonthStart());
      filter.setEndDateExclusive(DateUtils.getNextMonthStart());
    }
    int typeIndex = spType.getSelectedItemPosition();
    if (typeIndex == 1) {
      filter.setType(BillRecord.TYPE_INCOME);
    } else if (typeIndex == 2) {
      filter.setType(BillRecord.TYPE_EXPENSE);
    }
    int categoryIndex = spCategory.getSelectedItemPosition();
    if (categoryIndex > 0 && categoryIndex - 1 < categories.size()) {
      filter.setCategoryId(categories.get(categoryIndex - 1).getId());
    }
    int accountIndex = spAccount.getSelectedItemPosition();
    if (accountIndex > 0 && accountIndex - 1 < accounts.size()) {
      filter.setAccountId(accounts.get(accountIndex - 1).getId());
    }
    return filter;
  }

  private View createBillItem(BillRecord record) {
    LinearLayout item = new LinearLayout(requireContext());
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(0, dp(10), 0, dp(10));
    item.setOnClickListener(v -> ((MainActivity) requireActivity()).openBillDetail(record.getId()));
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    item.addView(createText(record.getCategoryName() + "  " + sign + MoneyUtils.format(record.getAmount()),
        16, R.color.text_primary));
    item.addView(createText(record.getRecordDate() + "  " + record.getAccountName() + "  "
        + nullToEmpty(record.getRemark()), 13, R.color.text_secondary));
    return item;
  }

  private LinearLayout createTitleRow(String title, String buttonText, View.OnClickListener listener) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView titleView = createText(title, 24, R.color.text_primary);
    titleView.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText(buttonText);
    button.setOnClickListener(listener);
    row.addView(button);
    return row;
  }

  private View createSpinner(String label, String[] values, SpinnerSetter setter) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(10), 0, 0);
    TextView labelView = createText(label, 15, R.color.text_primary);
    row.addView(labelView, new LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT));
    Spinner spinner = new Spinner(requireContext());
    spinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, values));
    row.addView(spinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    setter.set(spinner);
    return row;
  }

  private TextView createText(String text, int sp, int colorRes) {
    TextView textView = new TextView(requireContext());
    textView.setText(text);
    textView.setTextSize(sp);
    textView.setTextColor(getColor(colorRes));
    return textView;
  }

  private String nullToEmpty(String text) {
    return text == null ? "" : text;
  }

  private int getColor(int resId) {
    return getResources().getColor(resId, requireContext().getTheme());
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private interface SpinnerSetter {
    void set(Spinner spinner);
  }
}
