package com.example.accountbook.fragment;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.SummaryResult;
import com.example.accountbook.util.CsvExportUtils;
import com.example.accountbook.util.DateUtils;
import com.example.accountbook.util.PdfExportUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExportFragment extends Fragment {

  private BillRecordDao billRecordDao;
  private TextView tvResult;

  @Nullable
  @Override
  public View onCreateView(
      @NonNull android.view.LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    ScrollView scrollView = new ScrollView(requireContext());
    scrollView.setBackgroundColor(getColor(R.color.app_background));
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(root);
    root.addView(createTitleRow());
    root.addView(createText("当前 MVP 导出范围：本月正常账单", 15, R.color.text_secondary));
    Button btnCsv = new Button(requireContext());
    btnCsv.setText("导出本月 CSV");
    btnCsv.setOnClickListener(v -> exportCsv());
    root.addView(btnCsv);
    Button btnPdf = new Button(requireContext());
    btnPdf.setText("导出本月 PDF");
    btnPdf.setOnClickListener(v -> exportPdf());
    root.addView(btnPdf);
    tvResult = createText("暂无导出文件", 14, R.color.text_secondary);
    tvResult.setPadding(0, dp(12), 0, 0);
    root.addView(tvResult);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billRecordDao = new BillRecordDao(requireContext());
  }

  private void exportCsv() {
    try {
      List<BillRecord> records = getMonthRecords();
      SummaryResult summary = billRecordDao.getSummary(DateUtils.getMonthStart(), DateUtils.getNextMonthStart());
      File file = CsvExportUtils.exportBillsToCsv(requireContext(), records, summary, "本月");
      showSuccess(file);
    } catch (IOException e) {
      Toast.makeText(requireContext(), "CSV 导出失败", Toast.LENGTH_SHORT).show();
    }
  }

  private void exportPdf() {
    try {
      List<BillRecord> records = getMonthRecords();
      SummaryResult summary = billRecordDao.getSummary(DateUtils.getMonthStart(), DateUtils.getNextMonthStart());
      File file = PdfExportUtils.exportBillsToPdf(requireContext(), records, summary, "本月");
      showSuccess(file);
    } catch (IOException e) {
      Toast.makeText(requireContext(), "PDF 导出失败", Toast.LENGTH_SHORT).show();
    }
  }

  private List<BillRecord> getMonthRecords() {
    return billRecordDao.getNormalBillRecordsByDateRange(
        DateUtils.getMonthStart(),
        DateUtils.getNextMonthStart());
  }

  private void showSuccess(File file) {
    String message = "导出成功：" + file.getAbsolutePath();
    tvResult.setText(message);
    Toast.makeText(requireContext(), "导出成功", Toast.LENGTH_SHORT).show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("导出账单", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回上一级");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToToolbox());
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
