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
                Cursor c = ctx.getContentResolver().query(uri,
                        new String[]{"_id", "address", "body", "date"},
                        "date > ?", new String[]{String.valueOf(since)}, "date DESC");
                if (c == null) { if (cb != null) cb.onError("Cannot access SMS"); return; }
                int total = c.getCount(), imported = 0;
                AppDatabase db = AppDatabase.getInstance(ctx);
                List<Transaction> batch = new ArrayList<>();
                while (c.moveToNext()) {
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    String addr = c.getString(c.getColumnIndexOrThrow("address"));
                    long date = c.getLong(c.getColumnIndexOrThrow("date"));
                    // Use BankAwareSmsParser for accurate bank-specific parsing
                    BankAwareSmsParser.ParseResult p = BankAwareSmsParser.parse(body, addr);
                    if (p != null && db.transactionDao().findBySms(body) == null) {
                        Transaction tx = new Transaction();
                        tx.merchant = p.merchant; tx.amount = p.amount;
                        tx.paymentMethod = p.paymentMethod; tx.paymentDetail = p.paymentDetail;
                        tx.category = p.category; tx.timestamp = date;
                        tx.rawSms = body; tx.smsAddress = addr; tx.isManual = false;
                        tx.categoryIcon = CategoryEngine.getInfo(p.category).icon;
                        batch.add(tx); imported++;
                    }
                    if (cb != null && c.getPosition() % 50 == 0) {
                        final int pos = c.getPosition(), imp = imported;
                        cb.onProgress(imp, total);
                    }
                }
                c.close();
                for (Transaction tx : batch) db.transactionDao().insert(tx);
                if (cb != null) cb.onComplete(imported);
            } catch (Exception e) { if (cb != null) cb.onError(e.getMessage()); }
        }).start();
    }
}
