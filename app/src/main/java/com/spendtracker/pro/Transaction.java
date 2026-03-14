package com.spendtracker.pro;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// FIX 5: @Index on rawSms — makes findBySms() O(log n) instead of full table scan.
// Critical for duplicate detection during large SMS imports.
@Entity(tableName = "transactions",
        indices = {@Index(value = "rawSms")})
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String merchant;
    public double amount;
    public String category;      // Food, Transport, Shopping, etc.
    public String categoryIcon;  // emoji
    public String paymentMethod; // UPI, CREDIT_CARD, DEBIT_CARD, CASH, BANK
    public String paymentDetail;
    public long timestamp;
    public String notes;
    public String rawSms;
    public String smsAddress;
    public boolean isRecurring;
    public String recurringId;
    public boolean isManual;        // true = user added manually
    public boolean isSelfTransfer;  // true = excluded from totals

    public Transaction() {}

    public String getFormattedAmount() { return String.format("₹%.2f", amount); }

    public String getFormattedDate() {
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    public String getFormattedDateTime() {
        return new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date(timestamp));
    }

    public String getPaymentIcon() {
        switch (paymentMethod != null ? paymentMethod : "") {
            case "CREDIT_CARD": return "💳";
            case "DEBIT_CARD":  return "🏦";
            case "UPI":         return "📱";
            case "CASH":        return "💵";
            default:            return "🏛️";
        }
    }
}
