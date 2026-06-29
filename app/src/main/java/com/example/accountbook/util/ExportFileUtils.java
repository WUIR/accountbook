package com.example.accountbook.util;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ExportFileUtils {

  private ExportFileUtils() {
  }

  public static File createExportFile(Context context, String extension) {
    File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
    if (dir == null) {
      dir = new File(context.getFilesDir(), "exports");
    }
    if (!dir.exists()) {
      dir.mkdirs();
    }
    String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
    return new File(dir, "accountbook_bills_" + timestamp + "." + extension);
  }
}
