package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.util.PreferenceUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

  private TextView tvMonthlyBalance;
  private TextView tvMonthlyBalanceLabel;
  private TextView tvMonthlyIncome;
  private TextView tvMonthlyIncomeLabel;
  private TextView tvMonthlyExpense;
  private TextView tvMonthlyExpenseLabel;
  private LinearLayout recentBillsContainer;
  private BillRecordDao billRecordDao;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_home, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billRecordDao = new BillRecordDao(requireContext());
    tvMonthlyBalance = view.findViewById(R.id.tvMonthlyBalance);
    tvMonthlyBalanceLabel = view.findViewById(R.id.tvMonthlyBalanceLabel);
    tvMonthlyIncome = view.findViewById(R.id.tvMonthlyIncome);
    tvMonthlyIncomeLabel = view.findViewById(R.id.tvMonthlyIncomeLabel);
    tvMonthlyExpense = view.findViewById(R.id.tvMonthlyExpense);
    tvMonthlyExpenseLabel = view.findViewById(R.id.tvMonthlyExpenseLabel);
    recentBillsContainer = view.findViewById(R.id.recentBillsContainer);
    view.findViewById(R.id.tvAllBills)
        .setOnClickListener(v -> ((MainActivity) requireActivity()).openBillList());
    refreshHomeData();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (billRecordDao != null) {
      refreshHomeData();
    }
  }

  private void refreshHomeData() {
    String monthStart = getMonthStart();
    String nextMonthStart = getNextMonthStart();
    double income = billRecordDao.getMonthlyTotal(BillRecord.TYPE_INCOME, monthStart, nextMonthStart);
    double expense = billRecordDao.getMonthlyTotal(BillRecord.TYPE_EXPENSE, monthStart, nextMonthStart);
    boolean budgetModeEnabled = PreferenceUtils.isHomeBudgetModeEnabled(requireContext());
    double monthlyBudget = PreferenceUtils.getMonthlyBudget(requireContext());
    if (budgetModeEnabled) {
      tvMonthlyIncomeLabel.setText(R.string.month_budget_label);
      tvMonthlyExpenseLabel.setText(R.string.month_used_label);
      tvMonthlyBalanceLabel.setText(R.string.month_remaining_label);
      tvMonthlyIncome.setText(formatMoney(monthlyBudget));
      tvMonthlyExpense.setText(formatMoney(expense));
      tvMonthlyBalance.setText(formatMoney(monthlyBudget - expense));
    } else {
      tvMonthlyIncomeLabel.setText(R.string.month_income_label);
      tvMonthlyExpenseLabel.setText(R.string.month_expense_label);
      tvMonthlyBalanceLabel.setText(R.string.month_balance_label);
      tvMonthlyIncome.setText(formatMoney(income));
      tvMonthlyExpense.setText(formatMoney(expense));
      tvMonthlyBalance.setText(formatMoney(income - expense));
    }
    refreshRecentBills();
  }

  private void refreshRecentBills() {
    List<BillRecord> records = billRecordDao.getRecentBillRecords(10);
    recentBillsContainer.removeAllViews();
    if (records.isEmpty()) {
      TextView emptyView = createTextView(getString(R.string.no_bill_records), 15);
      emptyView.setGravity(android.view.Gravity.CENTER);
      emptyView.setMinHeight(dpToPx(96));
      recentBillsContainer.addView(emptyView);
      return;
    }
    String currentDate = "";
    double dayIncome = 0;
    double dayExpense = 0;
    for (BillRecord record : records) {
      if (!safeText(record.getRecordDate()).equals(currentDate)) {
        currentDate = safeText(record.getRecordDate());
        dayIncome = getDayTotal(records, currentDate, BillRecord.TYPE_INCOME);
        dayExpense = getDayTotal(records, currentDate, BillRecord.TYPE_EXPENSE);
        recentBillsContainer.addView(createDateHeaderView(currentDate, dayIncome, dayExpense));
      }
      recentBillsContainer.addView(createBillItemView(record));
    }
  }

  private View createDateHeaderView(String recordDate, double income, double expense) {
    LinearLayout headerView = new LinearLayout(requireContext());
    headerView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    headerView.setGravity(Gravity.CENTER_VERTICAL);
    headerView.setOrientation(LinearLayout.HORIZONTAL);
    headerView.setPadding(0, dpToPx(12), 0, dpToPx(8));

    TextView dateView = createTextView(formatDateHeader(recordDate), 14);
    dateView.setLayoutParams(new LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1));
    dateView.setTextColor(getColor(R.color.text_primary));
    dateView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

    TextView summaryView = createTextView(formatDaySummary(income, expense), 13);
    summaryView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    summaryView.setGravity(Gravity.END);
    summaryView.setTextColor(getColor(R.color.text_secondary));

    headerView.addView(dateView);
    headerView.addView(summaryView);
    return headerView;
  }

  private View createBillItemView(BillRecord record) {
    LinearLayout rowView = new LinearLayout(requireContext());
    rowView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    rowView.setGravity(Gravity.TOP);
    rowView.setOrientation(LinearLayout.HORIZONTAL);

    LinearLayout timelineView = new LinearLayout(requireContext());
    timelineView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), dpToPx(76)));
    timelineView.setGravity(Gravity.CENTER_HORIZONTAL);
    timelineView.setOrientation(LinearLayout.VERTICAL);

    TextView topLine = new TextView(requireContext());
    topLine.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(1), dpToPx(8)));
    topLine.setBackgroundColor(getColor(R.color.border));

    TextView dotView = new TextView(requireContext());
    dotView.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)));
    dotView.setBackgroundResource(R.drawable.bg_timeline_dot);

    TextView bottomLine = new TextView(requireContext());
    bottomLine.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(1), 0, 1));
    bottomLine.setBackgroundColor(getColor(R.color.border));

    timelineView.addView(topLine);
    timelineView.addView(dotView);
    timelineView.addView(bottomLine);

    LinearLayout itemView = new LinearLayout(requireContext());
    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1);
    itemParams.setMargins(0, 0, 0, dpToPx(10));
    itemView.setLayoutParams(itemParams);
    itemView.setBackgroundResource(R.drawable.bg_home_light_card);
    itemView.setGravity(Gravity.CENTER_VERTICAL);
    itemView.setOrientation(LinearLayout.HORIZONTAL);
    itemView.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

    TextView categoryMark = new TextView(requireContext());
    LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
    categoryMark.setLayoutParams(markParams);
    categoryMark.setBackgroundResource(R.drawable.bg_category_dot);
    categoryMark.setGravity(Gravity.CENTER);
    categoryMark.setText(getCategoryInitial(record));
    categoryMark.setTextColor(getColor(R.color.brand_green));
    categoryMark.setTextSize(14);
    categoryMark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

    LinearLayout contentView = new LinearLayout(requireContext());
    LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
        0,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        1);
    contentParams.setMargins(dpToPx(12), 0, dpToPx(12), 0);
    contentView.setLayoutParams(contentParams);
    contentView.setOrientation(LinearLayout.VERTICAL);

    TextView titleView = createTextView(safeText(record.getCategoryName()), 15);
    titleView.setTextColor(getColor(R.color.text_primary));
    TextView detailView = createTextView(formatBillDetail(record), 13);
    detailView.setTextColor(getColor(R.color.text_secondary));
    contentView.addView(titleView);
    contentView.addView(detailView);

    TextView amountView = createTextView(formatBillAmount(record), 15);
    amountView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    amountView.setGravity(Gravity.END);
    amountView.setTextColor(getColor(R.color.text_primary));

    itemView.addView(categoryMark);
    itemView.addView(contentView);
    itemView.addView(amountView);
    rowView.addView(timelineView);
    rowView.addView(itemView);
    return rowView;
  }

  private TextView createTextView(String text, int textSizeSp) {
    TextView textView = new TextView(requireContext());
    textView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    textView.setText(text);
    textView.setTextSize(textSizeSp);
    textView.setTextColor(getColor(R.color.text_secondary));
    return textView;
  }

  private String formatBillAmount(BillRecord record) {
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    return sign + formatMoney(record.getAmount());
  }

  private double getDayTotal(List<BillRecord> records, String recordDate, String type) {
    double total = 0;
    for (BillRecord record : records) {
      if (recordDate.equals(safeText(record.getRecordDate())) && type.equals(record.getType())) {
        total += record.getAmount();
      }
    }
    return total;
  }

  private String formatDateHeader(String recordDate) {
    if (TextUtils.isEmpty(recordDate)) {
      return "日期未知";
    }
    return recordDate;
  }

  private String formatDaySummary(double income, double expense) {
    if (income > 0 && expense > 0) {
      return String.format(Locale.CHINA, "收入 %s  支出 %s", formatMoney(income), formatMoney(expense));
    }
    if (income > 0) {
      return "收入 " + formatMoney(income);
    }
    if (expense > 0) {
      return "支出 " + formatMoney(expense);
    }
    return "";
  }

  private String formatBillDetail(BillRecord record) {
    StringBuilder builder = new StringBuilder();
    builder.append(record.getRecordDate())
        .append("  ")
        .append(safeText(record.getAccountName()));
    if (!TextUtils.isEmpty(record.getRemark())) {
      builder.append("  ").append(record.getRemark());
    }
    return builder.toString();
  }

  private String formatMoney(double amount) {
    return String.format(Locale.CHINA, "¥%.2f", amount);
  }

  private String getCategoryInitial(BillRecord record) {
    String categoryName = safeText(record.getCategoryName());
    if (categoryName.isEmpty()) {
      return "账";
    }
    return categoryName.substring(0, 1);
  }

  private int getColor(int colorRes) {
    return getResources().getColor(colorRes, requireContext().getTheme());
  }

  private String safeText(String text) {
    return TextUtils.isEmpty(text) ? "" : text;
  }

  private String getMonthStart() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    return formatDate(calendar);
  }

  private String getNextMonthStart() {
    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.add(Calendar.MONTH, 1);
    return formatDate(calendar);
  }

  private String formatDate(Calendar calendar) {
    return String.format(
        Locale.CHINA,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH));
  }

  private int dpToPx(int dp) {
    return Math.round(dp * getResources().getDisplayMetrics().density);
  }
}
