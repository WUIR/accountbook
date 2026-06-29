package com.example.accountbook.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;

import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.SummaryResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PdfExportUtils {

  private static final int PAGE_WIDTH = 595;
  private static final int PAGE_HEIGHT = 842;
  private static final int MARGIN = 40;

  private PdfExportUtils() {
  }

  public static File exportBillsToPdf(
      Context context,
      List<BillRecord> records,
      SummaryResult summary,
      String rangeText) throws IOException {
    File file = ExportFileUtils.createExportFile(context, "pdf");
    PdfDocument document = new PdfDocument();
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    paint.setTextSize(12);
    int pageNumber = 1;
    PdfDocument.Page page = newPage(document, pageNumber);
    Canvas canvas = page.getCanvas();
    int y = MARGIN;
    paint.setTextSize(20);
    canvas.drawText("轻记账账单导出", MARGIN, y, paint);
    y += 30;
    paint.setTextSize(12);
    canvas.drawText("范围: " + rangeText, MARGIN, y, paint);
    y += 20;
    canvas.drawText("导出时间: " + formatTime(System.currentTimeMillis()), MARGIN, y, paint);
    y += 20;
    canvas.drawText("收入: " + MoneyUtils.format(summary.getIncome())
        + "  支出: " + MoneyUtils.format(summary.getExpense())
        + "  结余: " + MoneyUtils.format(summary.getBalance()), MARGIN, y, paint);
    y += 28;
    canvas.drawText("账单明细", MARGIN, y, paint);
    y += 20;
    for (BillRecord record : records) {
      if (y > PAGE_HEIGHT - MARGIN) {
        document.finishPage(page);
        pageNumber++;
        page = newPage(document, pageNumber);
        canvas = page.getCanvas();
        y = MARGIN;
      }
      String type = BillRecord.TYPE_INCOME.equals(record.getType()) ? "收入" : "支出";
      String line = record.getRecordDate() + "  " + type + "  "
          + record.getCategoryName() + "  " + record.getAccountName() + "  "
          + MoneyUtils.format(record.getAmount());
      canvas.drawText(line, MARGIN, y, paint);
      y += 18;
      String remark = record.getRemark();
      if (remark != null && !remark.isEmpty()) {
        canvas.drawText("备注: " + remark, MARGIN + 16, y, paint);
        y += 18;
      }
    }
    document.finishPage(page);
    try (FileOutputStream outputStream = new FileOutputStream(file)) {
      document.writeTo(outputStream);
    } finally {
      document.close();
    }
    return file;
  }

  private static PdfDocument.Page newPage(PdfDocument document, int pageNumber) {
    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
        PAGE_WIDTH,
        PAGE_HEIGHT,
        pageNumber).create();
    return document.startPage(pageInfo);
  }

  private static String formatTime(long timeMillis) {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date(timeMillis));
  }
}
