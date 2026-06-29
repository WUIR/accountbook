package com.example.accountbook.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.CategorySummary;
import com.example.accountbook.model.SummaryResult;
import com.example.accountbook.util.DateUtils;
import com.example.accountbook.util.MoneyUtils;
import com.example.accountbook.view.CategoryPieChartView;

import java.util.List;
import java.util.Locale;

public class StatisticsFragment extends Fragment {

  private BillRecordDao billRecordDao;
  private LinearLayout root;

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
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
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
    SummaryResult week = billRecordDao.getSummary(DateUtils.getWeekStart(), DateUtils.getNextWeekStart());
    SummaryResult month = billRecordDao.getSummary(DateUtils.getMonthStart(), DateUtils.getNextMonthStart());
    root.addView(createSummaryPanel("本周统计", week));
    root.addView(createSummaryPanel("本月统计", month));

    List<CategorySummary> summaries = billRecordDao.getExpenseCategorySummary(
        DateUtils.getMonthStart(),
        DateUtils.getNextMonthStart());
    CategoryPieChartView pieChartView = new CategoryPieChartView(requireContext());
    pieChartView.setSummaries(summaries);
    LinearLayout.LayoutParams chartParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(190));
    chartParams.setMargins(0, dp(14), 0, 0);
    root.addView(pieChartView, chartParams);

    LinearLayout ranking = createPanel();
    ranking.addView(createHeader("本月分类支出排行"));
    if (summaries.isEmpty()) {
      TextView empty = createText("暂无分类支出数据", 15, R.color.text_secondary);
      empty.setGravity(Gravity.CENTER);
      empty.setMinHeight(dp(72));
      ranking.addView(empty);
    } else {
      for (CategorySummary summary : summaries) {
        ranking.addView(createText(
            String.format(
                Locale.CHINA,
                "%s  %s  %.0f%%",
                summary.getCategoryName(),
                MoneyUtils.format(summary.getAmount()),
                summary.getRatio() * 100),
            16,
            R.color.text_primary));
      }
    }
    root.addView(ranking);
  }

  private LinearLayout createSummaryPanel(String title, SummaryResult summary) {
    LinearLayout panel = createPanel();
    panel.addView(createHeader(title));
    panel.addView(createText("收入：" + MoneyUtils.format(summary.getIncome()), 16, R.color.income));
    panel.addView(createText("支出：" + MoneyUtils.format(summary.getExpense()), 16, R.color.expense));
    panel.addView(createText("结余：" + MoneyUtils.format(summary.getBalance()), 16, R.color.text_primary));
    return panel;
  }

  private LinearLayout createPanel() {
    LinearLayout panel = new LinearLayout(requireContext());
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setBackgroundResource(R.drawable.bg_panel);
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
    Button button = new Button(requireContext());
    button.setText("返回首页");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToHome());
    row.addView(button);
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
