package com.example.accountbook.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.accountbook.model.CategorySummary;

import java.util.ArrayList;
import java.util.List;

public class CategoryPieChartView extends View {

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF oval = new RectF();
  private final int[] colors = {
      Color.rgb(47, 111, 237),
      Color.rgb(20, 125, 100),
      Color.rgb(194, 65, 50),
      Color.rgb(154, 94, 26),
      Color.rgb(88, 80, 180),
      Color.rgb(80, 96, 112)
  };
  private List<CategorySummary> summaries = new ArrayList<>();

  public CategoryPieChartView(Context context) {
    super(context);
  }

  public CategoryPieChartView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public void setSummaries(List<CategorySummary> summaries) {
    this.summaries = summaries == null ? new ArrayList<>() : summaries;
    invalidate();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = MeasureSpec.getSize(widthMeasureSpec);
    int desiredHeight = Math.round(180 * getResources().getDisplayMetrics().density);
    setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int width = getWidth();
    int height = getHeight();
    if (summaries.isEmpty()) {
      paint.setColor(Color.rgb(96, 112, 128));
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setTextSize(15 * getResources().getDisplayMetrics().scaledDensity);
      canvas.drawText("暂无分类支出数据", width / 2f, height / 2f, paint);
      return;
    }
    float size = Math.min(width, height) - dp(20);
    float left = (width - size) / 2f;
    float top = (height - size) / 2f;
    oval.set(left, top, left + size, top + size);
    float startAngle = -90f;
    for (int i = 0; i < summaries.size(); i++) {
      CategorySummary summary = summaries.get(i);
      paint.setColor(colors[i % colors.length]);
      float sweep = (float) (summary.getRatio() * 360f);
      canvas.drawArc(oval, startAngle, sweep, true, paint);
      startAngle += sweep;
    }
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
}
