package com.spendtracker.pro;

import java.util.regex.*;

/**
 * SmsParser v1.6
 * - Fixed: credit card SMS no longer rejected by isTransaction()
 * - Fixed: orphaned return statement removed
 * - Null-safe throughout
 * - Handles ₹, Rs, INR, amount-first patterns
 * - Better merchant extraction (multi-word, stops at noise words)
 * - UPI / Card / Bank detection
 */
public class SmsParser {

    // ── Amount patterns ──────────────────────────────────────────
    private static final Pattern[] AMOUNT_PATTERNS = {
        // ₹350, Rs.350, INR 350 (prefix)
        Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // 350 INR / 350 Rs (suffix)
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:INR|Rs\\.?|₹)", Pattern.CASE_INSENSITIVE),
        // debited/paid/spent ... 350
        Pattern.compile("(?:debited|spent|paid|deducted|purchase(?:d)?)\\D{0,20}?([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        // for Rs 350 / of Rs 350
        Pattern.compile("(?:for|of)\\s+(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
    };

    // ── Transaction keywords ─────────────────────────────────────
    private static final Pattern SPEND_KW = Pattern.compile(
        "\\b(debited|deducted|spent|paid|payment|purchase|withdrawn|transaction|transferred)\\b",
        Pattern.CASE_INSENSITIVE);

    // FIX: Removed "credit" and "received" — they caused ALL credit card SMS to
    // be rejected. "credit" alone appears in "Credit Card" and "credit a/c".
    // Only keep unambiguous credit-only keywords.
    private static final Pattern CREDIT_KW = Pattern.compile(
        "\\b(credited|cashback|refund|reversal|reward|earned|deposit)\\b",
        Pattern.CASE_INSENSITIVE);

    // ── UPI app detection ────────────────────────────────────────
    private static final Pattern UPI_APP = Pattern.compile(
        "(PhonePe|Google\\s*Pay|GPay|Paytm|BHIM|Amazon\\s*Pay|WhatsApp\\s*Pay|MobiKwik|FreeCharge|CRED|Slice|Fi)",
        Pattern.CASE_INSENSITIVE);

    // ── Card last-4 ──────────────────────────────────────────────
    private static final Pattern CARD_LAST4 = Pattern.compile(
        "(?:card|a/c|acct)\\.?\\s*(?:no\\.?)?\\s*[Xx*]{0,8}([0-9]{4})",
        Pattern.CASE_INSENSITIVE);

    // ── Merchant patterns (ordered: most specific → least) ───────
    private static final Pattern[] MERCHANT_PATTERNS = {
        // "to MERCHANT on" / "to MERCHANT for" / "to MERCHANT."
        Pattern.compile("\\bto\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        // "at MERCHANT on" / "at MERCHANT for"
        Pattern.compile("\\bat\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        // "towards MERCHANT"
        Pattern.compile("\\btowards\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        // simple "at MERCHANT" or "to MERCHANT" with end-of-string
        Pattern.compile("\\b(?:at|to)\\s+([A-Za-z0-9&'./\\-]{2,30})", Pattern.CASE_INSENSITIVE),
    };

    // Known noise words that aren't real merchant names
    private static final String[] NOISE = {"your", "the", "a", "an", "via", "using", "bank", "account", "wallet", "linked"};


    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    /** Returns true if this SMS looks like a debit/spend transaction */
    public static boolean isTransaction(String sms) {
        if (sms == null || sms.isEmpty()) return false;
        String lower = sms.toLowerCase();
        // Must have a spend keyword
        if (!SPEND_KW.matcher(lower).find()) return false;
        // Must NOT be purely a credit/refund (cashback, reversal, etc.)
        if (CREDIT_KW.matcher(lower).find()) return false;
        // Must have an amount
        return extractAmount(sms) > 0;
    }

    /** Full parse — returns null if not a valid spend SMS */
    public static ParsedTransaction parse(String body, String sender) {
        if (body == null || sender == null) return null;
        if (!isTransaction(body)) return null;

        double amount = extractAmount(body);
        if (amount <= 0) return null;

        String bank = BankDetector.detectName(sender, body);
        if (bank == null) bank = "";

        String paymentMethod, paymentDetail;
        String lower = body.toLowerCase();

        if (lower.contains("upi") || UPI_APP.matcher(body).find()) {
            paymentMethod = "UPI";
            Matcher m = UPI_APP.matcher(body);
            paymentDetail = m.find() ? m.group(1) + " UPI"
                                     : (bank.isEmpty() ? "UPI" : bank + " UPI");
        } else if (lower.contains("credit card") || lower.contains("credit a/c")) {
            paymentMethod = "CREDIT_CARD";
            paymentDetail = buildCardDetail(body, bank, "Credit");
        } else if (lower.contains("debit card") || lower.contains("debit a/c")) {
            paymentMethod = "DEBIT_CARD";
            paymentDetail = buildCardDetail(body, bank, "Debit");
        } else {
            paymentMethod = "BANK";
            paymentDetail = bank.isEmpty() ? "Bank Transfer" : bank + " Bank Transfer";
        }

        String merchant = extractMerchant(body);
        if (merchant == null || merchant.trim().isEmpty()) merchant = "Unknown";

        String category = CategoryEngine.classify(merchant, body);
        if (category == null) category = "Others";

        String upiId = UpiDetector.detectUpiId(body);
        return new ParsedTransaction(merchant.trim(), amount, paymentMethod, paymentDetail, category, upiId);
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    public static double extractAmount(String sms) {
        if (sms == null) return -1;
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(sms);
            if (m.find()) {
                try {
                    double val = Double.parseDouble(m.group(1).replace(",", ""));
                    if (val > 0 && val < 10_000_000) return val; // sanity: < 1 crore
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    public static String extractMerchant(String sms) {
        if (sms == null) return null;
        for (Pattern p : MERCHANT_PATTERNS) {
            Matcher m = p.matcher(sms);
            if (m.find()) {
                String raw = m.group(1).trim();
                raw = removeNoise(raw);
                // Reject if it looks like an amount, date, or bank name
                if (raw.length() < 2 || raw.matches(".*\\d{5,}.*")) continue;
                if (raw.length() <= 40) return titleCase(raw);
            }
        }
        return null;
    }

    private static String removeNoise(String s) {
        if (s == null) return "";
        for (String n : NOISE) {
            s = s.replaceAll("(?i)\\b" + n + "\\b", "");
        }
        return s.replaceAll("\\s{2,}", " ").trim();
    }

    private static String buildCardDetail(String sms, String bank, String type) {
        Matcher m = CARD_LAST4.matcher(sms);
        String last4 = m.find() ? " (xx" + m.group(1) + ")" : "";
        return (bank.isEmpty() ? "" : bank + " ") + type + " Card" + last4;
    }

    private static String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String w : s.toLowerCase().split("\\s+")) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) sb.append(w.substring(1));
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────
    // RESULT CLASS
    // ─────────────────────────────────────────────────────────────

    public static class ParsedTransaction {
        public final String merchant, paymentMethod, paymentDetail, category, upiId;
        public final double amount;

        public ParsedTransaction(String merchant, double amount,
                                 String paymentMethod, String paymentDetail,
                                 String category, String upiId) {
            this.merchant = merchant;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDetail = paymentDetail;
            this.category = category;
            this.upiId = upiId;
        }
    }
}
