package com.spendtracker.pro;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.util.Log;
import java.util.*;

public class SmsImporter {
    private static final String TAG = "SMS_IMPORT";

    public interface Callback {
        void onProgress(int done, int total);
        void onComplete(int count);
        void onError(String msg);
    }

    public static void importAll(Context ctx, Callback cb) {
        new Thread(() -> {
            try {
                Uri uri = Uri.parse("content://sms/inbox");
                long since = System.currentTimeMillis() - 90L * 86400000L;

                // FIX 6: try-with-resources — cursor always closed even on exception
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

                    while (c.moveToNext()) {
                        try {
                            // FIX 1: getColumnIndexOrThrow instead of getColumnIndex
                            // getColumnIndex silently returns -1 on missing columns → crash
                            // getColumnIndexOrThrow throws a clear IllegalArgumentException instead
                            String body = c.getString(c.getColumnIndexOrThrow("body"));
                            String addr = c.getString(c.getColumnIndexOrThrow("address"));
                            long   date = c.getLong(c.getColumnIndexOrThrow("date"));

                            if (body == null || body.trim().isEmpty()) continue;
                            if (addr == null) addr = "";

                            // Debug log — filter in Logcat with tag SMS_IMPORT
                            Log.d(TAG, "Parsing SMS from: " + addr
                                    + " | " + body.substring(0, Math.min(body.length(), 60)));

                            // FIX 2: Always null-check parser result before accessing any field
                            BankAwareSmsParser.ParseResult p = BankAwareSmsParser.parse(body, addr);
                            if (p == null) {
                                Log.d(TAG, "Parser returned null — not a bank transaction, skipping");
                                continue;
                            }

                            if (db.transactionDao().findBySms(body) != null) {
                                Log.d(TAG, "Duplicate SMS — skipping");
                                continue;
                            }

                            // FIX 4: Null-safe assignment for every field from parser
                            Transaction tx   = new Transaction();
                            tx.merchant      = p.merchant      != null ? p.merchant      : "Unknown";
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
                            // getColumnIndexOrThrow throws this if column name doesn't exist
                            Log.e(TAG, "Column missing in SMS cursor: " + colEx.getMessage());
                        } catch (Exception rowEx) {
                            // Any other per-row error — log and skip, don't abort entire scan
                            Log.e(TAG, "Row error: " + rowEx.getMessage());
                        }

                        if (cb != null && c.getPosition() % 50 == 0) {
                            final int imp = imported;
                            cb.onProgress(imp, total);
                        }
                    }
                    // Cursor auto-closed here by try-with-resources

                    // FIX 3: DAO uses OnConflictStrategy.IGNORE so duplicate inserts don't crash
                    if (!batch.isEmpty()) {
                        db.transactionDao().insertAll(batch);
                        Log.d(TAG, "Bulk inserted " + batch.size() + " transactions");
                    }

                    if (cb != null) cb.onComplete(imported);
                }

            } catch (SecurityException se) {
                // FIX 5: Catch permission denial cleanly
                Log.e(TAG, "SMS permission denied: " + se.getMessage());
                if (cb != null) cb.onError("SMS permission denied. Please grant READ_SMS permission.");
            } catch (Exception e) {
                Log.e(TAG, "Import failed: " + e.getMessage());
                if (cb != null) {
                    String msg = e.getMessage();
                    cb.onError(msg != null ? msg : "Unexpected error during SMS scan.");
                }
            }
        }).start();
    }
}
