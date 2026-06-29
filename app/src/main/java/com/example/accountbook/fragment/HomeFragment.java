package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
  private TextView tvMonthlyIncome;
  private TextView tvMonthlyExpense;
  private ProgressBar progressBudget;
  private TextView tvBudgetStatus;
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
    tvMonthlyIncome = view.findViewById(R.id.tvMonthlyIncome);
    tvMonthlyExpense = view.findViewById(R.id.tvMonthlyExpense);
    progressBudget = view.findViewById(R.id.progressBudget);
    tvBudgetStatus = view.findViewById(R.id.tvBudgetStatus);
    recentBillsContainer = view.findViewById(R.id.recentBillsContainer);
    view.findViewById(R.id.btnAllBills)
        .setOnClickListener(v -> ((MainActivity) requireActivity()).openBillList());
    view.findViewById(R.id.btnStatistics)
        .setOnClickListener(v -> ((MainActivity) requireActivity()).openStatistics());
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
    tvMonthlyIncome.setText(getString(R.string.month_income_value, income));
    tvMonthlyExpense.setText(getString(R.string.month_expense_value, expense));
    tvMonthlyBalance.setText(formatMoney(income - expense));
    refreshBudgetStatus(expense);
    refreshRecentBills();
  }

  private void refreshBudgetStatus(double expense) {
    double monthlyBudget = PreferenceUtils.getMonthlyBudget(requireContext());
    boolean warnEnabled = PreferenceUtils.isBudgetWarnEnabled(requireContext());
    if (monthlyBudget <= 0) {
      progressBudget.setProgress(0);
      tvBudgetStatus.setText(R.string.budget_not_set);
      return;
    }
    double ratio = expense / monthlyBudget;
    int progress = (int) Math.min(100, Math.round(ratio * 100));
    progressBudget.setProgress(progress);
    if (!warnEnabled) {
      tvBudgetStatus.setText(getString(R.string.budget_progress_value, progress));
    } else if (ratio >= 1) {
      tvBudgetStatus.setText(getString(R.string.budget_over_limit, progress));
    } else if (ratio >= 0.8) {
      tvBudgetStatus.setText(getString(R.string.budget_warning, progress));
    } else {
      tvBudgetStatus.setText(getString(R.string.budget_normal, progress));
    }
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
    for (BillRecord record : records) {
      recentBillsContainer.addView(createBillItemView(record));
    }
  }

  private View createBillItemView(BillRecord record) {
    LinearLayout itemView = new LinearLayout(requireContext());
    itemView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    itemView.setOrientation(LinearLayout.VERTICAL);
    itemView.setPadding(0, dpToPx(8), 0, dpToPx(8));

    TextView titleView = createTextView(formatBillTitle(record), 16);
    titleView.setTextColor(getResources().getColor(R.color.text_primary, requireContext().getTheme()));
    TextView detailView = createTextView(formatBillDetail(record), 13);
    detailView.setTextColor(getResources().getColor(R.color.text_secondary, requireContext().getTheme()));
    itemView.addView(titleView);
    itemView.addView(detailView);
    return itemView;
  }

  private TextView createTextView(String text, int textSizeSp) {
    TextView textView = new TextView(requireContext());
    textView.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    textView.setText(text);
    textView.setTextSize(textSizeSp);
    textView.setTextColor(getResources().getColor(R.color.text_secondary, requireContext().getTheme()));
    return textView;
  }

  private String formatBillTitle(BillRecord record) {
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    return String.format(
        Locale.CHINA,
        "%s  %s%s",
        safeText(record.getCategoryName()),
        sign,
        formatMoney(record.getAmount()));
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
