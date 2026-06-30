package com.example.accountbook.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.accountbook.model.TrendItem;

import java.util.ArrayList;
import java.util.List;

public class ExpenseTrendChartView extends View {

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF barRect = new RectF();
  private List<TrendItem> items = new ArrayList<>();

  public ExpenseTrendChartView(Context context) {
    super(context);
  }

  public ExpenseTrendChartView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setItems(List<TrendItem> items) {
    this.items = items == null ? new ArrayList<>() : items;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int desiredHeight = Math.round(190 * getResources().getDisplayMetrics().density);
    setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int width = getWidth();
    int height = getHeight();
    if (items.isEmpty() || getMaxAmount() <= 0) {
      paint.setColor(Color.rgb(96, 112, 128));
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(sp(15));
      canvas.drawText("暂无支出趋势数据", width / 2f, height / 2f, paint);
      return;
    }

    float left = dp(10);
    float right = width - dp(10);
    float top = dp(12);
    float bottom = height - dp(30);
    float chartHeight = bottom - top;
    double maxAmount = getMaxAmount();
    int count = items.size();
    float slotWidth = (right - left) / count;
    float barWidth = Math.max(dp(3), Math.min(dp(18), slotWidth * 0.58f));

    paint.setStrokeWidth(dp(1));
    paint.setColor(Color.rgb(226, 232, 228));
    canvas.drawLine(left, bottom, right, bottom, paint);

    for (int i = 0; i < count; i++) {
      TrendItem item = items.get(i);
      float centerX = left + slotWidth * i + slotWidth / 2f;
      float barHeight = (float) (chartHeight * item.getAmount() / maxAmount);
      float barTop = bottom - Math.max(dp(4), barHeight);
      paint.setColor(Color.rgb(57, 184, 90));
      barRect.set(centerX - barWidth / 2f, barTop, centerX + barWidth / 2f, bottom);
      canvas.drawRoundRect(barRect, dp(4), dp(4), paint);

      if (shouldDrawLabel(i, count)) {
        paint.setColor(Color.rgb(96, 112, 128));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(11));
        canvas.drawText(item.getLabel(), centerX, height - dp(8), paint);
      }
    }
  }

  private boolean shouldDrawLabel(int index, int count) {
    if (count <= 12) {
      return true;
    }
    return index == 0 || index == count - 1 || (index + 1) % 5 == 0;
  }

  private double getMaxAmount() {
    double max = 0;
    for (TrendItem item : items) {
      max = Math.max(max, item.getAmount());
    }
    return max;
  }

  private float dp(int value) {
    return value * getResources().getDisplayMetrics().density;
  }

  private float sp(int value) {
    return value * getResources().getDisplayMetrics().scaledDensity;
  }
}
