package com.spendtracker.pro;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.*;
import android.view.MenuItem;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * AddExpenseActivity v1.5
 * - Single (non-duplicate) TextWatcher for auto-category
 * - Merchant learning: if user overrides auto-category, stores the correction
 * - Learning triggered only on manual save (not on edit load)
 */
public class AddExpenseActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "txn_id";

    private TextInputEditText etAmount, etMerchant, etNotes;
    private Spinner spCategory, spPayment;
    private TextView tvDate;
    private Switch swSelfTransfer;
    private long selectedDate = System.currentTimeMillis();
    private AppDatabase db;
    private Transaction editingTransaction = null;

    // Track what auto-category suggested vs what user actually picked
    private String autoSuggestedCategory = null;
    private boolean userOverrodeCategory  = false;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_add_expense);
        db = AppDatabase.getInstance(this);

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etAmount       = findViewById(R.id.etAmount);
        etMerchant     = findViewById(R.id.etMerchant);
        etNotes        = findViewById(R.id.etNotes);
        tvDate         = findViewById(R.id.tvDate);
        spCategory     = findViewById(R.id.spCategory);
        spPayment      = findViewById(R.id.spPayment);
        swSelfTransfer = findViewById(R.id.swSelfTransfer);

        // ── Spinners ─────────────────────────────────────────────
        String[] cats = CategoryEngine.getCategoryNames();
        spCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, cats));

        String[] payments = {"UPI", "Credit Card", "Debit Card", "Cash", "Bank Transfer"};
        spPayment.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, payments));

        // ── Detect manual category override by user ───────────────
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                String selected = (String) parent.getItemAtPosition(pos);
                if (autoSuggestedCategory != null && !selected.equals(autoSuggestedCategory)) {
                    userOverrodeCategory = true;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ── Single auto-category TextWatcher ─────────────────────
        etMerchant.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (editingTransaction != null) return; // don't override when editing
                String typed = s.toString().trim();
                if (typed.length() < 3) return;

                String suggested = CategoryEngine.autoCategory(typed);
                if (!suggested.equals("💼 Others")) {
                    autoSuggestedCategory = suggested;
                    userOverrodeCategory  = false; // reset flag when merchant changes
                    String[] catList = CategoryEngine.getCategoryNames();
                    for (int i = 0; i < catList.length; i++) {
                        if (catList[i].equals(suggested)) {
                            spCategory.setSelection(i);
                            break;
                        }
                    }
                }
            }
        });

        // ── Date picker ──────────────────────────────────────────
        updateDateLabel();
        tvDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedDate);
            new DatePickerDialog(this, (view, y, m, d) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(y, m, d);
                selectedDate = sel.getTimeInMillis();
                updateDateLabel();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // ── Load for edit ────────────────────────────────────────
        int txnId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);
        if (txnId != -1) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Expense");
            loadForEditing(txnId);
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("Add Expense");
        }

        findViewById(R.id.btnSave).setOnClickListener(v -> saveExpense());
    }

    // ─────────────────────────────────────────────────────────────
    // LOAD FOR EDITING
    // ─────────────────────────────────────────────────────────────
    private void loadForEditing(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Transaction> all = db.transactionDao().getAllSync();
            for (Transaction t : all) {
                if (t.id == id) { editingTransaction = t; break; }
            }
            if (editingTransaction == null) return;
            final Transaction t = editingTransaction;
            runOnUiThread(() -> {
                etAmount.setText(String.format("%.2f", t.amount));
                etMerchant.setText(t.merchant);
                etNotes.setText(t.notes != null ? t.notes : "");
                selectedDate = t.timestamp;
                updateDateLabel();
                swSelfTransfer.setChecked(t.isSelfTransfer);

                String[] catList = CategoryEngine.getCategoryNames();
                for (int i = 0; i < catList.length; i++) {
                    if (catList[i].equals(t.category)) { spCategory.setSelection(i); break; }
                }
                String[] payments = {"UPI", "Credit Card", "Debit Card", "Cash", "Bank Transfer"};
                String pm = t.paymentDetail != null ? t.paymentDetail : "";
                for (int i = 0; i < payments.length; i++) {
                    if (pm.toLowerCase().contains(payments[i].toLowerCase())) {
                        spPayment.setSelection(i);
                        break;
                    }
                }
                ((Button) findViewById(R.id.btnSave)).setText("Update Expense");
            });
        });
    }

    // ─────────────────────────────────────────────────────────────
    // SAVE / UPDATE
    // ─────────────────────────────────────────────────────────────
    private void saveExpense() {
        String amtStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        if (amtStr.isEmpty()) { etAmount.setError("Enter amount"); return; }
        double amount;
        try { amount = Double.parseDouble(amtStr); }
        catch (Exception e) { etAmount.setError("Invalid amount"); return; }

        String merchant = etMerchant.getText() != null ? etMerchant.getText().toString().trim() : "";
        if (merchant.isEmpty()) merchant = "Manual Entry";

        String category   = (String) spCategory.getSelectedItem();
        String paymentRaw = (String) spPayment.getSelectedItem();
        String paymentMethod = paymentRaw.equals("Credit Card") ? "CREDIT_CARD"
                             : paymentRaw.equals("Debit Card")  ? "DEBIT_CARD"
                             : paymentRaw.replace(" ", "_").toUpperCase();
        String notes  = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";
        boolean isSelf = swSelfTransfer.isChecked();

        // ── Merchant learning: save correction if user overrode auto-category ──
        if (userOverrodeCategory && !merchant.equals("Manual Entry")) {
            CategoryEngine.learnMerchant(merchant, category);
        }

        if (editingTransaction != null) {
            editingTransaction.merchant      = merchant;
            editingTransaction.amount        = amount;
            editingTransaction.category      = category;
            editingTransaction.categoryIcon  = CategoryEngine.getInfo(category).icon;
            editingTransaction.paymentMethod = paymentMethod;
            editingTransaction.paymentDetail = paymentRaw;
            editingTransaction.timestamp     = selectedDate;
            editingTransaction.notes         = notes;
            editingTransaction.isSelfTransfer = isSelf;
            Executors.newSingleThreadExecutor().execute(() -> {
                db.transactionDao().update(editingTransaction);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Expense updated!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            Transaction tx = new Transaction();
            tx.merchant      = merchant;
            tx.amount        = amount;
            tx.category      = category;
            tx.categoryIcon  = CategoryEngine.getInfo(category).icon;
            tx.paymentMethod = paymentMethod;
            tx.paymentDetail = paymentRaw;
            tx.timestamp     = selectedDate;
            tx.notes         = notes;
            tx.isManual      = true;
            tx.isSelfTransfer = isSelf;
            final String finalMerchant = merchant;
            Executors.newSingleThreadExecutor().execute(() -> {
                db.transactionDao().insert(tx);
                if (!isSelf) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(selectedDate);
                    db.budgetDao().addUsed(category, amount,
                            cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
                    Budget budget = db.budgetDao().getByCategoryMonthYear(
                            category, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
                    if (budget != null && budget.getProgress() >= 0.9f)
                        NotificationHelper.sendBudgetAlert(this, category,
                                budget.usedAmount, budget.limitAmount, budget.id);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Expense saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }

    private void updateDateLabel() {
        tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(selectedDate)));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
