package com.example.accountbook.fragment;

import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.CategorySummary;
import com.example.accountbook.model.SummaryResult;
import com.example.accountbook.model.TrendItem;
import com.example.accountbook.util.DateUtils;
import com.example.accountbook.util.MoneyUtils;
import com.example.accountbook.view.CategoryPieChartView;
import com.example.accountbook.view.ExpenseTrendChartView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsFragment extends Fragment {

  private enum PeriodMode {
    WEEK,
    MONTH,
    YEAR
  }

  private BillRecordDao billRecordDao;
  private LinearLayout root;
  private PeriodMode currentMode = PeriodMode.MONTH;
  private final Calendar periodAnchor = Calendar.getInstance();

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(32));
    scrollView.addView(root);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billRecordDao = new BillRecordDao(requireContext());
    render();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (billRecordDao != null) {
      render();
    }
  }

  private void render() {
    root.removeAllViews();
    root.addView(createTitleRow());
    root.addView(createPeriodModeControl());
    root.addView(createPeriodSwitchRow());

    String startDate = getPeriodStartDate();
    String endDate = getPeriodEndDateExclusive();
    SummaryResult periodSummary = billRecordDao.getSummary(startDate, endDate);
    root.addView(createSummaryPanel("收支概览", periodSummary));
    root.addView(createTrendPanel(startDate, endDate));

    List<CategorySummary> summaries = billRecordDao.getExpenseCategorySummary(startDate, endDate);
    root.addView(createCategoryChartPanel(summaries));

    LinearLayout ranking = createPanel();
    ranking.addView(createHeader("分类支出排行"));
    if (summaries.isEmpty()) {
      TextView empty = createText("暂无分类支出数据", 15, R.color.text_secondary);
      empty.setGravity(Gravity.CENTER);
      empty.setMinHeight(dp(72));
      ranking.addView(empty);
    } else {
      for (CategorySummary summary : summaries) {
        ranking.addView(createRankingRow(summary));
      }
    }
    root.addView(ranking);
  }

  private View createPeriodModeControl() {
    LinearLayout container = new LinearLayout(requireContext());
    container.setOrientation(LinearLayout.HORIZONTAL);
    container.setGravity(Gravity.CENTER);
    container.setBackgroundResource(R.drawable.bg_segment_container);
    container.setPadding(dp(4), dp(4), dp(4), dp(4));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(18), 0, 0);
    container.setLayoutParams(params);
    container.addView(createModeOption("周", PeriodMode.WEEK));
    container.addView(createModeOption("月", PeriodMode.MONTH));
    container.addView(createModeOption("年", PeriodMode.YEAR));
    return container;
  }

  private TextView createModeOption(String text, PeriodMode mode) {
    TextView option = createText(text, 15, currentMode == mode ? R.color.white : R.color.text_primary);
    option.setGravity(Gravity.CENTER);
    option.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    option.setMinHeight(dp(40));
    option.setBackgroundResource(currentMode == mode
        ? R.drawable.bg_save_button
        : android.R.color.transparent);
    option.setOnClickListener(v -> {
      if (currentMode != mode) {
        currentMode = mode;
        render();
      }
    });
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
    option.setLayoutParams(params);
    return option;
  }

  private View createPeriodSwitchRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(14), 0, 0);
    row.setLayoutParams(params);
    row.addView(createPeriodButton("‹", -1));
    TextView periodText = createText(getPeriodLabel(), 16, R.color.text_primary);
    periodText.setGravity(Gravity.CENTER);
    periodText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
    row.addView(periodText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    row.addView(createPeriodButton("›", 1));
    return row;
  }

  private TextView createPeriodButton(String text, int direction) {
    TextView button = createText(text, 24, R.color.text_primary);
    button.setGravity(Gravity.CENTER);
    button.setMinWidth(dp(44));
    button.setMinHeight(dp(40));
    button.setBackgroundResource(R.drawable.bg_plain_button);
    button.setOnClickListener(v -> {
      shiftPeriod(direction);
      render();
    });
    return button;
  }

  private LinearLayout createSummaryPanel(String title, SummaryResult summary) {
    LinearLayout panel = createPanel();
    panel.addView(createHeader(title));
    panel.addView(createText("收入：" + MoneyUtils.format(summary.getIncome()), 16, R.color.income));
    panel.addView(createText("支出：" + MoneyUtils.format(summary.getExpense()), 16, R.color.expense));
    panel.addView(createText("结余：" + MoneyUtils.format(summary.getBalance()), 16, R.color.text_primary));
    return panel;
  }

  private LinearLayout createCategoryChartPanel(List<CategorySummary> summaries) {
    LinearLayout panel = createPanel();
    panel.addView(createHeader("支出分类占比"));
    CategoryPieChartView pieChartView = new CategoryPieChartView(requireContext());
    pieChartView.setSummaries(summaries);
    LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(190));
    chartParams.setMargins(0, dp(8), 0, 0);
    panel.addView(pieChartView, chartParams);
    return panel;
  }

  private LinearLayout createTrendPanel(String startDate, String endDate) {
    LinearLayout panel = createPanel();
    panel.addView(createHeader("支出趋势"));
    ExpenseTrendChartView trendChartView = new ExpenseTrendChartView(requireContext());
    trendChartView.setItems(buildTrendItems(startDate, endDate));
    LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(190));
    chartParams.setMargins(0, dp(8), 0, 0);
    panel.addView(trendChartView, chartParams);
    return panel;
  }

  private LinearLayout createPanel() {
    LinearLayout panel = new LinearLayout(requireContext());
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setBackgroundResource(R.drawable.bg_home_light_card);
    panel.setPadding(dp(18), dp(14), dp(18), dp(14));
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(14), 0, 0);
    panel.setLayoutParams(params);
    return panel;
  }

  private TextView createHeader(String text) {
    TextView header = createText(text, 18, R.color.text_primary);
    header.setTypeface(null, android.graphics.Typeface.BOLD);
    return header;
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("统计", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    return row;
  }

  private String getPeriodStartDate() {
    if (currentMode == PeriodMode.WEEK) {
      return DateUtils.getWeekStart(periodAnchor);
    } else if (currentMode == PeriodMode.YEAR) {
      return DateUtils.getYearStart(periodAnchor);
    }
    return DateUtils.getMonthStart(periodAnchor);
  }

  private String getPeriodEndDateExclusive() {
    if (currentMode == PeriodMode.WEEK) {
      return DateUtils.getNextWeekStart(periodAnchor);
    } else if (currentMode == PeriodMode.YEAR) {
      return DateUtils.getNextYearStart(periodAnchor);
    }
    return DateUtils.getNextMonthStart(periodAnchor);
  }

  private String getPeriodLabel() {
    if (currentMode == PeriodMode.WEEK) {
      return DateUtils.formatWeekPeriod(periodAnchor);
    } else if (currentMode == PeriodMode.YEAR) {
      return DateUtils.formatYearPeriod(periodAnchor);
    }
    return DateUtils.formatMonthPeriod(periodAnchor);
  }

  private void shiftPeriod(int direction) {
    if (currentMode == PeriodMode.WEEK) {
      periodAnchor.add(Calendar.DAY_OF_MONTH, direction * 7);
    } else if (currentMode == PeriodMode.YEAR) {
      periodAnchor.add(Calendar.YEAR, direction);
    } else {
      periodAnchor.add(Calendar.MONTH, direction);
    }
  }

  private List<TrendItem> buildTrendItems(String startDate, String endDate) {
    String groupMode = currentMode == PeriodMode.YEAR
        ? BillRecordDao.TREND_GROUP_MONTH
        : BillRecordDao.TREND_GROUP_DAY;
    List<TrendItem> rawItems = billRecordDao.getExpenseTrend(startDate, endDate, groupMode);
    Map<String, Double> amountByLabel = new HashMap<>();
    for (TrendItem item : rawItems) {
      amountByLabel.put(item.getLabel(), item.getAmount());
    }
    if (currentMode == PeriodMode.YEAR) {
      return buildYearTrendItems(amountByLabel);
    }
    return buildDayTrendItems(amountByLabel);
  }

  private List<TrendItem> buildDayTrendItems(Map<String, Double> amountByDate) {
    List<TrendItem> items = new ArrayList<>();
    Calendar cursor = getPeriodStartCalendar();
    Calendar end = getPeriodEndCalendar();
    while (cursor.before(end)) {
      String dateKey = DateUtils.formatDate(cursor);
      String label = currentMode == PeriodMode.WEEK
          ? String.valueOf(cursor.get(Calendar.DAY_OF_MONTH))
          : String.valueOf(cursor.get(Calendar.DAY_OF_MONTH));
      items.add(new TrendItem(label, getAmount(amountByDate, dateKey)));
      cursor.add(Calendar.DAY_OF_MONTH, 1);
    }
    return items;
  }

  private List<TrendItem> buildYearTrendItems(Map<String, Double> amountByMonth) {
    List<TrendItem> items = new ArrayList<>();
    int year = periodAnchor.get(Calendar.YEAR);
    for (int month = 1; month <= 12; month++) {
      String key = String.format(Locale.CHINA, "%04d-%02d", year, month);
      items.add(new TrendItem(month + "月", getAmount(amountByMonth, key)));
    }
    return items;
  }

  private Calendar getPeriodStartCalendar() {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(periodAnchor.getTime());
    if (currentMode == PeriodMode.WEEK) {
      int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
      int diffToMonday = dayOfWeek == Calendar.SUNDAY ? -6 : Calendar.MONDAY - dayOfWeek;
      calendar.add(Calendar.DAY_OF_MONTH, diffToMonday);
    } else if (currentMode == PeriodMode.YEAR) {
      calendar.set(Calendar.MONTH, Calendar.JANUARY);
      calendar.set(Calendar.DAY_OF_MONTH, 1);
    } else {
      calendar.set(Calendar.DAY_OF_MONTH, 1);
    }
    return calendar;
  }

  private Calendar getPeriodEndCalendar() {
    Calendar calendar = getPeriodStartCalendar();
    if (currentMode == PeriodMode.WEEK) {
      calendar.add(Calendar.DAY_OF_MONTH, 7);
    } else if (currentMode == PeriodMode.YEAR) {
      calendar.add(Calendar.YEAR, 1);
    } else {
      calendar.add(Calendar.MONTH, 1);
    }
    return calendar;
  }

  private double getAmount(Map<String, Double> amountByLabel, String label) {
    Double amount = amountByLabel.get(label);
    return amount == null ? 0 : amount;
  }

  private View createRankingRow(CategorySummary summary) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setPadding(0, dp(8), 0, dp(8));
    TextView name = createText(summary.getCategoryName(), 15, R.color.text_primary);
    row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    TextView amount = createText(
        String.format(Locale.CHINA, "%s  %.0f%%", MoneyUtils.format(summary.getAmount()), summary.getRatio() * 100),
        15,
        R.color.text_primary);
    amount.setGravity(Gravity.END);
    row.addView(amount);
    return row;
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
