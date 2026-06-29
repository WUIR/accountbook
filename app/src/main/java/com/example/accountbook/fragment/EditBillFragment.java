package com.example.accountbook.fragment;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.accountbook.MainActivity;
import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.db.CategoryDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.Category;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditBillFragment extends Fragment {

  private static final String ARG_BILL_ID = "bill_id";

  private BillRecordDao billRecordDao;
  private CategoryDao categoryDao;
  private AccountDao accountDao;
  private long billId;
  private BillRecord sourceRecord;
  private RadioGroup rgType;
  private RadioButton rbExpense;
  private RadioButton rbIncome;
  private EditText etAmount;
  private Spinner spCategory;
  private Spinner spAccount;
  private TextView tvDate;
  private EditText etRemark;
  private String selectedDate;
  private List<Category> categories = new ArrayList<>();
  private List<Account> accounts = new ArrayList<>();

  public static EditBillFragment newInstance(long billId) {
    EditBillFragment fragment = new EditBillFragment();
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
    LinearLayout root = new LinearLayout(requireContext());
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(20), dp(20), dp(20));
    scrollView.addView(root);
    root.addView(createTitleRow());

    rgType = new RadioGroup(requireContext());
    rgType.setOrientation(RadioGroup.HORIZONTAL);
    rbExpense = new RadioButton(requireContext());
    rbExpense.setId(View.generateViewId());
    rbExpense.setText("支出");
    rbIncome = new RadioButton(requireContext());
    rbIncome.setId(View.generateViewId());
    rbIncome.setText("收入");
    rgType.addView(rbExpense);
    rgType.addView(rbIncome);
    root.addView(rgType);

    etAmount = new EditText(requireContext());
    etAmount.setHint("请输入金额");
    etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    etAmount.setBackgroundResource(R.drawable.bg_input);
    etAmount.setPadding(dp(14), 0, dp(14), 0);
    root.addView(etAmount, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

    spCategory = new Spinner(requireContext());
    root.addView(createLabeledView("分类", spCategory));
    spAccount = new Spinner(requireContext());
    root.addView(createLabeledView("账户", spAccount));
    tvDate = createText("", 16, R.color.text_primary);
    tvDate.setBackgroundResource(R.drawable.bg_input);
    tvDate.setGravity(Gravity.CENTER_VERTICAL);
    tvDate.setPadding(dp(14), 0, dp(14), 0);
    tvDate.setOnClickListener(v -> showDatePicker());
    root.addView(createLabeledView("日期", tvDate));
    etRemark = new EditText(requireContext());
    etRemark.setHint("备注，可不填");
    etRemark.setBackgroundResource(R.drawable.bg_input);
    etRemark.setPadding(dp(14), 0, dp(14), 0);
    root.addView(etRemark, new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

    Button btnSave = new Button(requireContext());
    btnSave.setText("保存修改");
    btnSave.setOnClickListener(v -> saveEdit());
    root.addView(btnSave);
    return scrollView;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    billId = requireArguments().getLong(ARG_BILL_ID);
    billRecordDao = new BillRecordDao(requireContext());
    categoryDao = new CategoryDao(requireContext());
    accountDao = new AccountDao(requireContext());
    sourceRecord = billRecordDao.getBillRecordById(billId, false);
    if (sourceRecord == null) {
      Toast.makeText(requireContext(), "账单不存在", Toast.LENGTH_SHORT).show();
      ((MainActivity) requireActivity()).backToBillList();
      return;
    }
    rgType.setOnCheckedChangeListener((group, checkedId) -> loadCategories(getCurrentType(), -1));
    bindRecord();
  }

  private void bindRecord() {
    if (BillRecord.TYPE_INCOME.equals(sourceRecord.getType())) {
      rbIncome.setChecked(true);
    } else {
      rbExpense.setChecked(true);
    }
    etAmount.setText(String.format(Locale.CHINA, "%.2f", sourceRecord.getAmount()));
    selectedDate = sourceRecord.getRecordDate();
    tvDate.setText(selectedDate);
    etRemark.setText(sourceRecord.getRemark());
    loadCategories(sourceRecord.getType(), sourceRecord.getCategoryId());
    loadAccounts(sourceRecord.getAccountId());
  }

  private void loadCategories(String type, long selectedId) {
    categories = categoryDao.getCategoriesByType(type);
    List<String> names = new ArrayList<>();
    int selectedIndex = 0;
    for (int i = 0; i < categories.size(); i++) {
      Category category = categories.get(i);
      names.add(category.getName());
      if (category.getId() == selectedId) {
        selectedIndex = i;
      }
    }
    spCategory.setAdapter(new ArrayAdapter<>(
        requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
    if (!categories.isEmpty()) {
      spCategory.setSelection(selectedIndex);
    }
  }

  private void loadAccounts(long selectedId) {
    accounts = accountDao.getAllAccounts();
    List<String> names = new ArrayList<>();
    int selectedIndex = 0;
    for (int i = 0; i < accounts.size(); i++) {
      Account account = accounts.get(i);
      names.add(account.getName());
      if (account.getId() == selectedId) {
        selectedIndex = i;
      }
    }
    spAccount.setAdapter(new ArrayAdapter<>(
        requireContext(), android.R.layout.simple_spinner_dropdown_item, names));
    if (!accounts.isEmpty()) {
      spAccount.setSelection(selectedIndex);
    }
  }

  private void saveEdit() {
    Double amount = parseAmount();
    if (amount == null || categories.isEmpty() || accounts.isEmpty()) {
      return;
    }
    BillRecord record = new BillRecord();
    record.setId(sourceRecord.getId());
    record.setType(getCurrentType());
    record.setAmount(amount);
    record.setCategoryId(categories.get(spCategory.getSelectedItemPosition()).getId());
    record.setAccountId(accounts.get(spAccount.getSelectedItemPosition()).getId());
    record.setRecordDate(selectedDate);
    record.setRemark(etRemark.getText().toString().trim());
    record.setCreateTime(sourceRecord.getCreateTime());
    record.setDeletedAt(0);
    boolean success = billRecordDao.updateBillRecord(record);
    Toast.makeText(requireContext(), success ? "修改成功" : "修改失败", Toast.LENGTH_SHORT).show();
    if (success) {
      ((MainActivity) requireActivity()).openBillDetail(billId);
    }
  }

  @Nullable
  private Double parseAmount() {
    String amountText = etAmount.getText().toString().trim();
    if (TextUtils.isEmpty(amountText)) {
      Toast.makeText(requireContext(), R.string.error_amount_empty, Toast.LENGTH_SHORT).show();
      return null;
    }
    try {
      double amount = Double.parseDouble(amountText);
      if (amount <= 0) {
        Toast.makeText(requireContext(), R.string.error_amount_positive, Toast.LENGTH_SHORT).show();
        return null;
      }
      return amount;
    } catch (NumberFormatException e) {
      Toast.makeText(requireContext(), R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
      return null;
    }
  }

  private String getCurrentType() {
    return rbIncome.isChecked() ? BillRecord.TYPE_INCOME : BillRecord.TYPE_EXPENSE;
  }

  private void showDatePicker() {
    Calendar calendar = Calendar.getInstance();
    String[] dateParts = selectedDate.split("-");
    if (dateParts.length == 3) {
      calendar.set(
          Integer.parseInt(dateParts[0]),
          Integer.parseInt(dateParts[1]) - 1,
          Integer.parseInt(dateParts[2]));
    }
    new DatePickerDialog(
        requireContext(),
        (view, year, month, dayOfMonth) -> {
          selectedDate = String.format(Locale.CHINA, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
          tvDate.setText(selectedDate);
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH))
        .show();
  }

  private LinearLayout createTitleRow() {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = createText("编辑账单", 24, R.color.text_primary);
    title.setTypeface(null, android.graphics.Typeface.BOLD);
    row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
    Button button = new Button(requireContext());
    button.setText("返回详情");
    button.setOnClickListener(v -> ((MainActivity) requireActivity()).openBillDetail(billId));
    row.addView(button);
    return row;
  }

  private LinearLayout createLabeledView(String label, View content) {
    LinearLayout row = new LinearLayout(requireContext());
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, dp(10), 0, 0);
    TextView labelView = createText(label, 15, R.color.text_primary);
    row.addView(labelView, new LinearLayout.LayoutParams(dp(58), ViewGroup.LayoutParams.WRAP_CONTENT));
    row.addView(content, new LinearLayout.LayoutParams(0, dp(52), 1));
    return row;
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
