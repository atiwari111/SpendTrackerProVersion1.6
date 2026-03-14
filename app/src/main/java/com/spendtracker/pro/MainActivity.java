package com.spendtracker.pro;

import android.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int PERM_CODE = 100;
    private TextView tvGreeting, tvDate, tvTodayAmt, tvMonthAmt, tvBudgetLeft, tvTopCat, tvTransCount, tvHealthScore, tvImportStatus, tvPrediction;
    private RecyclerView rvRecent;
    private TransactionAdapter adapter;
    private ProgressBar progressBar;
    private Button btnScan;
    private AppDatabase db;
    private ExecutorService exec = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getInstance(this);
        CategoryEngine.init(this);
        MerchantLogoProvider.load(this);
        initViews();
        setupNav();
        setGreeting();
        observeData();
        if (!hasSmsPermission()) requestSmsPermission();
    }

    private void initViews() {
        tvGreeting     = findViewById(R.id.tvGreeting);
        tvDate         = findViewById(R.id.tvDate);
        tvTodayAmt     = findViewById(R.id.tvTodayAmt);
        tvMonthAmt     = findViewById(R.id.tvMonthAmt);
        tvBudgetLeft   = findViewById(R.id.tvBudgetLeft);
        tvTopCat       = findViewById(R.id.tvTopCat);
        tvTransCount   = findViewById(R.id.tvTransCount);
        tvHealthScore  = findViewById(R.id.tvHealthScore);
        tvImportStatus = findViewById(R.id.tvImportStatus);
        tvPrediction   = findViewById(R.id.tvPrediction); // nullable — only present if added to layout
        progressBar    = findViewById(R.id.progressBar);
        btnScan        = findViewById(R.id.btnScan);
        rvRecent       = findViewById(R.id.rvRecent);

        adapter = new TransactionAdapter(true);
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        rvRecent.setAdapter(adapter);
        rvRecent.setNestedScrollingEnabled(false);

        btnScan.setOnClickListener(v -> {
            if (hasSmsPermission()) {
                startImport();
            } else {
                requestSmsPermission();
            }
        });

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));
        findViewById(R.id.cardInsights).setOnClickListener(v ->
                startActivity(new Intent(this, AnalyticsActivity.class)));
        findViewById(R.id.cardBudget).setOnClickListener(v ->
                startActivity(new Intent(this, BudgetActivity.class)));
        findViewById(R.id.cardNetWorth).setOnClickListener(v ->
                startActivity(new Intent(this, NetWorthActivity.class)));
    }

    private void setupNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_txn)       { startActivity(new Intent(this, TransactionsActivity.class)); return true; }
            if (id == R.id.nav_analytics) { startActivity(new Intent(this, AnalyticsActivity.class));    return true; }
            if (id == R.id.nav_budget)    { startActivity(new Intent(this, BudgetActivity.class));        return true; }
            if (id == R.id.nav_settings)  { startActivity(new Intent(this, SettingsActivity.class));     return true; }
            return true;
        });
    }

    private void setGreeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = h < 12 ? "Good Morning ☀️" : h < 17 ? "Good Afternoon 🌤️" : "Good Evening 🌙";
        tvGreeting.setText(g);
        tvDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date()));
    }

    private void observeData() {
        db.transactionDao().getAll().observe(this, list -> {
            if (list == null) return;
            List<Transaction> recent = list.size() > 6 ? list.subList(0, 6) : list;
            adapter.setTransactions(recent);
            tvTransCount.setText(list.size() + " total");
            exec.execute(() -> updateStats(list));
        });
    }

    private void updateStats(List<Transaction> list) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        long todayStart = getDayStart(now);
        long monthStart = getMonthStart(now);

        double todayTotal = 0, monthTotal = 0;
        Map<String, Double> catMap = new HashMap<>();
        for (Transaction t : list) {
            if (t.isSelfTransfer) continue;
            if (t.timestamp >= todayStart) todayTotal += t.amount;
            if (t.timestamp >= monthStart) {
                monthTotal += t.amount;
                String cat = t.category != null ? t.category : "Others";
                catMap.merge(cat, t.amount, Double::sum);
            }
        }
        String topCat = catMap.isEmpty() ? "N/A"
                : Collections.max(catMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        int month = cal.get(Calendar.MONTH) + 1, year = cal.get(Calendar.YEAR);
        List<Budget> budgets = db.budgetDao().getByMonthYearSync(month, year);
        double budgetLimit = 0, budgetUsed = 0;
        for (Budget b : budgets) {
            budgetLimit += b.limitAmount;
            double liveUsed = 0;
            for (Transaction t : list) {
                if (!t.isSelfTransfer && t.category != null
                        && t.category.equals(b.category) && t.timestamp >= monthStart) {
                    liveUsed += t.amount;
                }
            }
            budgetUsed += liveUsed;
        }
        double remaining = budgetLimit - budgetUsed;

        int score = InsightEngine.calcHealthScore(list, budgets);

        // Spend prediction — null-safe in case SpendPredictor is unavailable
        SpendPredictor.Prediction prediction = null;
        try {
            prediction = SpendPredictor.predict(list);
        } catch (Exception ignored) {}

        final double ft = todayTotal, fm = monthTotal, fr = remaining, fb = budgetLimit;
        final String fc = topCat;
        final int fs = score;
        final boolean hasBudget = budgetLimit > 0;
        final SpendPredictor.Prediction pred = prediction;

        runOnUiThread(() -> {
            tvTodayAmt.setText(String.format("₹%.0f", ft));
            tvMonthAmt.setText(String.format("₹%.0f", fm));
            tvBudgetLeft.setText(hasBudget
                    ? String.format("₹%.0f left of ₹%.0f", fr, fb)
                    : "➕ Tap to set budgets");
            tvBudgetLeft.setTextColor(hasBudget ? (fr < 0 ? 0xFFEF4444 : 0xFF10B981) : 0xFFA78BFA);
            tvTopCat.setText(fc);

            String scoreLabel = InsightEngine.getHealthScoreLabel(fs);
            tvHealthScore.setText(fs + "/100  " + scoreLabel);
            int color = fs >= 70 ? 0xFF10B981 : fs >= 40 ? 0xFFF59E0B : 0xFFEF4444;
            tvHealthScore.setTextColor(color);

            if (tvPrediction != null && pred != null) {
                tvPrediction.setText(String.format("🔮 Projected: ₹%.0f  %s",
                        pred.projectedTotal, pred.label));
                tvPrediction.setTextColor(pred.isOnTrack ? 0xFF10B981 : 0xFFF59E0B);
            }
        });
    }

    private void startImport() {
        progressBar.setVisibility(View.VISIBLE);
        tvImportStatus.setVisibility(View.VISIBLE);
        btnScan.setEnabled(false);
        tvImportStatus.setText("🔍 Scanning SMS messages (works offline)...");

        SmsImporter.importAll(this, new SmsImporter.Callback() {
            public void onProgress(int d, int t) {
                runOnUiThread(() -> tvImportStatus.setText("Scanning... " + d + " found so far"));
            }
            public void onComplete(int count) {
                int m = Calendar.getInstance().get(Calendar.MONTH) + 1;
                int y = Calendar.getInstance().get(Calendar.YEAR);
                Calendar recalCal = Calendar.getInstance();
                recalCal.set(Calendar.MONTH, m - 1);
                recalCal.set(Calendar.YEAR, y);
                recalCal.set(Calendar.DAY_OF_MONTH, 1);
                recalCal.set(Calendar.HOUR_OF_DAY, 0);
                recalCal.set(Calendar.MINUTE, 0);
                recalCal.set(Calendar.SECOND, 0);
                recalCal.set(Calendar.MILLISECOND, 0);
                long rStart = recalCal.getTimeInMillis();
                recalCal.add(Calendar.MONTH, 1);
                long rEnd = recalCal.getTimeInMillis();
                db.budgetDao().recalcAllUsed(m, y, rStart, rEnd);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    if (count > 0) {
                        tvImportStatus.setText("✅ Imported " + count + " new transactions!");
                        Executors.newSingleThreadExecutor().execute(() -> {
                            List<Transaction> all = db.transactionDao().getAllSync();
                            List<Budget> budgets = db.budgetDao().getByMonthYearSync(m, y);
                            List<String> ins = InsightEngine.generateInsights(all);
                            if (!ins.isEmpty()) NotificationHelper.sendInsight(MainActivity.this, ins.get(0));
                        });
                    } else {
                        tvImportStatus.setText("ℹ️ No new transactions found (already up to date)");
                    }
                    loadDashboard();
                });
            }
            public void onError(String msg) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnScan.setEnabled(true);
                    tvImportStatus.setText("❌ " + (msg != null ? msg : "Unknown error"));
                });
            }
        });
    }

    // ── Permission handling ───────────────────────────────────────

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.READ_SMS);
        perms.add(Manifest.permission.RECEIVE_SMS);
        // POST_NOTIFICATIONS only required on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        ActivityCompat.requestPermissions(this, perms.toArray(new String[0]), PERM_CODE);
    }

    /**
     * FIX: This was missing — without it, granting READ_SMS never triggered
     * the scan. Now scan auto-starts as soon as the user taps "Allow".
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.READ_SMS.equals(permissions[i])
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // Permission just granted — auto-start the scan
                    startImport();
                    return;
                }
            }
            // Permission denied
            if (tvImportStatus != null) {
                tvImportStatus.setVisibility(View.VISIBLE);
                tvImportStatus.setText("⚠️ SMS permission denied. Tap Scan again to retry.");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────

    private long getDayStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);      c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private long getMonthStart(long ts) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);       c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setGreeting();
        loadDashboard();
    }

    private void loadDashboard() {
        exec.execute(() -> {
            List<Transaction> all = db.transactionDao().getAllSync();
            if (all != null) updateStats(all);
        });
    }
}
