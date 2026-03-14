package com.spendtracker.pro;

import android.content.*;
import android.database.*;
import android.net.Uri;
import java.util.*;

public class SmsImporter {
    public interface Callback { void onProgress(int done, int total); void onComplete(int count); void onError(String msg); }

    public static void importAll(Context ctx, Callback cb) {
        new Thread(() -> {
            try {
                Uri uri = Uri.parse("content://sms/inbox");
                long since = System.currentTimeMillis() - 90L * 86400000L;

                Cursor c;
                try {
                    c = ctx.getContentResolver().query(uri,
                            new String[]{"_id", "address", "body", "date"},
                            "date > ?", new String[]{String.valueOf(since)}, "date DESC");
                } catch (SecurityException se) {
                    if (cb != null) cb.onError("SMS permission denied. Please grant READ_SMS permission.");
                    return;
                }

                if (c == null) {
                    if (cb != null) cb.onError("Cannot access SMS inbox.");
                    return;
                }

                int total = c.getCount(), imported = 0;
                AppDatabase db = AppDatabase.getInstance(ctx);
                List<Transaction> batch = new ArrayList<>();

                while (c.moveToNext()) {
                    try {
                        int bodyIdx = c.getColumnIndex("body");
                        int addrIdx = c.getColumnIndex("address");
                        int dateIdx = c.getColumnIndex("date");

                        if (bodyIdx < 0 || addrIdx < 0 || dateIdx < 0) continue;

                        String body = c.getString(bodyIdx);
                        String addr = c.getString(addrIdx);
                        long date   = c.getLong(dateIdx);

                        if (body == null || body.trim().isEmpty()) continue;
                        if (addr == null) addr = "";

                        // Use BankAwareSmsParser for accurate bank-specific parsing
                        BankAwareSmsParser.ParseResult p = BankAwareSmsParser.parse(body, addr);
                        if (p == null) continue;
                        if (db.transactionDao().findBySms(body) != null) continue; // dedup

                        // Null-safe field assignment
                        Transaction tx = new Transaction();
                        tx.merchant       = p.merchant != null ? p.merchant : "Unknown";
                        tx.amount         = p.amount;
                        tx.paymentMethod  = p.paymentMethod != null ? p.paymentMethod : "BANK";
                        tx.paymentDetail  = p.paymentDetail != null ? p.paymentDetail : "";
                        tx.category       = p.category != null ? p.category : "Others";
                        tx.timestamp      = date;
                        tx.rawSms         = body;
                        tx.smsAddress     = addr;
                        tx.isManual       = false;

                        // Null-safe icon lookup
                        CategoryEngine.CategoryInfo info = CategoryEngine.getInfo(tx.category);
                        tx.categoryIcon = (info != null) ? info.icon : "💼";

                        batch.add(tx);
                        imported++;

                    } catch (Exception rowEx) {
                        // Skip malformed row, continue scanning
                    }

                    if (cb != null && c.getPosition() % 50 == 0) {
                        final int imp = imported;
                        cb.onProgress(imp, total);
                    }
                }
                c.close();

                // Single bulk insert — much faster than one-by-one for large batches
                if (!batch.isEmpty()) {
                    db.transactionDao().insertAll(batch);
                }

                if (cb != null) cb.onComplete(imported);

            } catch (Exception e) {
                if (cb != null) {
                    // e.getMessage() can itself be null — always guard it
                    String msg = e.getMessage();
                    cb.onError(msg != null ? msg : "Unexpected error during SMS scan.");
                }
            }
        }).start();
    }
}
