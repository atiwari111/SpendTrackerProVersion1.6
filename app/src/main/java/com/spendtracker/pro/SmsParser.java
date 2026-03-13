package com.spendtracker.pro;

import java.util.regex.*;

/**
 * SmsParser v1.5
 * - More robust transaction detection
 * - Handles ₹, Rs, INR, amount-first patterns
 * - Better merchant extraction
 * - UPI / Card / Bank detection
 */
public class SmsParser {

    // ── Amount patterns ──────────────────────────────────────────
    private static final Pattern[] AMOUNT_PATTERNS = {
        Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:INR|Rs\\.?|₹)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:debited|spent|paid|deducted|purchase(?:d)?)\\D{0,20}?([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:for|of)\\s+(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
    };

    // ── Transaction keywords ─────────────────────────────────────
    private static final Pattern SPEND_KW = Pattern.compile(
        "\\b(debited|deducted|spent|paid|payment|purchase|withdrawn|transaction|transferred)\\b",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern CREDIT_KW = Pattern.compile(
        "\\b(credited|received|credit|cashback|refund|reversal|reward|earned|deposit)\\b",
        Pattern.CASE_INSENSITIVE);

    // ── UPI app detection ────────────────────────────────────────
    private static final Pattern UPI_APP = Pattern.compile(
        "(PhonePe|Google\\s*Pay|GPay|Paytm|BHIM|Amazon\\s*Pay|WhatsApp\\s*Pay|MobiKwik|FreeCharge|CRED|Slice|Fi)",
        Pattern.CASE_INSENSITIVE);

    // ── Card last-4 ──────────────────────────────────────────────
    private static final Pattern CARD_LAST4 = Pattern.compile(
        "(?:card|a/c|acct)\\.?\\s*(?:no\\.?)?\\s*[Xx*]{0,8}([0-9]{4})",
        Pattern.CASE_INSENSITIVE);

    // ── Merchant patterns ────────────────────────────────────────
    private static final Pattern[] MERCHANT_PATTERNS = {
        Pattern.compile("\\bto\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bat\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btowards\\s+([A-Za-z0-9&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:at|to)\\s+([A-Za-z0-9&'./\\-]{2,30})", Pattern.CASE_INSENSITIVE),
    };

    private static final String[] NOISE = {
        "your", "the", "a", "an", "via", "using", "bank", "account", "wallet", "linked"
    };

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    public static boolean isTransaction(String sms) {
        if (sms == null || sms.isEmpty()) return false;

        String lower = sms.toLowerCase();

        if (!SPEND_KW.matcher(lower).find()) return false;

        if (CREDIT_KW.matcher(lower).find()) return false;

        return extractAmount(sms) > 0;
    }

    public static ParsedTransaction parse(String body, String sender) {

        if (!isTransaction(body)) return null;

        double amount = extractAmount(body);
        if (amount <= 0) return null;

        String bank = BankDetector.detectName(sender, body);

        String paymentMethod;
        String paymentDetail;

        String lower = body.toLowerCase();

        if (lower.contains("upi") || UPI_APP.matcher(body).find()) {

            paymentMethod = "UPI";

            Matcher m = UPI_APP.matcher(body);

            paymentDetail = m.find()
                    ? m.group(1) + " UPI"
                    : (bank.isEmpty() ? "UPI" : bank + " UPI");

        } else if (lower.contains("credit card") || lower.contains("credit a/c")) {

            paymentMethod = "CREDIT_CARD";
            paymentDetail = buildCardDetail(body, bank, "Credit");

        } else if (lower.contains("debit card") || lower.contains("debit a/c")) {

            paymentMethod = "DEBIT_CARD";
            paymentDetail = buildCardDetail(body, bank, "Debit");

        } else {

            paymentMethod = "BANK";
            paymentDetail = bank.isEmpty()
                    ? "Bank Transfer"
                    : bank + " Bank Transfer";
        }

        String merchant = extractMerchant(body);

        if (merchant == null || merchant.trim().isEmpty())
            merchant = "Unknown";

        String category = CategoryEngine.classify(merchant, body);

        String upiId = UpiDetector.detectUpiId(body);

        return new ParsedTransaction(
                merchant.trim(),
                amount,
                paymentMethod,
                paymentDetail,
                category,
                upiId
        );
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    public static double extractAmount(String sms) {

        for (Pattern p : AMOUNT_PATTERNS) {

            Matcher m = p.matcher(sms);

            if (m.find()) {

                try {

                    double val = Double.parseDouble(
                            m.group(1).replace(",", "")
                    );

                    if (val > 0 && val < 10_000_000)
                        return val;

                } catch (Exception ignored) {
                }
            }
        }

        return -1;
    }

    public static String extractMerchant(String sms) {

        for (Pattern p : MERCHANT_PATTERNS) {

            Matcher m = p.matcher(sms);

            if (m.find()) {

                String raw = m.group(1).trim();

                raw = removeNoise(raw);

                if (raw.length() < 2)
                    continue;

                if (raw.matches(".*\\d{5,}.*"))
                    continue;

                if (raw.length() <= 40)
                    return titleCase(raw);
            }
        }

        return null;
    }

    private static String removeNoise(String s) {

        for (String n : NOISE) {

            s = s.replaceAll("(?i)\\b" + n + "\\b", "");
        }

        return s.replaceAll("\\s{2,}", " ").trim();
    }

    private static String buildCardDetail(String sms, String bank, String type) {

        Matcher m = CARD_LAST4.matcher(sms);

        String last4 = m.find()
                ? " (xx" + m.group(1) + ")"
                : "";

        return (bank.isEmpty() ? "" : bank + " ")
                + type
                + " Card"
                + last4;
    }

    private static String titleCase(String s) {

        StringBuilder sb = new StringBuilder();

        for (String w : s.toLowerCase().split("\\s+")) {

            if (!w.isEmpty()) {

                sb.append(Character.toUpperCase(w.charAt(0)));

                if (w.length() > 1)
                    sb.append(w.substring(1));

                sb.append(" ");
            }
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────
    // RESULT CLASS
    // ─────────────────────────────────────────────────────────────

    public static class ParsedTransaction {

        public final String merchant;
        public final double amount;
        public final String paymentMethod;
        public final String paymentDetail;
        public final String category;
        public final String upiId;

        public ParsedTransaction(
                String merchant,
                double amount,
                String paymentMethod,
                String paymentDetail,
                String category,
                String upiId
        ) {

            this.merchant = merchant;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDetail = paymentDetail;
            this.category = category;
            this.upiId = upiId;
        }
    }
}
