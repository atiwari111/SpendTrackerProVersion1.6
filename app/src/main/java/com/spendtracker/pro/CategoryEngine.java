package com.spendtracker.pro;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public class CategoryEngine {

    public static class CategoryInfo {
        public final String name, icon;
        public final int color;

        public CategoryInfo(String name, String icon, int color) {
            this.name = name;
            this.icon = icon;
            this.color = color;
        }
    }

    //────────────────────────────
    // CATEGORY LIST
    //────────────────────────────

    public static final Map<String, CategoryInfo> CATEGORIES = new LinkedHashMap<>();

    static {

        CATEGORIES.put("🍔 Food", new CategoryInfo("🍔 Food","🍔",0xFFFF6B6B));
        CATEGORIES.put("🛒 Groceries", new CategoryInfo("🛒 Groceries","🛒",0xFF4ECDC4));
        CATEGORIES.put("🚗 Transport", new CategoryInfo("🚗 Transport","🚗",0xFF45B7D1));
        CATEGORIES.put("⛽ Fuel", new CategoryInfo("⛽ Fuel","⛽",0xFFFFBE0B));
        CATEGORIES.put("✈️ Travel", new CategoryInfo("✈️ Travel","✈️",0xFF96CEB4));
        CATEGORIES.put("🛍️ Shopping", new CategoryInfo("🛍️ Shopping","🛍️",0xFFDDA0DD));
        CATEGORIES.put("🏠 Rent", new CategoryInfo("🏠 Rent","🏠",0xFFFF8C69));
        CATEGORIES.put("🔌 Bills", new CategoryInfo("🔌 Bills","🔌",0xFFA8E6CF));
        CATEGORIES.put("🎬 Entertainment", new CategoryInfo("🎬 Entertainment","🎬",0xFFFFD3B6));
        CATEGORIES.put("🏥 Health", new CategoryInfo("🏥 Health","🏥",0xFFFF6B9D));
        CATEGORIES.put("💊 Medicine", new CategoryInfo("💊 Medicine","💊",0xFFB8E0FF));
        CATEGORIES.put("📚 Education", new CategoryInfo("📚 Education","📚",0xFFC3B1E1));
        CATEGORIES.put("💪 Fitness", new CategoryInfo("💪 Fitness","💪",0xFF98D8C8));
        CATEGORIES.put("💰 Investment", new CategoryInfo("💰 Investment","💰",0xFFFFD700));
        CATEGORIES.put("🎁 Gifts", new CategoryInfo("🎁 Gifts","🎁",0xFFFF9AA2));

        // Income categories
        CATEGORIES.put("💵 Salary", new CategoryInfo("💵 Salary","💵",0xFF4CAF50));
        CATEGORIES.put("🎉 Cashback", new CategoryInfo("🎉 Cashback","🎉",0xFF81C784));
        CATEGORIES.put("📈 Investment Return", new CategoryInfo("📈 Investment Return","📈",0xFF66BB6A));
        CATEGORIES.put("↩️ Refund", new CategoryInfo("↩️ Refund","↩️",0xFF64B5F6));

        CATEGORIES.put("💼 Others", new CategoryInfo("💼 Others","💼",0xFFB0BEC5));
    }

    //────────────────────────────
    // MERCHANT MAP
    //────────────────────────────

    public static final Map<String,String> MERCHANT_MAP = new LinkedHashMap<>();

    static {

        MERCHANT_MAP.put("swiggy","🍔 Food");
        MERCHANT_MAP.put("zomato","🍔 Food");
        MERCHANT_MAP.put("dominos","🍔 Food");

        MERCHANT_MAP.put("amazon","🛍️ Shopping");
        MERCHANT_MAP.put("flipkart","🛍️ Shopping");

        MERCHANT_MAP.put("uber","🚗 Transport");
        MERCHANT_MAP.put("ola","🚗 Transport");

        MERCHANT_MAP.put("hp","⛽ Fuel");
        MERCHANT_MAP.put("shell","⛽ Fuel");

        MERCHANT_MAP.put("netflix","🎬 Entertainment");
        MERCHANT_MAP.put("spotify","🎬 Entertainment");

        MERCHANT_MAP.put("zerodha","💰 Investment");
        MERCHANT_MAP.put("groww","💰 Investment");
    }

    //────────────────────────────
    // KEYWORDS
    //────────────────────────────

    private static final Map<String,String[]> KEYWORDS = new LinkedHashMap<>();

    static {

        KEYWORDS.put("🍔 Food", new String[]{"restaurant","pizza","burger","cafe"});
        KEYWORDS.put("🛒 Groceries", new String[]{"grocery","kirana","vegetable"});
        KEYWORDS.put("🚗 Transport", new String[]{"metro","cab","taxi","auto"});
        KEYWORDS.put("⛽ Fuel", new String[]{"petrol","diesel","fuel"});
        KEYWORDS.put("🛍️ Shopping", new String[]{"mall","store","shopping"});
        KEYWORDS.put("🔌 Bills", new String[]{"electricity","recharge","broadband"});
        KEYWORDS.put("🎬 Entertainment", new String[]{"movie","subscription","ott"});
        KEYWORDS.put("🏥 Health", new String[]{"hospital","clinic","doctor"});
        KEYWORDS.put("💊 Medicine", new String[]{"pharmacy","medicine"});
        KEYWORDS.put("📚 Education", new String[]{"school","college","course"});
        KEYWORDS.put("💪 Fitness", new String[]{"gym","fitness","yoga"});
        KEYWORDS.put("💰 Investment", new String[]{"sip","stocks","mutual fund"});
        KEYWORDS.put("🎁 Gifts", new String[]{"gift","flower","cake"});
    }

    //────────────────────────────
    // MERCHANT LEARNING
    //────────────────────────────

    private static final String PREFS_NAME="merchant_learning";
    private static SharedPreferences learningPrefs;

    public static void init(Context ctx){
        learningPrefs=ctx.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE);
    }

    public static void learnMerchant(String merchant,String category){

        if(learningPrefs==null||merchant==null||category==null) return;

        learningPrefs.edit()
                .putString(merchant.toLowerCase().trim(),category)
                .apply();
    }

    private static String getLearnedCategory(String merchant){

        if(learningPrefs==null||merchant==null) return null;

        return learningPrefs.getString(merchant.toLowerCase().trim(),null);
    }

    //────────────────────────────
    // CLASSIFY ENGINE
    //────────────────────────────

    public static String classify(String merchant,String smsBody){

        if(merchant==null) merchant="";
        merchant=merchant.toLowerCase();

        String body = smsBody!=null ? smsBody.toLowerCase() : "";

        //────────────────────────
        // INCOME DETECTION
        //────────────────────────

        if(merchant.contains("salary") || body.contains("salary"))
            return "💵 Salary";

        if(merchant.contains("cashback") || body.contains("cashback"))
            return "🎉 Cashback";

        if(merchant.contains("dividend") || body.contains("dividend"))
            return "📈 Investment Return";

        if(merchant.contains("interest") || body.contains("interest"))
            return "📈 Investment Return";

        if(merchant.contains("refund") || body.contains("refund"))
            return "↩️ Refund";

        //────────────────────────
        // LEARNED CATEGORY
        //────────────────────────

        String learned=getLearnedCategory(merchant);
        if(learned!=null) return learned;

        //────────────────────────
        // EXACT MERCHANT MATCH
        //────────────────────────

        if(MERCHANT_MAP.containsKey(merchant))
            return MERCHANT_MAP.get(merchant);

        //────────────────────────
        // PARTIAL MERCHANT MATCH
        //────────────────────────

        for(Map.Entry<String,String> e:MERCHANT_MAP.entrySet()){

            String key=e.getKey();

            if(merchant.contains(key) || key.contains(merchant))
                return e.getValue();
        }

        //────────────────────────
        // KEYWORD MATCH
        //────────────────────────

        String text=merchant+" "+body;

        for(Map.Entry<String,String[]> entry:KEYWORDS.entrySet()){

            for(String kw:entry.getValue()){

                if(text.contains(kw))
                    return entry.getKey();
            }
        }

        //────────────────────────
        // NLP FALLBACK
        //────────────────────────

        String nlp=NlpCategorizer.predict(merchant);

        if(!nlp.equals("💼 Others"))
            return nlp;

        return "💼 Others";
    }

    public static CategoryInfo getInfo(String category){

        CategoryInfo info=CATEGORIES.get(category);

        return info!=null?info:CATEGORIES.get("💼 Others");
    }

    public static String[] getCategoryNames(){

        return CATEGORIES.keySet().toArray(new String[0]);
    }
}
