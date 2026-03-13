package com.spendtracker.pro;

import android.*;
import android.content.*;
import android.content.pm.PackageManager;
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
        CategoryEngine.init(this);           // enable merchant learning across the app
        MerchantLogoProvider.load(this);     // load merchant logo map from assets
        initViews();
        setupNav();
        setGreeting();
        observeData();
        if (!hasSmsPermission()) requestSmsPermission();
    }

    private void initViews() {
        tvGreeting    = findViewById(R.id.tvGreeting);
        tvDate        = findViewById(R.id.tvDate);
        tvTodayAmt    = findViewById(R.id.tvTodayAmt);
        tvMonthAmt    = findViewById(R.id.tvMonthAmt);
        tvBudgetLeft  = findViewById(R.id.tvBudgetLeft);
        tvTopCat      = findViewById(R.id.tvTopCat);
        tvTransCount  = findViewById(R.id.tvTransCount);
        tvHealthScore = findViewById(R.id.tvHealthScore);
        tvImportStatus= findViewById(R.id.tvImportStatus);
        tvPrediction  = findViewById(R.id.tvPrediction); // nullable — only present if added to layout
        progressBar   = findViewById(R.id.progressBar);
        btnScan       = findViewById(R.id.btnScan);
        rvRecent      = findViewById(R.id.rvRecent);

        adapter = new TransactionAdapter(true);
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        rvRecent.setAdapter(adapter);
        rvRecent.setNestedScrollingEnabled(false);

        btnScan.setOnClickListener(v -> { if (hasSmsPermission()) startImport(); else requestSmsPermission(); });
        findViewById(R.id.fabAdd).setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));
        findViewById(R.id.cardInsights).setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));
        findViewById(R.id.cardBudget).setOnClickListener(v -> startActivity(new Intent(this, BudgetActivity.class)));
        findViewById(R.id.cardNetWorth).setOnClickListener(v -> startActivity(new Intent(this, NetWorthActivity.class)));
    }

    private void setupNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_txn)      { startActivity(new Intent(this, TransactionsActivity.class)); return true; }
            if (id == R.id.nav_analytics){ startActivity(new Intent(this, AnalyticsActivity.class)); return true; }
            if (id == R.id.nav_budget)   { startActivity(new Intent(this, BudgetActivity.class)); return true; }
            if (id == R.id.nav_settings) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
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
            if (t.timestamp >= todayStart && !t.isSelfTransfer) todayTotal += t.amount;
            if (t.timestamp >= monthStart && !t.isSelfTransfer) {
                monthTotal += t.amount;
                catMap.merge(t.category, t.amount, Double::sum);
            }
        }
        String topCat = catMap.isEmpty() ? "N/A" : Collections.max(catMap.entrySet(), Map.Entry.comparingByValue()).getKey();

        // Budget remaining — compute usedAmount LIVE from transactions (never stale)
        int month = cal.get(Calendar.MONTH) + 1, year = cal.get(Calendar.YEAR);
        List<Budget> budgets = db.budgetDao().getByMonthYearSync(month, year);
        double budgetLimit = 0, budgetUsed = 0;
        for (Budget b : budgets) {
            budgetLimit += b.limitAmount;
            // Live compute from the transaction list already in memory
            double liveUsed = 0;
            for (Transaction t : list) {
                if (!t.isSelfTransfer && t.category != null && t.category.equals(b.category)
                        && t.timestamp >= monthStart) {
                    liveUsed += t.amount;
                }
            }
            budgetUsed += liveUsed;
        }
        double remaining = budgetLimit - budgetUsed;

        int score = InsightEngine.calcHealthScore(list, budgets);

        // Fix 1.7: Spend prediction
        SpendPredictor.Prediction prediction = SpendPredictor.predict(list);

        final double ft = todayTotal, fm = monthTotal, fr = remaining, fb = budgetLimit;
        final String fc = topCat;
        final int fs = score;
        final boolean hasBudget = budgetLimit > 0;
        final SpendPredictor.Prediction pred = prediction;
        runOnUiThread(() -> {
            tvTodayAmt.setText(String.format("₹%.0f", ft));
            tvMonthAmt.setText(String.format("₹%.0f", fm));
            tvBudgetLeft.setText(hasBudget ? String.format("₹%.0f left of ₹%.0f", fr, fb) : "➕ Tap to set budgets");
            tvBudgetLeft.setTextColor(hasBudget ? (fr < 0 ? 0xFFEF4444 : 0xFF10B981) : 0xFFA78BFA);
            tvTopCat.setText(fc);
            // Health score with label
            String scoreLabel = InsightEngine.getHealthScoreLabel(fs);
            tvHealthScore.setText(fs + "/100  " + scoreLabel);
            int color = fs >= 70 ? 0xFF10B981 : fs >= 40 ? 0xFFF59E0B : 0xFFEF4444;
            tvHealthScore.setTextColor(color);
            // Spend prediction
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
                // Recalc budget usedAmounts from fresh transaction data
                int m = Calendar.getInstance().get(Calendar.MONTH) + 1;
                int y = Calendar.getInstance().get(Calendar.YEAR);
                // Recalc with proper timezone-safe timestamp range
                        Calendar recalCal = Calendar.getInstance();
                        recalCal.set(Calendar.MONTH, m - 1); recalCal.set(Calendar.YEAR, y);
                        recalCal.set(Calendar.DAY_OF_MONTH, 1);
                        recalCal.set(Calendar.HOUR_OF_DAY, 0); recalCal.set(Calendar.MINUTE, 0);
                        recalCal.set(Calendar.SECOND, 0); recalCal.set(Calendar.MILLISECOND, 0);
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
                    tvImportStatus.setText("❌ " + msg);
                });
            }
        });
    }

    private boolean hasSmsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.POST_NOTIFICATIONS}, PERM_CODE);
    }

    private long getDayStart(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
    private long getMonthStart(long ts) {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(ts);
        c.set(Calendar.DAY_OF_MONTH,1); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }

    @Override protected void onResume() {
        super.onResume();
        setGreeting();
        loadDashboard();
    }

    /** Trigger a fresh stats recalculation — called after scan, on resume, etc. */
    private void loadDashboard() {
        exec.execute(() -> {
            List<Transaction> all = db.transactionDao().getAllSync();
            if (all != null) updateStats(all);
        });
    }
}
