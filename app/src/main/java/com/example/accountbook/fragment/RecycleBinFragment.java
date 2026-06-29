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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.util.DateUtils;
import com.example.accountbook.util.MoneyUtils;

import java.util.List;

public class RecycleBinFragment extends Fragment {

  private BillRecordDao billRecordDao;
  private LinearLayout listContainer;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(root);
    root.addView(createTitleRow());
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
    billRecordDao = new BillRecordDao(requireContext());
    refreshList();
  }

  private void refreshList() {
    billRecordDao.cleanupExpiredRecycleBinRecords(DateUtils.getEarliestRecycleDeletedAt());
    List<BillRecord> records = billRecordDao.getRecycleBinRecords(DateUtils.getEarliestRecycleDeletedAt());
    listContainer.removeAllViews();
    if (records.isEmpty()) {
      TextView empty = createText("回收站暂无账单", 15, R.color.text_secondary);
      empty.setGravity(Gravity.CENTER);
      empty.setMinHeight(dp(96));
      listContainer.addView(empty);
      return;
    }
    for (BillRecord record : records) {
      listContainer.addView(createItem(record));
    }
  }

  private View createItem(BillRecord record) {
    LinearLayout item = new LinearLayout(requireContext());
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(0, dp(10), 0, dp(10));
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    item.addView(createText(record.getCategoryName() + "  " + sign + MoneyUtils.format(record.getAmount()),
        16, R.color.text_primary));
    item.addView(createText(record.getRecordDate() + "  " + record.getAccountName()
        + "  剩余 " + DateUtils.getRecycleDaysLeft(record.getDeletedAt()) + " 天",
        13, R.color.text_secondary));
    LinearLayout actions = new LinearLayout(requireContext());
    Button restore = new Button(requireContext());
    restore.setText("恢复");
    restore.setOnClickListener(v -> {
      boolean success = billRecordDao.restoreFromRecycleBin(record.getId());
      Toast.makeText(requireContext(), success ? "已恢复" : "恢复失败", Toast.LENGTH_SHORT).show();
      refreshList();
    });
    Button delete = new Button(requireContext());
    delete.setText("彻底删除");
    delete.setOnClickListener(v -> confirmDelete(record.getId()));
    actions.addView(restore, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    actions.addView(delete, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    item.addView(actions);
    return item;
  }

  private void confirmDelete(long billId) {
    new AlertDialog.Builder(requireContext())
        .setTitle("彻底删除")
        .setMessage("彻底删除后不可恢复，确认删除？")
        .setPositiveButton("删除", (dialog, which) -> {
          boolean success = billRecordDao.permanentlyDelete(billId);
          Toast.makeText(requireContext(), success ? "已彻底删除" : "删除失败", Toast.LENGTH_SHORT).show();
          refreshList();
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("回收站", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回我的");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToMine());
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
