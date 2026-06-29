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
import com.example.accountbook.util.MoneyUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BillDetailFragment extends Fragment {

  private static final String ARG_BILL_ID = "bill_id";
  private BillRecordDao billRecordDao;
  private long billId;
  private LinearLayout contentContainer;

  public static BillDetailFragment newInstance(long billId) {
    BillDetailFragment fragment = new BillDetailFragment();
    Bundle args = new Bundle();
    args.putLong(ARG_BILL_ID, billId);
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    contentContainer = new LinearLayout(requireContext());
    contentContainer.setOrientation(LinearLayout.VERTICAL);
    contentContainer.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(contentContainer);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billId = requireArguments().getLong(ARG_BILL_ID);
    billRecordDao = new BillRecordDao(requireContext());
    render();
  }

  private void render() {
    BillRecord record = billRecordDao.getBillRecordById(billId, false);
    contentContainer.removeAllViews();
    contentContainer.addView(createTitleRow());
    if (record == null) {
      TextView emptyView = createText("账单不存在或已进入回收站", 16, R.color.text_secondary);
      emptyView.setGravity(Gravity.CENTER);
      emptyView.setMinHeight(dp(120));
      contentContainer.addView(emptyView);
      return;
    }
    LinearLayout panel = new LinearLayout(requireContext());
    panel.setOrientation(LinearLayout.VERTICAL);
    panel.setBackgroundResource(R.drawable.bg_panel);
    panel.setPadding(dp(18), dp(18), dp(18), dp(18));
    contentContainer.addView(panel);

    panel.addView(createField("类型", BillRecord.TYPE_INCOME.equals(record.getType()) ? "收入" : "支出"));
    panel.addView(createField("金额", MoneyUtils.format(record.getAmount())));
    panel.addView(createField("分类", record.getCategoryName()));
    panel.addView(createField("账户", record.getAccountName()));
    panel.addView(createField("日期", record.getRecordDate()));
    panel.addView(createField("备注", record.getRemark() == null || record.getRemark().isEmpty() ? "无" : record.getRemark()));
    panel.addView(createField("创建时间", formatTime(record.getCreateTime())));

    Button btnEdit = new Button(requireContext());
    btnEdit.setText("编辑账单");
    btnEdit.setOnClickListener(v -> ((MainActivity) requireActivity()).openEditBill(billId));
    contentContainer.addView(btnEdit);

    Button btnDelete = new Button(requireContext());
    btnDelete.setText("移入回收站");
    btnDelete.setOnClickListener(v -> confirmDelete());
    contentContainer.addView(btnDelete);
  }

  private void confirmDelete() {
    new AlertDialog.Builder(requireContext())
        .setTitle("删除账单")
        .setMessage("确认将该账单移入 7 天回收站？")
        .setPositiveButton("删除", (dialog, which) -> {
          boolean success = billRecordDao.moveToRecycleBin(billId, System.currentTimeMillis());
          Toast.makeText(requireContext(), success ? "已移入回收站" : "删除失败", Toast.LENGTH_SHORT).show();
          if (success) {
            ((MainActivity) requireActivity()).backToBillList();
          }
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("账单详情", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回列表");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToBillList());
    row.addView(button);
    return row;
  }

  private TextView createField(String label, String value) {
    return createText(label + "：" + (value == null ? "" : value), 16, R.color.text_primary);
  }

  private TextView createText(String text, int sp, int colorRes) {
    TextView textView = new TextView(requireContext());
    textView.setPadding(0, dp(7), 0, dp(7));
    textView.setText(text);
    textView.setTextSize(sp);
    textView.setTextColor(getColor(colorRes));
    return textView;
  }

  private String formatTime(long createTime) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(createTime));
  }

  private int getColor(int resId) {
    return getResources().getColor(resId, requireContext().getTheme());
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
