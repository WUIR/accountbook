package com.example.accountbook.fragment;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {

  private static final String HITOKOTO_URL = "https://v1.hitokoto.cn/?encode=json&max_length=24";
  private static final int HITOKOTO_MAX_LENGTH = 24;
  private static final int HITOKOTO_TIMEOUT_MS = 3000;
  private static final long HITOKOTO_FIRST_REQUEST_DELAY_MS = 3000L;
  private static final long HITOKOTO_REFRESH_INTERVAL_MS = 10_000L;
  private static final long HITOKOTO_VISIBLE_BEFORE_SCROLL_MS = 1200L;
  private static final long HITOKOTO_SCROLL_DURATION_MS = 7000L;

  private TextView tvHomeSubtitle;
  private TextView tvMonthlyBalance;
  private TextView tvMonthlyBalanceLabel;
  private TextView tvMonthlyIncome;
  private TextView tvMonthlyIncomeLabel;
  private TextView tvMonthlyExpense;
  private TextView tvMonthlyExpenseLabel;
  private LinearLayout recentBillsContainer;
  private BillRecordDao billRecordDao;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private final ExecutorService hitokotoExecutor = Executors.newSingleThreadExecutor();
  private final Runnable hitokotoScrollRunnable = this::startHitokotoScrollAnimation;
  private final Runnable hitokotoRefreshRunnable = () -> {
    requestHitokoto();
    scheduleNextHitokotoRequest();
  };
  private final AtomicInteger hitokotoRequestVersion = new AtomicInteger();
  private boolean hitokotoPolling;
  private boolean hitokotoRequestRunning;
  private ObjectAnimator hitokotoAnimator;

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
    tvHomeSubtitle = view.findViewById(R.id.tvHomeSubtitle);
    tvHomeSubtitle.setText("");
    tvHomeSubtitle.setVisibility(View.INVISIBLE);
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
    startHitokotoPolling();
  }

  @Override
  public void onPause() {
    stopHitokotoPolling();
    super.onPause();
  }

  @Override
  public void onDestroyView() {
    stopHitokotoPolling();
    if (tvHomeSubtitle != null) {
      cancelHitokotoAnimation();
      tvHomeSubtitle = null;
    }
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    hitokotoExecutor.shutdownNow();
    super.onDestroy();
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

  private void startHitokotoPolling() {
    if (hitokotoPolling) {
      return;
    }
    hitokotoPolling = true;
    mainHandler.postDelayed(hitokotoRefreshRunnable, HITOKOTO_FIRST_REQUEST_DELAY_MS);
  }

  private void stopHitokotoPolling() {
    hitokotoPolling = false;
    mainHandler.removeCallbacks(hitokotoRefreshRunnable);
    mainHandler.removeCallbacks(hitokotoScrollRunnable);
    cancelHitokotoAnimation();
  }

  private void requestHitokoto() {
    if (!hitokotoPolling || hitokotoRequestRunning || hitokotoExecutor.isShutdown()) {
      return;
    }
    hitokotoRequestRunning = true;
    int requestVersion = hitokotoRequestVersion.incrementAndGet();
    hitokotoExecutor.execute(() -> {
      String hitokoto = fetchHitokoto();
      mainHandler.post(() -> {
        hitokotoRequestRunning = false;
        if (!canUpdateHitokoto() || requestVersion != hitokotoRequestVersion.get()) {
          return;
        }
        if (!TextUtils.isEmpty(hitokoto)) {
          updateHitokotoText(hitokoto);
        }
      });
    });
  }

  private void scheduleNextHitokotoRequest() {
    mainHandler.removeCallbacks(hitokotoRefreshRunnable);
    if (hitokotoPolling) {
      mainHandler.postDelayed(hitokotoRefreshRunnable, HITOKOTO_REFRESH_INTERVAL_MS);
    }
  }

  @Nullable
  private String fetchHitokoto() {
    HttpURLConnection connection = null;
    try {
      URL url = new URL(HITOKOTO_URL + "&t=" + System.currentTimeMillis());
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(HITOKOTO_TIMEOUT_MS);
      connection.setReadTimeout(HITOKOTO_TIMEOUT_MS);
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("User-Agent", "AccountBookAndroid/1.0");
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        return null;
      }
      String response = readStream(connection.getInputStream());
      JSONObject jsonObject = new JSONObject(response);
      String hitokoto = jsonObject.optString("hitokoto", "").trim();
      if (hitokoto.length() == 0 || hitokoto.length() > HITOKOTO_MAX_LENGTH) {
        return null;
      }
      return hitokoto;
    } catch (Exception ignored) {
      return null;
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  private String readStream(InputStream inputStream) throws Exception {
    StringBuilder builder = new StringBuilder();
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    String line;
    while ((line = reader.readLine()) != null) {
      builder.append(line);
    }
    reader.close();
    return builder.toString();
  }

  private boolean canUpdateHitokoto() {
    return hitokotoPolling && isAdded() && getView() != null && tvHomeSubtitle != null;
  }

  private void updateHitokotoText(String text) {
    if (tvHomeSubtitle == null) {
      return;
    }
    mainHandler.removeCallbacks(hitokotoScrollRunnable);
    cancelHitokotoAnimation();
    tvHomeSubtitle.setText(text);
    tvHomeSubtitle.setVisibility(View.VISIBLE);
    tvHomeSubtitle.setTranslationX(0);
    mainHandler.postDelayed(hitokotoScrollRunnable, HITOKOTO_VISIBLE_BEFORE_SCROLL_MS);
  }

  private void startHitokotoScrollAnimation() {
    if (tvHomeSubtitle == null) {
      return;
    }
    int parentWidth = ((View) tvHomeSubtitle.getParent()).getWidth();
    int textWidth = (int) tvHomeSubtitle.getPaint().measureText(tvHomeSubtitle.getText().toString());
    if (parentWidth <= 0 || textWidth <= 0) {
      return;
    }
    float startX = parentWidth;
    float endX = -textWidth;
    cancelHitokotoAnimation();
    hitokotoAnimator = ObjectAnimator.ofFloat(tvHomeSubtitle, "translationX", startX, endX);
    hitokotoAnimator.setDuration(HITOKOTO_SCROLL_DURATION_MS);
    hitokotoAnimator.setRepeatCount(ValueAnimator.INFINITE);
    hitokotoAnimator.start();
  }

  private void cancelHitokotoAnimation() {
    if (hitokotoAnimator != null) {
      hitokotoAnimator.cancel();
      hitokotoAnimator = null;
    }
    if (tvHomeSubtitle != null) {
      tvHomeSubtitle.setTranslationX(0);
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
    itemView.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

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
    titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
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
