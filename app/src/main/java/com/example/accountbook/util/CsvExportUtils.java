package com.example.accountbook.util;

import android.content.Context;

import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.SummaryResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CsvExportUtils {

  private CsvExportUtils() {
  }

  public static File exportBillsToCsv(
      Context context,
      List<BillRecord> records,
      SummaryResult summary,
      String rangeText) throws IOException {
    File file = ExportFileUtils.createExportFile(context, "csv");
    try (FileOutputStream fileOutputStream = new FileOutputStream(file);
         OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
      fileOutputStream.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
      writer.write("轻记账账单导出\n");
      writer.write("范围," + escapeCsv(rangeText) + "\n");
      writer.write("导出时间," + escapeCsv(formatTime(System.currentTimeMillis())) + "\n");
      writer.write("收入合计," + summary.getIncome() + "\n");
      writer.write("支出合计," + summary.getExpense() + "\n");
      writer.write("结余," + summary.getBalance() + "\n\n");
      writer.write("账单ID,收支类型,金额,分类名称,账户名称,账单日期,备注,创建时间\n");
      for (BillRecord record : records) {
        writer.write(record.getId() + ",");
        writer.write(escapeCsv(BillRecord.TYPE_INCOME.equals(record.getType()) ? "收入" : "支出") + ",");
        writer.write(record.getAmount() + ",");
        writer.write(escapeCsv(record.getCategoryName()) + ",");
        writer.write(escapeCsv(record.getAccountName()) + ",");
        writer.write(escapeCsv(record.getRecordDate()) + ",");
        writer.write(escapeCsv(record.getRemark()) + ",");
        writer.write(escapeCsv(formatTime(record.getCreateTime())) + "\n");
      }
    }
    return file;
  }

  public static String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    boolean needsQuote = value.contains(",") || value.contains("\n") || value.contains("\"");
    String escaped = value.replace("\"", "\"\"");
    return needsQuote ? "\"" + escaped + "\"" : escaped;
  }

  private static String formatTime(long timeMillis) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timeMillis));
  }
}
