package com.example.accountbook.fragment;

import android.app.DatePickerDialog;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.accountbook.R;
import com.example.accountbook.db.AccountDao;
import com.example.accountbook.db.BillRecordDao;
import com.example.accountbook.db.CategoryDao;
import com.example.accountbook.model.Account;
import com.example.accountbook.model.BillRecord;
import com.example.accountbook.model.Category;
import com.example.accountbook.util.VoucherFileUtils;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddBillFragment extends Fragment {

  private RadioGroup rgBillType;
  private EditText etAmount;
  private TextView tvCategory;
  private TextView tvAccount;
  private TextView tvDate;
  private TextView tvVoucherStatus;
  private EditText etRemark;
  private String selectedImagePath;
  private ActivityResultLauncher<Intent> voucherPickerLauncher;

  private CategoryDao categoryDao;
  private AccountDao accountDao;
  private BillRecordDao billRecordDao;

  private String currentType = BillRecord.TYPE_EXPENSE;
  private String selectedDate;
  private Category selectedCategory;
  private Account selectedAccount;
  private List<Category> categories = new ArrayList<>();
  private List<Account> accounts = new ArrayList<>();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    voucherPickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri uri = result.getData().getData();
            if (uri != null) {
              copySelectedVoucher(uri);
            }
          }
        });
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_add_bill, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initDaos();
    initViews(view);
    setupEvents();
    selectedDate = formatDate(Calendar.getInstance());
    tvDate.setText(selectedDate);
    loadCategories();
    loadAccounts();
  }

  private void initDaos() {
    categoryDao = new CategoryDao(requireContext());
    accountDao = new AccountDao(requireContext());
    billRecordDao = new BillRecordDao(requireContext());
  }

  private void initViews(View view) {
    rgBillType = view.findViewById(R.id.rgBillType);
    etAmount = view.findViewById(R.id.etAmount);
    tvCategory = view.findViewById(R.id.tvCategory);
    tvAccount = view.findViewById(R.id.tvAccount);
    tvDate = view.findViewById(R.id.tvDate);
    tvVoucherStatus = view.findViewById(R.id.tvVoucherStatus);
    etRemark = view.findViewById(R.id.etRemark);
    View btnSelectVoucher = view.findViewById(R.id.btnSelectVoucher);
    btnSelectVoucher.setOnClickListener(v -> openVoucherPicker());
    View btnRemoveVoucher = view.findViewById(R.id.btnRemoveVoucher);
    btnRemoveVoucher.setOnClickListener(v -> removeSelectedVoucher());
    View btnSaveBill = view.findViewById(R.id.btnSaveBill);
    btnSaveBill.setOnClickListener(v -> saveBillRecord());
  }

  private void setupEvents() {
    rgBillType.setOnCheckedChangeListener((group, checkedId) -> {
      currentType = checkedId == R.id.rbIncome ? BillRecord.TYPE_INCOME : BillRecord.TYPE_EXPENSE;
      loadCategories();
    });
    tvCategory.setOnClickListener(v -> showCategoryDialog());
    tvAccount.setOnClickListener(v -> showAccountDialog());
    tvDate.setOnClickListener(v -> showDatePicker());
  }

  private void loadCategories() {
    categories = categoryDao.getCategoriesByType(currentType);
    selectedCategory = categories.isEmpty() ? null : categories.get(0);
    tvCategory.setText(selectedCategory == null
        ? getString(R.string.select_category)
        : selectedCategory.getName());
  }

  private void loadAccounts() {
    accounts = accountDao.getActiveAccounts();
    selectedAccount = accounts.isEmpty() ? null : accounts.get(0);
    tvAccount.setText(selectedAccount == null
        ? getString(R.string.select_account)
        : selectedAccount.getName());
  }

  private void showCategoryDialog() {
    if (categories.isEmpty()) {
      Toast.makeText(requireContext(), R.string.no_categories, Toast.LENGTH_SHORT).show();
      return;
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.select_category)
        .setItems(toNames(categories), (dialog, which) -> {
          selectedCategory = categories.get(which);
          tvCategory.setText(selectedCategory.getName());
        })
        .show();
  }

  private void showAccountDialog() {
    if (accounts.isEmpty()) {
      Toast.makeText(requireContext(), R.string.no_accounts, Toast.LENGTH_SHORT).show();
      return;
    }
    new AlertDialog.Builder(requireContext())
        .setTitle(R.string.select_account)
        .setItems(toAccountNames(accounts), (dialog, which) -> {
          selectedAccount = accounts.get(which);
          tvAccount.setText(selectedAccount.getName());
        })
        .show();
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

  private void saveBillRecord() {
    Double amount = parseAmount();
    if (amount == null || selectedCategory == null || selectedAccount == null) {
      return;
    }

    BillRecord record = new BillRecord();
    record.setType(currentType);
    record.setAmount(amount);
    record.setCategoryId(selectedCategory.getId());
    record.setAccountId(selectedAccount.getId());
    record.setRecordDate(selectedDate);
    record.setRemark(etRemark.getText().toString().trim());
    record.setCreateTime(System.currentTimeMillis());
    record.setImagePath(selectedImagePath);

    long recordId = billRecordDao.insertBillRecord(record);
    if (recordId > 0) {
      Toast.makeText(requireContext(), R.string.save_bill_success, Toast.LENGTH_SHORT).show();
      resetForm();
    } else {
      Toast.makeText(requireContext(), R.string.save_bill_failed, Toast.LENGTH_SHORT).show();
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
      if (selectedCategory == null) {
        Toast.makeText(requireContext(), R.string.error_category_required, Toast.LENGTH_SHORT).show();
        return null;
      }
      if (selectedAccount == null) {
        Toast.makeText(requireContext(), R.string.error_account_required, Toast.LENGTH_SHORT).show();
        return null;
      }
      return amount;
    } catch (NumberFormatException e) {
      Toast.makeText(requireContext(), R.string.error_amount_invalid, Toast.LENGTH_SHORT).show();
      return null;
    }
  }

  private void resetForm() {
    etAmount.setText("");
    etRemark.setText("");
    selectedImagePath = null;
    updateVoucherStatus();
    selectedDate = formatDate(Calendar.getInstance());
    tvDate.setText(selectedDate);
    loadCategories();
    loadAccounts();
  }

  private void openVoucherPicker() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.setType("image/*");
    voucherPickerLauncher.launch(intent);
  }

  private void copySelectedVoucher(Uri uri) {
    try {
      if (selectedImagePath != null) {
        VoucherFileUtils.deleteVoucherFile(requireContext(), selectedImagePath);
      }
      selectedImagePath = VoucherFileUtils.copyVoucherToPrivateDir(requireContext(), uri);
      updateVoucherStatus();
    } catch (IOException e) {
      Toast.makeText(requireContext(), "凭证复制失败", Toast.LENGTH_SHORT).show();
    }
  }

  private void removeSelectedVoucher() {
    VoucherFileUtils.deleteVoucherFile(requireContext(), selectedImagePath);
    selectedImagePath = null;
    updateVoucherStatus();
  }

  private void updateVoucherStatus() {
    tvVoucherStatus.setText(selectedImagePath == null ? "暂无凭证" : "已选择凭证");
  }

  private String formatDate(Calendar calendar) {
    return String.format(
        Locale.CHINA,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH));
  }

  private String[] toNames(List<Category> categories) {
    String[] names = new String[categories.size()];
    for (int i = 0; i < categories.size(); i++) {
      names[i] = categories.get(i).getName();
    }
    return names;
  }

  private String[] toAccountNames(List<Account> accounts) {
    String[] names = new String[accounts.size()];
    for (int i = 0; i < accounts.size(); i++) {
      names[i] = accounts.get(i).getName();
    }
    return names;
  }
}
