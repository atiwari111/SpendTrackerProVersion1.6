package com.spendtracker.pro;

import java.util.regex.*;

/**
 * BankAwareSmsParser v1.6
 *
 * Different banks send wildly different SMS formats.
 * Generic regex often fails on bank-specific templates.
 *
 * This class tries bank-specific patterns FIRST, then falls back
 * to the generic SmsParser.
 *
 * Example bank SMS formats:
 * ──────────────────────────────────────────────────────────────
 * HDFC:   Rs 450 debited from A/c XX1234 on 13-Mar to SWIGGY via UPI
 * SBI:    INR 450 spent on card XX1234 at AMAZON on 13-Mar-25
 * ICICI:  UPI txn of Rs 320 to ZOMATO Ref No 123456 on 13/03/25
 * AXIS:   INR 200.00 debited from AXIS Bank ac XX5678 towards NETFLIX on 13-03-25
 * KOTAK:  Rs.180.00 debited from Kotak Bank A/c XX9012 to BLINKIT via UPI
 * YES:    ₹500 debited from YBL A/c XX3456 to FLIPKART via UPI
 * ──────────────────────────────────────────────────────────────
 */
public class BankAwareSmsParser {

    // ── Result class ─────────────────────────────────────────────
    public static class ParseResult {
        public final double   amount;
        public final String   merchant;
        public final String   paymentMethod;
        public final String   paymentDetail;
        public final String   category;
        public final String   bankName;
        public final boolean  isUpi;

        ParseResult(double amount, String merchant, String paymentMethod,
                    String paymentDetail, String category, String bankName, boolean isUpi) {
            this.amount        = amount;
            this.merchant      = merchant;
            this.paymentMethod = paymentMethod;
            this.paymentDetail = paymentDetail;
            this.category      = category;
            this.bankName      = bankName;
            this.isUpi         = isUpi;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BANK-SPECIFIC PATTERNS
    // Each pattern group: [amountGroup, merchantGroup, patternRegex]
    // ═══════════════════════════════════════════════════════════════

    // ── HDFC ──────────────────────────────────────────────────────
    // Rs 450 debited from A/c XX1234 on 13-Mar to SWIGGY via UPI
    // Rs.2,500 debited from a/c XX1234 on 12-Mar-25.Info:AMAZON.Avbl Bal:12345.67
    private static final Pattern HDFC_DEBIT = Pattern.compile(
        "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:has been\\s*)?debited.*?to\\s+([A-Za-z0-9&'./\\-\\s]{2,40}?)\\s*(?:via|Ref|Avbl|$)",
        Pattern.CASE_INSENSITIVE);

    // Rs. 1,234.00 debited from HDFC Bank A/c XX1234.Info:ZOMATO
    private static final Pattern HDFC_INFO = Pattern.compile(
        "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*debited.*?Info:([A-Za-z0-9&'./\\-\\s]{2,40})",
        Pattern.CASE_INSENSITIVE);

    // ── SBI ───────────────────────────────────────────────────────
    // INR 450 spent on card XX1234 at AMAZON on 13-Mar-25
    // Your A/c XX1234 debited by INR 320.00 on 13-Mar-25. Merchant: FLIPKART
    private static final Pattern SBI_SPENT = Pattern.compile(
        "(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2])?)\\s*(?:spent|debited).*?(?:at|to|merchant:?)\\s+([A-Za-z0-9&'./\\-\\s]{2,40}?)(?:\\s+on|\\s+via|\\s*Ref|\\s*Avl|\\.|,|$)",
        Pattern.CASE_INSENSITIVE);

    // SBI: IMPS/UPI: Rs 320 debited from A/c XX1234 to ZOMATO (Ref:123)
    private static final Pattern SBI_UPI = Pattern.compile(
        "(?:IMPS|UPI)[/\\s].*?(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*debited.*?to\\s+([A-Za-z0-9@&'./\\-\\s]{2,50}?)(?:\\s*\\(|\\s+Ref|\\.|,|$)",
        Pattern.CASE_INSENSITIVE);

    // ── ICICI ─────────────────────────────────────────────────────
    // UPI txn of Rs 320 to ZOMATO Ref No 123456 on 13/03/25
    // ICICI Bank: Rs 500 debited from A/c XX1234 on 13/03. Info: NETFLIX. Avail Bal: Rs 10000
    private static final Pattern ICICI_UPI = Pattern.compile(
        "UPI\\s+txn\\s+of\\s+(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s+to\\s+([A-Za-z0-9&'./\\-\\s]{2,40}?)\\s+Ref",
        Pattern.CASE_INSENSITIVE);

    private static final Pattern ICICI_INFO = Pattern.compile(
        "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*debited.*?Info[:\\s]+([A-Za-z0-9&'./\\-\\s]{2,40}?)(?:\\.|Avail|$)",
        Pattern.CASE_INSENSITIVE);

    // ── AXIS ──────────────────────────────────────────────────────
    // INR 200.00 debited from AXIS Bank ac XX5678 towards NETFLIX on 13-03-25
    // INR 350 spent on your Axis Bank Credit Card XX5678 at SWIGGY on 13-03-25
    private static final Pattern AXIS_TOWARDS = Pattern.compile(
        "(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|spent).*?(?:towards|at|to)\\s+([A-Za-z0-9&'./\\-\\s]{2,40}?)\\s+on",
        Pattern.CASE_INSENSITIVE);

    // ── KOTAK ─────────────────────────────────────────────────────
    // Rs.180.00 debited from Kotak Bank A/c XX9012 to BLINKIT via UPI
    private static final Pattern KOTAK_TO = Pattern.compile(
        "(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*debited.*?to\\s+([A-Za-z0-9&'./\\-\\s]{2,40}?)(?:\\s+via|\\s+Ref|\\.|$)",
        Pattern.CASE_INSENSITIVE);

    // ── GENERIC FALLBACKS ─────────────────────────────────────────
    // "UPI/IMPS payment of Rs 500 to merchant@upi" — extract UPI recipient
    private static final Pattern GENERIC_UPI_TO = Pattern.compile(
        "(?:payment|txn|transaction|transfer).*?(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2])?).*?to\\s+([A-Za-z0-9@&'./\\-\\s]{2,50}?)(?:\\s+Ref|\\s+on|\\.|,|$)",
        Pattern.CASE_INSENSITIVE);

    // ═══════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════

    /**
     * Main entry point.
     * Detects the bank, tries bank-specific pattern, falls back to generic SmsParser.
     */
    public static ParseResult parse(String body, String sender) {
        if (body == null || body.isEmpty()) return null;

        // Detect bank first
        BankDetector.BankInfo bankInfo = BankDetector.detect(sender, body);
        String bank = bankInfo.name; // e.g. "HDFC", "SBI", "ICICI"

        // Try bank-specific parser
        AmountMerchant am = null;
        switch (bank) {
            case "HDFC":
                am = tryPatterns(body, HDFC_DEBIT, HDFC_INFO);
                break;
            case "SBI":
                am = tryPatterns(body, SBI_UPI, SBI_SPENT);
                break;
            case "ICICI":
                am = tryPatterns(body, ICICI_UPI, ICICI_INFO);
                break;
            case "AXIS":
                am = tryPatterns(body, AXIS_TOWARDS);
                break;
            case "KOTAK":
                am = tryPatterns(body, KOTAK_TO);
                break;
            default:
                am = tryPatterns(body, GENERIC_UPI_TO);
                break;
        }

        // Fall back to generic SmsParser if bank-specific failed
        if (am == null || am.amount <= 0) {
            SmsParser.ParsedTransaction generic = SmsParser.parse(body, sender);
            if (generic == null) return null;
            String detail = buildPaymentDetail(bank, generic.paymentDetail);
            return new ParseResult(generic.amount, generic.merchant,
                    generic.paymentMethod, detail, generic.category, bank, isUpi(body));
        }

        // Bank-specific result
        String merchant       = am.merchant;
        String category       = CategoryEngine.classify(merchant, body);
        String paymentMethod  = detectPaymentMethod(body);
        String paymentDetail  = buildPaymentDetail(bank, buildDetailFromBody(body, bank));
        String upiId          = UpiDetector.detectUpiId(body);
        if (upiId != null) paymentDetail += " · " + upiId;

        return new ParseResult(am.amount, merchant, paymentMethod, paymentDetail,
                category, bank, isUpi(body));
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════

    /** Try a list of patterns and return first successful AmountMerchant */
    private static AmountMerchant tryPatterns(String body, Pattern... patterns) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(body);
            if (m.find()) {
                try {
                    double amount = Double.parseDouble(m.group(1).replace(",", ""));
                    String merchant = m.group(2).trim();
                    merchant = cleanMerchant(merchant);
                    if (amount > 0 && merchant.length() >= 2) {
                        return new AmountMerchant(amount, titleCase(merchant));
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /** Strip trailing noise from extracted merchant name */
    private static String cleanMerchant(String s) {
        // Remove trailing "via UPI", "Ref...", date patterns
        s = s.replaceAll("(?i)\\s+(via|ref|on|avail|avbl|mob|utr)\\b.*$", "").trim();
        s = s.replaceAll("(?i)\\b(your|the|a|an)\\b", "").trim();
        s = s.replaceAll("\\s{2,}", " ").trim();
        return s;
    }

    private static String detectPaymentMethod(String body) {
        String lower = body.toLowerCase();
        if (lower.contains("upi"))         return "UPI";
        if (lower.contains("credit card") || lower.contains("credit a/c")) return "CREDIT_CARD";
        if (lower.contains("debit card")  || lower.contains("debit a/c"))  return "DEBIT_CARD";
        if (lower.contains("imps"))        return "BANK";
        if (lower.contains("neft"))        return "BANK";
        return "BANK";
    }

    private static String buildDetailFromBody(String body, String bank) {
        String method = detectPaymentMethod(body);
        Matcher card = Pattern.compile("[Xx*]{0,8}([0-9]{4})").matcher(body);
        String last4 = card.find() ? " (xx" + card.group(1) + ")" : "";
        switch (method) {
            case "UPI":          return bank + " UPI";
            case "CREDIT_CARD":  return bank + " Credit Card" + last4;
            case "DEBIT_CARD":   return bank + " Debit Card" + last4;
            default:             return bank + " Bank Transfer";
        }
    }

    private static String buildPaymentDetail(String bank, String existing) {
        if (bank == null || bank.isEmpty()) return existing;
        if (existing != null && existing.toUpperCase().startsWith(bank)) return existing;
        return bank + " " + (existing != null ? existing : "");
    }

    private static boolean isUpi(String body) {
        return body != null && body.toLowerCase().contains("upi");
    }

    private static String titleCase(String s) {
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

    /** Internal holder for extracted amount + merchant */
    private static class AmountMerchant {
        final double amount;
        final String merchant;
        AmountMerchant(double amount, String merchant) {
            this.amount = amount;
            this.merchant = merchant;
        }
    }
}
