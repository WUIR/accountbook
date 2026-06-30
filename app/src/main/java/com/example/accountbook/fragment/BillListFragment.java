package com.example.accountbook.fragment;

import android.os.Bundle;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
    root.setPadding(dp(20), dp(20), dp(20), dp(32));
    scrollView.addView(root);

    root.addView(createTitleRow());
    root.addView(createFilterCard());

    listContainer = new LinearLayout(requireContext());
    listContainer.setOrientation(LinearLayout.VERTICAL);
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
    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    itemParams.setMargins(0, 0, 0, dp(10));
    item.setLayoutParams(itemParams);
    item.setBackgroundResource(R.drawable.bg_home_light_card);
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setPadding(dp(14), dp(12), dp(14), dp(12));
    item.setOnClickListener(v -> ((MainActivity) requireActivity()).openBillDetail(record.getId()));

    TextView markView = createAccountMark(record);
    item.addView(markView);

    LinearLayout content = new LinearLayout(requireContext());
    LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1);
    contentParams.setMargins(dp(12), 0, dp(12), 0);
    content.setLayoutParams(contentParams);
    content.setOrientation(LinearLayout.VERTICAL);

    TextView title = createText(nullToEmpty(record.getCategoryName()), 15, R.color.text_primary);
    title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    TextView detail = createText(formatBillDetail(record), 13, R.color.text_secondary);
    detail.setPadding(0, dp(4), 0, 0);
    content.addView(title);
    content.addView(detail);

    TextView amount = createText(formatBillAmount(record), 15, R.color.text_primary);
    amount.setGravity(Gravity.END);
    item.addView(content);
    item.addView(amount);
    return item;
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView titleView = createText("全部账单", 24, R.color.text_primary);
    titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    TextView backView = createText("返回首页", 14, R.color.text_secondary);
    backView.setGravity(Gravity.CENTER);
    backView.setMinHeight(dp(40));
    backView.setPadding(dp(12), 0, 0, 0);
    backView.setOnClickListener(v -> ((MainActivity) requireActivity()).backToHome());
    row.addView(backView);
    return row;
  }

  private View createFilterCard() {
    LinearLayout card = new LinearLayout(requireContext());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(18), 0, 0);
    card.setLayoutParams(params);
    card.setBackgroundResource(R.drawable.bg_home_light_card);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(14), dp(8), dp(14), dp(14));
    card.addView(createSpinner("日期", new String[] {"全部", "本周", "本月"}, view -> spDateRange = view));
    card.addView(createSpinner("类型", new String[] {"全部", "收入", "支出"}, view -> spType = view));
    card.addView(createSpinner("分类", new String[] {"全部"}, view -> spCategory = view));
    card.addView(createSpinner("账户", new String[] {"全部"}, view -> spAccount = view));
    TextView applyView = createText("应用筛选", 15, R.color.white);
    LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(44));
    applyParams.setMargins(0, dp(10), 0, 0);
    applyView.setLayoutParams(applyParams);
    applyView.setBackgroundResource(R.drawable.bg_save_button);
    applyView.setClickable(true);
    applyView.setFocusable(true);
    TypedArray typedArray = requireContext().getTheme()
        .obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
    applyView.setForeground(typedArray.getDrawable(0));
    typedArray.recycle();
    applyView.setGravity(Gravity.CENTER);
    applyView.setOnClickListener(v -> refreshList());
    card.addView(applyView);
    return card;
  }

  private View createSpinner(String label, String[] values, SpinnerSetter setter) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(6), 0, dp(6));
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

  private TextView createAccountMark(BillRecord record) {
    TextView markView = new TextView(requireContext());
    markView.setLayoutParams(new LinearLayout.LayoutParams(dp(40), dp(40)));
    markView.setBackgroundResource(R.drawable.bg_category_dot);
    markView.setGravity(Gravity.CENTER);
    markView.setText(getCategoryInitial(record));
    markView.setTextColor(getColor(R.color.brand_green));
    markView.setTextSize(14);
    markView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    return markView;
  }

  private String formatBillAmount(BillRecord record) {
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    return sign + MoneyUtils.format(record.getAmount());
  }

  private String formatBillDetail(BillRecord record) {
    StringBuilder builder = new StringBuilder();
    builder.append(nullToEmpty(record.getRecordDate()))
        .append("  ")
        .append(nullToEmpty(record.getAccountName()));
    if (!nullToEmpty(record.getRemark()).isEmpty()) {
      builder.append("  ").append(record.getRemark());
    }
    return builder.toString();
  }

  private String getCategoryInitial(BillRecord record) {
    String categoryName = nullToEmpty(record.getCategoryName());
    if (categoryName.isEmpty()) {
      return "账";
    }
    return categoryName.substring(0, 1);
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
