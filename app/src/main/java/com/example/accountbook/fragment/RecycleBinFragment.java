package com.example.accountbook.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.accountbook.util.VoucherFileUtils;

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
    root.setPadding(dp(20), dp(20), dp(20), dp(32));
    scrollView.addView(root);
    root.addView(createTitleRow());
    listContainer = new LinearLayout(requireContext());
    listContainer.setOrientation(LinearLayout.VERTICAL);
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
    long earliestDeletedAt = DateUtils.getEarliestRecycleDeletedAt();
    cleanupExpiredRecords(earliestDeletedAt);
    List<BillRecord> records = billRecordDao.getRecycleBinRecords(earliestDeletedAt);
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
    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    itemParams.setMargins(0, 0, 0, dp(10));
    item.setLayoutParams(itemParams);
    item.setBackgroundResource(R.drawable.bg_home_light_card);
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(dp(14), dp(12), dp(14), dp(12));
    String sign = BillRecord.TYPE_INCOME.equals(record.getType()) ? "+" : "-";
    item.addView(createText(record.getCategoryName() + "  " + sign + MoneyUtils.format(record.getAmount()),
        16, R.color.text_primary));
    item.addView(createText(record.getRecordDate() + "  " + record.getAccountName()
        + "  剩余 " + DateUtils.getRecycleDaysLeft(record.getDeletedAt()) + " 天",
        13, R.color.text_secondary));
    LinearLayout actions = new LinearLayout(requireContext());
    actions.setPadding(0, dp(8), 0, 0);
    TextView restore = createActionButton("恢复", true);
    restore.setOnClickListener(v -> {
      boolean success = billRecordDao.restoreFromRecycleBin(record.getId());
      Toast.makeText(requireContext(), success ? "已恢复" : "恢复失败", Toast.LENGTH_SHORT).show();
      refreshList();
    });
    TextView delete = createActionButton("彻底删除", false);
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
          BillRecord record = billRecordDao.getBillRecordById(billId, true);
          boolean success = billRecordDao.permanentlyDelete(billId);
          if (success && record != null) {
            VoucherFileUtils.deleteVoucherFile(requireContext(), record.getImagePath());
          }
          Toast.makeText(requireContext(), success ? "已彻底删除" : "删除失败", Toast.LENGTH_SHORT).show();
          refreshList();
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private void cleanupExpiredRecords(long earliestDeletedAt) {
    List<BillRecord> expiredRecords = billRecordDao.getExpiredRecycleBinRecords(earliestDeletedAt);
    int deletedCount = billRecordDao.cleanupExpiredRecycleBinRecords(earliestDeletedAt);
    if (deletedCount > 0) {
      for (BillRecord record : expiredRecords) {
        VoucherFileUtils.deleteVoucherFile(requireContext(), record.getImagePath());
      }
    }
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("回收站", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    TextView back = createText("返回上一级", 14, R.color.text_secondary);
    back.setGravity(Gravity.CENTER);
    back.setMinHeight(dp(40));
    back.setPadding(dp(12), 0, 0, 0);
    back.setOnClickListener(v -> ((MainActivity) requireActivity()).backToToolbox());
    row.addView(back);
    return row;
  }

  private TextView createActionButton(String text, boolean primary) {
    TextView button = createText(text, 14, primary ? R.color.white : R.color.text_primary);
    button.setBackgroundResource(primary ? R.drawable.bg_save_button : R.drawable.bg_plain_button);
    button.setClickable(true);
    button.setFocusable(true);
    button.setGravity(Gravity.CENTER);
    return button;
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
