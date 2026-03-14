package com.spendtracker.pro;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.util.Log;
import java.util.*;

public class SmsImporter {
    private static final String TAG           = "SMS_IMPORT";
    private static final String PREFS_NAME    = "sms_importer_prefs";
    private static final String KEY_LAST_SCAN = "last_scan_ts";

    // ── FIX 7: Incremental scanning ──────────────────────────────
    // First scan: goes back 90 days.
    // Every subsequent scan: only fetches SMS newer than last scan
    // (with a 1-minute overlap to avoid edge-case gaps).
    private static long getSinceTimestamp(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastScan = prefs.getLong(KEY_LAST_SCAN, 0L);
        if (lastScan == 0L) {
            return System.currentTimeMillis() - 90L * 86400000L; // 90 days
        }
        return lastScan - 60_000L; // 1-min overlap
    }

    private static void saveLastScanTimestamp(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_SCAN, System.currentTimeMillis()).apply();
    }

    /** Call from Settings to force a full 90-day rescan next time */
    public static void resetLastScanTimestamp(Context ctx) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().remove(KEY_LAST_SCAN).apply();
    }

    public interface Callback {
        void onProgress(int done, int total);
        void onComplete(int count);
        void onError(String msg);
    }

    public static void importAll(Context ctx, Callback cb) {
        // FIX 8: Use shared AppExecutors instead of new Thread() — prevents thread proliferation
        AppExecutors.io().execute(() -> {
            try {
                // FIX 3: Ensure CategoryEngine is always initialized before use
                CategoryEngine.init(ctx);

                Uri uri = Uri.parse("content://sms/inbox");
                long since = getSinceTimestamp(ctx);
                Log.d(TAG, "Scanning SMS since: " + new Date(since));

                // FIX 6: try-with-resources — cursor always closed, even on exception
                try (Cursor c = ctx.getContentResolver().query(uri,
                        new String[]{"_id", "address", "body", "date"},
                        "date > ?", new String[]{String.valueOf(since)}, "date DESC")) {

                    if (c == null) {
                        if (cb != null) cb.onError("Cannot access SMS inbox.");
                        return;
                    }

                    int total    = c.getCount();
                    int imported = 0;
                    AppDatabase db = AppDatabase.getInstance(ctx);
                    List<Transaction> batch = new ArrayList<>();
                    Log.d(TAG, "Total SMS to scan: " + total);

                    while (c.moveToNext()) {
                        try {
                            // FIX 1: getColumnIndexOrThrow instead of getColumnIndex
                            // getColumnIndex returns -1 silently → crash on getString(-1)
                            // getColumnIndexOrThrow throws a clear IllegalArgumentException
                            String body = c.getString(c.getColumnIndexOrThrow("body"));
                            String addr = c.getString(c.getColumnIndexOrThrow("address"));
                            long   date = c.getLong(c.getColumnIndexOrThrow("date"));

                            if (body == null || body.trim().isEmpty()) continue;
                            if (addr == null) addr = "";

                            Log.d(TAG, "Parsing: " + addr + " | "
                                    + body.substring(0, Math.min(body.length(), 60)));

                            // FIX 2: Always null-check parser result before accessing any field
                            BankAwareSmsParser.ParseResult p = BankAwareSmsParser.parse(body, addr);
                            if (p == null) {
                                Log.d(TAG, "Not a bank transaction — skipping");
                                continue;
                            }

                            // FIX 5: rawSms has @Index in Transaction entity — this is fast
                            if (db.transactionDao().findBySms(body) != null) {
                                Log.d(TAG, "Duplicate — skipping");
                                continue;
                            }

                            // FIX 4: Null-safe assignment + UPI merchant resolution
                            // e.g. "Block Upi.download Pnb" → "Gullak"
                            Transaction tx   = new Transaction();
                            String resolvedMerchant = CategoryEngine.resolveUpiMerchant(p.merchant);
                            tx.merchant      = resolvedMerchant != null ? resolvedMerchant
                                             : (p.merchant != null ? p.merchant : "Unknown");
                            tx.amount        = p.amount;
                            tx.paymentMethod = p.paymentMethod != null ? p.paymentMethod : "BANK";
                            tx.paymentDetail = p.paymentDetail != null ? p.paymentDetail : "";
                            tx.category      = p.category      != null ? p.category      : "Others";
                            tx.timestamp     = date;
                            tx.rawSms        = body;
                            tx.smsAddress    = addr;
                            tx.isManual      = false;

                            CategoryEngine.CategoryInfo info = CategoryEngine.getInfo(tx.category);
                            tx.categoryIcon = (info != null) ? info.icon : "💼";

                            batch.add(tx);
                            imported++;
                            Log.d(TAG, "✓ " + tx.merchant + " ₹" + tx.amount + " [" + tx.category + "]");

                        } catch (IllegalArgumentException colEx) {
                            Log.e(TAG, "Column missing: " + colEx.getMessage());
                        } catch (Exception rowEx) {
                            Log.e(TAG, "Row error: " + rowEx.getMessage());
                        }

                        if (cb != null && c.getPosition() % 50 == 0) {
                            final int imp = imported;
                            cb.onProgress(imp, total);
                        }
                    }
                    // Cursor auto-closed by try-with-resources

                    // FIX 3 (DAO side): insertAll uses OnConflictStrategy.IGNORE
                    if (!batch.isEmpty()) {
                        db.transactionDao().insertAll(batch);
                        Log.d(TAG, "Bulk inserted: " + batch.size());
                    }

                    // FIX 7: Save timestamp so next scan only fetches new SMS
                    saveLastScanTimestamp(ctx);

                    if (cb != null) cb.onComplete(imported);
                }

            } catch (SecurityException se) {
                Log.e(TAG, "Permission denied: " + se.getMessage());
                if (cb != null) cb.onError("SMS permission denied. Please grant READ_SMS permission.");
            } catch (Exception e) {
                Log.e(TAG, "Import failed: " + e.getMessage());
                if (cb != null) {
                    String msg = e.getMessage();
                    cb.onError(msg != null ? msg : "Unexpected error during SMS scan.");
                }
            }
        });
    }
}
