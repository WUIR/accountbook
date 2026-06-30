package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.example.accountbook.util.VoucherFileUtils;
import com.bumptech.glide.Glide;

import java.io.File;

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
    contentContainer.setPadding(dp(20), dp(20), dp(20), dp(32));
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
    panel.setBackgroundResource(R.drawable.bg_home_light_card);
    panel.setPadding(dp(18), dp(18), dp(18), dp(18));
    contentContainer.addView(panel);

    panel.addView(createField("类型", BillRecord.TYPE_INCOME.equals(record.getType()) ? "收入" : "支出"));
    panel.addView(createField("金额", MoneyUtils.format(record.getAmount())));
    panel.addView(createField("分类", record.getCategoryName()));
    panel.addView(createField("账户", record.getAccountName()));
    panel.addView(createField("日期", record.getRecordDate()));
    panel.addView(createField("备注", record.getRemark() == null || record.getRemark().isEmpty() ? "无" : record.getRemark()));
    panel.addView(createField("创建时间", formatTime(record.getCreateTime())));
    panel.addView(createVoucherView(record));

    LinearLayout actions = new LinearLayout(requireContext());
    actions.setOrientation(LinearLayout.HORIZONTAL);
    LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    actionParams.setMargins(0, dp(14), 0, 0);
    contentContainer.addView(actions, actionParams);
    actions.addView(createActionButton("编辑账单", true,
        v -> ((MainActivity) requireActivity()).openEditBill(billId)));
    actions.addView(createActionButton("移入回收站", false, v -> confirmDelete()));
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
    TextView back = createText("返回列表", 14, R.color.text_secondary);
    back.setGravity(Gravity.CENTER);
    back.setMinHeight(dp(40));
    back.setPadding(dp(12), 0, 0, 0);
    back.setOnClickListener(v -> ((MainActivity) requireActivity()).backToBillList());
    row.addView(back);
    return row;
  }

  private View createField(String label, String value) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setPadding(0, dp(7), 0, dp(7));
    TextView labelView = createText(label, 15, R.color.text_secondary);
    row.addView(labelView, new LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT));
    TextView valueView = createText(value == null ? "" : value, 15, R.color.text_primary);
    valueView.setGravity(Gravity.END);
    row.addView(valueView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    return row;
  }

  private TextView createActionButton(String text, boolean primary, View.OnClickListener listener) {
    TextView button = createText(text, 15, primary ? R.color.white : R.color.text_primary);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
    params.setMargins(primary ? 0 : dp(10), 0, 0, 0);
    button.setLayoutParams(params);
    button.setBackgroundResource(primary ? R.drawable.bg_save_button : R.drawable.bg_plain_button);
    button.setClickable(true);
    button.setFocusable(true);
    button.setGravity(Gravity.CENTER);
    button.setOnClickListener(listener);
    return button;
  }

  private View createVoucherView(BillRecord record) {
    LinearLayout container = new LinearLayout(requireContext());
    container.setOrientation(LinearLayout.VERTICAL);
    container.setPadding(0, dp(10), 0, 0);
    TextView title = createText("凭证：", 16, R.color.text_primary);
    container.addView(title);
    if (TextUtils.isEmpty(record.getImagePath())) {
      container.addView(createText("暂无凭证", 14, R.color.text_secondary));
      return container;
    }
    if (!VoucherFileUtils.voucherExists(record.getImagePath())) {
      container.addView(createText("凭证图片不存在或已无法读取", 14, R.color.text_secondary));
      return container;
    }
    ImageView imageView = new ImageView(requireContext());
    imageView.setAdjustViewBounds(true);
    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(180));
    params.setMargins(0, dp(8), 0, 0);
    container.addView(imageView, params);
    Glide.with(this)
        .load(new File(record.getImagePath()))
        .into(imageView);
    return container;
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
