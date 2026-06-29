package com.example.accountbook.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.db.CategoryDao;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.Category;

import java.util.List;

public class CategoryManageFragment extends Fragment {

  private static final String[] TYPE_LABELS = {"支出", "收入"};
  private static final String[] TYPE_VALUES = {BillRecord.TYPE_EXPENSE, BillRecord.TYPE_INCOME};

  private CategoryDao categoryDao;
  private BillRecordDao billRecordDao;
  private LinearLayout listContainer;

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
    Button btnAdd = new Button(requireContext());
    btnAdd.setText("新增分类");
    btnAdd.setOnClickListener(v -> showEditDialog(null));
    root.addView(btnAdd);
    listContainer = new LinearLayout(requireContext());
    listContainer.setOrientation(LinearLayout.VERTICAL);
    listContainer.setBackgroundResource(R.drawable.bg_panel);
    listContainer.setPadding(dp(18), dp(10), dp(18), dp(10));
    root.addView(listContainer);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    categoryDao = new CategoryDao(requireContext());
    billRecordDao = new BillRecordDao(requireContext());
    refreshList();
  }

  private void refreshList() {
    listContainer.removeAllViews();
    List<Category> categories = categoryDao.getAllCategoriesIncludingInactive();
    if (categories.isEmpty()) {
      listContainer.addView(createText("暂无分类", 15, R.color.text_secondary));
      return;
    }
    for (Category category : categories) {
      listContainer.addView(createItem(category));
    }
  }

  private View createItem(Category category) {
    LinearLayout item = new LinearLayout(requireContext());
    item.setOrientation(LinearLayout.VERTICAL);
    item.setPadding(0, dp(8), 0, dp(8));
    item.addView(createText(category.getName() + "  " + typeLabel(category.getType())
        + (category.isActive() ? "" : "  已停用"), 16, R.color.text_primary));
    LinearLayout actions = new LinearLayout(requireContext());
    Button edit = new Button(requireContext());
    edit.setText("编辑");
    edit.setOnClickListener(v -> showEditDialog(category));
    Button remove = new Button(requireContext());
    remove.setText(billRecordDao.hasRecordsByCategoryId(category.getId()) ? "停用" : "删除");
    remove.setOnClickListener(v -> confirmRemove(category));
    actions.addView(edit, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    actions.addView(remove, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    item.addView(actions);
    return item;
  }

  private void showEditDialog(@Nullable Category category) {
    boolean used = category != null && billRecordDao.hasRecordsByCategoryId(category.getId());
    LinearLayout content = new LinearLayout(requireContext());
    content.setOrientation(LinearLayout.VERTICAL);
    EditText nameInput = new EditText(requireContext());
    nameInput.setHint("分类名称");
    Spinner typeSpinner = new Spinner(requireContext());
    typeSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, TYPE_LABELS));
    content.addView(nameInput);
    content.addView(typeSpinner);
    if (category != null) {
      nameInput.setText(category.getName());
      typeSpinner.setSelection(typeIndex(category.getType()));
      typeSpinner.setEnabled(!used);
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(category == null ? "新增分类" : "编辑分类")
        .setView(content)
        .setPositiveButton("保存", (dialog, which) -> saveCategory(category, nameInput, typeSpinner))
        .setNegativeButton("取消", null)
        .show();
  }

  private void saveCategory(@Nullable Category source, EditText nameInput, Spinner typeSpinner) {
    String name = nameInput.getText().toString().trim();
    if (TextUtils.isEmpty(name)) {
      Toast.makeText(requireContext(), "分类名称不能为空", Toast.LENGTH_SHORT).show();
      return;
    }
    String type = source != null && billRecordDao.hasRecordsByCategoryId(source.getId())
        ? source.getType()
        : TYPE_VALUES[typeSpinner.getSelectedItemPosition()];
    long excludeId = source == null ? -1 : source.getId();
    if (categoryDao.existsActiveCategoryName(type, name, excludeId)) {
      Toast.makeText(requireContext(), "分类名称已存在", Toast.LENGTH_SHORT).show();
      return;
    }
    Category category = source == null ? new Category() : source;
    category.setName(name);
    category.setType(type);
    category.setActive(source == null || source.isActive());
    if (source == null) {
      categoryDao.insertCategory(category);
    } else {
      categoryDao.updateCategory(category);
    }
    refreshList();
  }

  private void confirmRemove(Category category) {
    boolean used = billRecordDao.hasRecordsByCategoryId(category.getId());
    new AlertDialog.Builder(requireContext())
        .setTitle(used ? "停用分类" : "删除分类")
        .setMessage(used ? "该分类已有账单，只能停用。确认停用？" : "该分类没有账单，确认删除？")
        .setPositiveButton("确认", (dialog, which) -> {
          boolean success = used
              ? categoryDao.deactivateCategory(category.getId())
              : categoryDao.deleteCategoryIfUnused(category.getId());
          Toast.makeText(requireContext(), success ? "操作成功" : "操作失败", Toast.LENGTH_SHORT).show();
          refreshList();
        })
        .setNegativeButton("取消", null)
        .show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("分类管理", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回我的");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).backToMine());
    row.addView(button);
    return row;
  }

  private int typeIndex(String value) {
    for (int i = 0; i < TYPE_VALUES.length; i++) {
      if (TYPE_VALUES[i].equals(value)) {
        return i;
      }
    }
    return 0;
  }

  private String typeLabel(String value) {
    return TYPE_LABELS[typeIndex(value)];
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
