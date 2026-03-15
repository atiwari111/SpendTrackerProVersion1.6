package com.spendtracker.pro;

import java.util.regex.*;

/**
 * SmsParser v2.1
 *
 * Improvements
 * - Supports CREDIT transactions (salary, cashback, dividend, refund)
 * - Detects UPI "Txn Rs..." SMS formats
 * - Detects transaction type (DEBIT / CREDIT)
 * - Better merchant extraction for UPI QR
 * - Supports HDFC / ICICI / SBI QR transactions
 */

public class SmsParser {

    // ── Special pattern for HDFC "Txn Rs" format ─────────────────
    private static final Pattern TXN_RS_PATTERN =
            Pattern.compile("Txn\\s+Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
                    Pattern.CASE_INSENSITIVE);

    // ── Amount patterns ──────────────────────────────────────────
    private static final Pattern[] AMOUNT_PATTERNS = {

        TXN_RS_PATTERN,

        Pattern.compile("(?:INR|Rs\\.?|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
                Pattern.CASE_INSENSITIVE),

        Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:INR|Rs\\.?|₹)",
                Pattern.CASE_INSENSITIVE),

        Pattern.compile("(?:debited|spent|paid|deducted|purchase(?:d)?)\\D{0,20}?([0-9,]+(?:\\.[0-9]{1,2})?)",
                Pattern.CASE_INSENSITIVE),

        Pattern.compile("(?:for|of)\\s+(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)",
                Pattern.CASE_INSENSITIVE)
    };

    // ── Debit keywords ───────────────────────────────────────────
    private static final Pattern SPEND_KW = Pattern.compile(
            "\\b(debited|deducted|spent|paid|payment|purchase|withdrawn|transaction|txn|transferred|upi|txn\\s+rs)\\b",
            Pattern.CASE_INSENSITIVE);

    // ── Credit keywords ──────────────────────────────────────────
    private static final Pattern CREDIT_KW = Pattern.compile(
            "\\b(credited|salary|cashback|refund|reversal|reward|earned|deposit|dividend|interest|received)\\b",
            Pattern.CASE_INSENSITIVE);

    // ── UPI detection ────────────────────────────────────────────
    private static final Pattern UPI_APP = Pattern.compile(
            "(PhonePe|Google\\s*Pay|GPay|Paytm|BHIM|Amazon\\s*Pay|WhatsApp\\s*Pay|MobiKwik|FreeCharge|CRED|Slice|Fi)",
            Pattern.CASE_INSENSITIVE);

    // ── Card last4 ───────────────────────────────────────────────
    private static final Pattern CARD_LAST4 = Pattern.compile(
            "(?:card|a/c|acct)\\.?\\s*(?:no\\.?)?\\s*[Xx*]{0,8}([0-9]{4})",
            Pattern.CASE_INSENSITIVE);

    // ── Merchant patterns ────────────────────────────────────────
    private static final Pattern[] MERCHANT_PATTERNS = {

            Pattern.compile("\\bto\\s+([A-Za-z0-9@&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),

            Pattern.compile("\\bat\\s+([A-Za-z0-9@&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),

            Pattern.compile("\\btowards\\s+([A-Za-z0-9@&'./\\-\\s]{2,35}?)\\s+(?:on|for|via|using|ref|\\-|\\.|,|$)", Pattern.CASE_INSENSITIVE),

            Pattern.compile("\\b(?:at|to)\\s+([A-Za-z0-9@&'./\\-]{2,40})", Pattern.CASE_INSENSITIVE)
    };

    private static final String[] NOISE = {"your","the","a","an","via","using","bank","account","wallet","linked"};

    // ─────────────────────────────────────────────────────────────
    // TRANSACTION TYPE DETECTION
    // ─────────────────────────────────────────────────────────────

    public static boolean isTransaction(String sms) {

        if (sms == null || sms.isEmpty()) return false;

        if (extractAmount(sms) <= 0) return false;

        if (SPEND_KW.matcher(sms).find()) return true;

        if (CREDIT_KW.matcher(sms).find()) return true;

        if (TXN_RS_PATTERN.matcher(sms).find()) return true;

        return false;
    }

    public static String detectType(String sms){

        if(sms==null) return "DEBIT";

        if(CREDIT_KW.matcher(sms).find()) return "CREDIT";

        return "DEBIT";
    }

    // ─────────────────────────────────────────────────────────────
    // MAIN PARSER
    // ─────────────────────────────────────────────────────────────

    public static ParsedTransaction parse(String body,String sender){

        if(body==null || sender==null) return null;

        if(!isTransaction(body)) return null;

        double amount = extractAmount(body);

        if(amount<=0) return null;

        String type = detectType(body);

        String bank = BankDetector.detectName(sender,body);

        if(bank==null) bank="";

        String paymentMethod;

        String paymentDetail;

        String lower = body.toLowerCase();

        if(lower.contains("upi") || UPI_APP.matcher(body).find()){

            paymentMethod="UPI";

            Matcher m=UPI_APP.matcher(body);

            paymentDetail=m.find()? m.group(1)+" UPI" : "UPI";

        }
        else if(lower.contains("credit card")){

            paymentMethod="CREDIT_CARD";

            paymentDetail=buildCardDetail(body,bank,"Credit");

        }
        else if(lower.contains("debit card")){

            paymentMethod="DEBIT_CARD";

            paymentDetail=buildCardDetail(body,bank,"Debit");

        }
        else{

            paymentMethod="BANK";

            paymentDetail=bank+" Bank Transfer";

        }

        String merchant = extractMerchant(body);

        if(merchant==null || merchant.isEmpty()) merchant="Unknown";

        String category = CategoryEngine.classify(merchant,body);

        if(category==null) category="Others";

        String upiId = UpiDetector.detectUpiId(body);

        return new ParsedTransaction(
                merchant,
                amount,
                type,
                paymentMethod,
                paymentDetail,
                category,
                upiId
        );
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    public static double extractAmount(String sms){

        if(sms==null) return -1;

        for(Pattern p:AMOUNT_PATTERNS){

            Matcher m=p.matcher(sms);

            if(m.find()){

                try{

                    double val=Double.parseDouble(m.group(1).replace(",",""));

                    if(val>0 && val<10000000) return val;

                }catch(Exception ignored){}
            }
        }

        return -1;
    }

    public static String extractMerchant(String sms){

        if(sms==null) return null;

        for(Pattern p:MERCHANT_PATTERNS){

            Matcher m=p.matcher(sms);

            if(m.find()){

                String raw=m.group(1).trim();

                raw=removeNoise(raw);

                if(raw.length()<2) continue;

                if(raw.matches(".*\\d{6,}.*")) continue;

                return titleCase(raw);
            }
        }

        return null;
    }

    private static String removeNoise(String s){

        if(s==null) return "";

        for(String n:NOISE){

            s=s.replaceAll("(?i)\\b"+n+"\\b","");
        }

        return s.replaceAll("\\s{2,}"," ").trim();
    }

    private static String buildCardDetail(String sms,String bank,String type){

        Matcher m=CARD_LAST4.matcher(sms);

        String last4=m.find()? " (xx"+m.group(1)+")":"";

        return (bank.isEmpty()? "" : bank+" ")+type+" Card"+last4;
    }

    private static String titleCase(String s){

        if(s==null || s.isEmpty()) return s;

        StringBuilder sb=new StringBuilder();

        for(String w:s.toLowerCase().split("\\s+")){

            if(!w.isEmpty()){

                sb.append(Character.toUpperCase(w.charAt(0)));

                if(w.length()>1) sb.append(w.substring(1));

                sb.append(" ");
            }
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────
    // RESULT CLASS
    // ─────────────────────────────────────────────────────────────

    public static class ParsedTransaction{

        public final String merchant,type,paymentMethod,paymentDetail,category,upiId;

        public final double amount;

        public ParsedTransaction(
                String merchant,
                double amount,
                String type,
                String paymentMethod,
                String paymentDetail,
                String category,
                String upiId){

            this.merchant=merchant;
            this.amount=amount;
            this.type=type;
            this.paymentMethod=paymentMethod;
            this.paymentDetail=paymentDetail;
            this.category=category;
            this.upiId=upiId;
        }
    }
}
