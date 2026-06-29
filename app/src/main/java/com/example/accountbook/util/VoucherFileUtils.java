package com.example.accountbook.util;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class VoucherFileUtils {

  private static final String VOUCHER_DIR = "vouchers";

  private VoucherFileUtils() {
  }

  public static String copyVoucherToPrivateDir(Context context, Uri sourceUri) throws IOException {
    File dir = getVoucherDir(context);
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Cannot create voucher directory");
    }
    File target = new File(dir, "voucher_" + System.currentTimeMillis() + ".jpg");
    try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
         FileOutputStream outputStream = new FileOutputStream(target)) {
      if (inputStream == null) {
        throw new IOException("Cannot open selected image");
      }
      byte[] buffer = new byte[8192];
      int length;
      while ((length = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, length);
      }
    }
    return target.getAbsolutePath();
  }

  public static void deleteVoucherFile(Context context, String imagePath) {
    if (TextUtils.isEmpty(imagePath)) {
      return;
    }
    File file = new File(imagePath);
    File voucherDir = getVoucherDir(context);
    try {
      String filePath = file.getCanonicalPath();
      String dirPath = voucherDir.getCanonicalPath();
      if (filePath.startsWith(dirPath) && file.exists()) {
        file.delete();
      }
    } catch (IOException ignored) {
      // Best-effort cleanup only.
    }
  }

  public static boolean voucherExists(String imagePath) {
    return !TextUtils.isEmpty(imagePath) && new File(imagePath).exists();
  }

  private static File getVoucherDir(Context context) {
    return new File(context.getFilesDir(), VOUCHER_DIR);
  }
}
