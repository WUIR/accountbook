package com.example.accountbook.fragment;

import android.os.Bundle;
import android.content.res.TypedArray;
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
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.util.UserSessionUtils;

public class ToolboxFragment extends Fragment {

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
    root.addView(createToolList());
    return scrollView;
  }

  private View createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setOrientation(LinearLayout.HORIZONTAL);

    TextView title = createText(getString(R.string.general_settings), 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

    TextView back = createText(getString(R.string.back_to_previous), 14, R.color.text_secondary);
    back.setGravity(Gravity.CENTER);
    back.setMinHeight(dp(40));
    back.setPadding(dp(12), 0, 0, 0);
    back.setOnClickListener(v -> ((MainActivity) requireActivity()).backToMine());
    row.addView(back);
    return row;
  }

  private View createToolList() {
    LinearLayout list = new LinearLayout(requireContext());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    params.setMargins(0, dp(18), 0, 0);
    list.setLayoutParams(params);
    list.setBackgroundResource(R.drawable.bg_home_light_card);
    list.setOrientation(LinearLayout.VERTICAL);
    list.addView(createToolItem(R.string.recycle_bin,
        v -> ((MainActivity) requireActivity()).openRecycleBin()));
    list.addView(createDivider());
    list.addView(createToolItem(R.string.export_bill,
        v -> ((MainActivity) requireActivity()).openExport()));
    list.addView(createDivider());
    list.addView(createToolItem(R.string.account_manage,
        v -> ((MainActivity) requireActivity()).openAccountManage()));
    list.addView(createDivider());
    list.addView(createToolItem(R.string.category_manage,
        v -> ((MainActivity) requireActivity()).openCategoryManage()));
    if (UserSessionUtils.isLoggedIn(requireContext())) {
      list.addView(createDivider());
      list.addView(createToolItem(R.string.logout, v -> logout()));
    }
    return list;
  }

  private void logout() {
    UserSessionUtils.clearLoginSession(requireContext());
    Toast.makeText(requireContext(), R.string.logout_success, Toast.LENGTH_SHORT).show();
    ((MainActivity) requireActivity()).backToMine();
  }

  private View createToolItem(int titleRes, View.OnClickListener listener) {
    LinearLayout item = new LinearLayout(requireContext());
    item.setLayoutParams(new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(56)));
    item.setClickable(true);
    item.setFocusable(true);
    TypedArray typedArray = requireContext().getTheme()
        .obtainStyledAttributes(new int[] { android.R.attr.selectableItemBackground });
    item.setForeground(typedArray.getDrawable(0));
    typedArray.recycle();
    item.setGravity(Gravity.CENTER_VERTICAL);
    item.setOrientation(LinearLayout.HORIZONTAL);
    item.setPadding(dp(16), 0, dp(14), 0);
    item.setOnClickListener(listener);

    TextView title = createText(getString(titleRes), 16, R.color.text_primary);
    item.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    TextView arrow = createText(">", 18, R.color.text_hint);
    arrow.setGravity(Gravity.CENTER);
    item.addView(arrow, new LinearLayout.LayoutParams(dp(20), ViewGroup.LayoutParams.WRAP_CONTENT));
    return item;
  }

  private View createDivider() {
    View divider = new View(requireContext());
    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        dp(1));
    params.setMargins(dp(16), 0, 0, 0);
    divider.setLayoutParams(params);
    divider.setBackgroundColor(getColor(R.color.border));
    return divider;
  }

  private TextView createText(String text, int sp, int colorRes) {
    TextView textView = new TextView(requireContext());
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
