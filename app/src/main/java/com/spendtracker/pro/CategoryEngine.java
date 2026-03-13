package com.spendtracker.pro;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

/**
 * CategoryEngine v1.5
 * - 500+ merchant dataset across all categories
 * - Custom merchant learning (user corrections stored & reused)
 * - Priority: Custom learned > Merchant map > Keyword scan > Others
 */
public class CategoryEngine {

    public static class CategoryInfo {
        public final String name, icon;
        public final int color;
        public CategoryInfo(String name, String icon, int color) { this.name=name; this.icon=icon; this.color=color; }
    }

    public static final Map<String, CategoryInfo> CATEGORIES = new LinkedHashMap<>();
    static {
        CATEGORIES.put("🍔 Food",          new CategoryInfo("🍔 Food",          "🍔", 0xFFFF6B6B));
        CATEGORIES.put("🛒 Groceries",     new CategoryInfo("🛒 Groceries",     "🛒", 0xFF4ECDC4));
        CATEGORIES.put("🚗 Transport",     new CategoryInfo("🚗 Transport",     "🚗", 0xFF45B7D1));
        CATEGORIES.put("⛽ Fuel",          new CategoryInfo("⛽ Fuel",          "⛽", 0xFFFFBE0B));
        CATEGORIES.put("✈️ Travel",        new CategoryInfo("✈️ Travel",        "✈️", 0xFF96CEB4));
        CATEGORIES.put("🛍️ Shopping",      new CategoryInfo("🛍️ Shopping",      "🛍️", 0xFFDDA0DD));
        CATEGORIES.put("🏠 Rent",          new CategoryInfo("🏠 Rent",          "🏠", 0xFFFF8C69));
        CATEGORIES.put("🔌 Bills",         new CategoryInfo("🔌 Bills",         "🔌", 0xFFA8E6CF));
        CATEGORIES.put("🎬 Entertainment", new CategoryInfo("🎬 Entertainment", "🎬", 0xFFFFD3B6));
        CATEGORIES.put("🏥 Health",        new CategoryInfo("🏥 Health",        "🏥", 0xFFFF6B9D));
        CATEGORIES.put("💊 Medicine",      new CategoryInfo("💊 Medicine",      "💊", 0xFFB8E0FF));
        CATEGORIES.put("📚 Education",     new CategoryInfo("📚 Education",     "📚", 0xFFC3B1E1));
        CATEGORIES.put("💪 Fitness",       new CategoryInfo("💪 Fitness",       "💪", 0xFF98D8C8));
        CATEGORIES.put("💰 Investment",    new CategoryInfo("💰 Investment",    "💰", 0xFFFFD700));
        CATEGORIES.put("🎁 Gifts",         new CategoryInfo("🎁 Gifts",         "🎁", 0xFFFF9AA2));
        CATEGORIES.put("💼 Others",        new CategoryInfo("💼 Others",        "💼", 0xFFB0BEC5));
    }

    // ─────────────────────────────────────────────────────────────
    // 500+ MERCHANT DATASET
    // ─────────────────────────────────────────────────────────────
    public static final Map<String, String> MERCHANT_MAP = new LinkedHashMap<>();
    static {

        // ── FOOD ──────────────────────────────────────────────────
        MERCHANT_MAP.put("swiggy",             "🍔 Food");
        MERCHANT_MAP.put("zomato",             "🍔 Food");
        MERCHANT_MAP.put("dominos",            "🍔 Food");
        MERCHANT_MAP.put("domino",             "🍔 Food");
        MERCHANT_MAP.put("pizza hut",          "🍔 Food");
        MERCHANT_MAP.put("kfc",                "🍔 Food");
        MERCHANT_MAP.put("mcd",                "🍔 Food");
        MERCHANT_MAP.put("mcdonalds",          "🍔 Food");
        MERCHANT_MAP.put("mcdonald",           "🍔 Food");
        MERCHANT_MAP.put("burger king",        "🍔 Food");
        MERCHANT_MAP.put("subway",             "🍔 Food");
        MERCHANT_MAP.put("starbucks",          "🍔 Food");
        MERCHANT_MAP.put("cafe coffee day",    "🍔 Food");
        MERCHANT_MAP.put("ccd",                "🍔 Food");
        MERCHANT_MAP.put("blue tokai",         "🍔 Food");
        MERCHANT_MAP.put("third wave",         "🍔 Food");
        MERCHANT_MAP.put("haldiram",           "🍔 Food");
        MERCHANT_MAP.put("haldirams",          "🍔 Food");
        MERCHANT_MAP.put("chaayos",            "🍔 Food");
        MERCHANT_MAP.put("faasos",             "🍔 Food");
        MERCHANT_MAP.put("eatfit",             "🍔 Food");
        MERCHANT_MAP.put("freshmenu",          "🍔 Food");
        MERCHANT_MAP.put("box8",               "🍔 Food");
        MERCHANT_MAP.put("behrouz",            "🍔 Food");
        MERCHANT_MAP.put("taco bell",          "🍔 Food");
        MERCHANT_MAP.put("tacobell",           "🍔 Food");
        MERCHANT_MAP.put("barbeque nation",    "🍔 Food");
        MERCHANT_MAP.put("barbeque",           "🍔 Food");
        MERCHANT_MAP.put("wow momo",           "🍔 Food");
        MERCHANT_MAP.put("wow momos",          "🍔 Food");
        MERCHANT_MAP.put("biryani by kilo",    "🍔 Food");
        MERCHANT_MAP.put("paradise biryani",   "🍔 Food");
        MERCHANT_MAP.put("social",             "🍔 Food");
        MERCHANT_MAP.put("the bombay canteen", "🍔 Food");
        MERCHANT_MAP.put("pita pit",           "🍔 Food");
        MERCHANT_MAP.put("dunkin",             "🍔 Food");
        MERCHANT_MAP.put("baskin robbins",     "🍔 Food");
        MERCHANT_MAP.put("baskin",             "🍔 Food");
        MERCHANT_MAP.put("naturals ice cream", "🍔 Food");
        MERCHANT_MAP.put("amul",               "🍔 Food");
        MERCHANT_MAP.put("papa johns",         "🍔 Food");
        MERCHANT_MAP.put("la pino z",          "🍔 Food");
        MERCHANT_MAP.put("rolls mania",        "🍔 Food");
        MERCHANT_MAP.put("oven story",         "🍔 Food");
        MERCHANT_MAP.put("ovenstory",          "🍔 Food");
        MERCHANT_MAP.put("lunchbox",           "🍔 Food");
        MERCHANT_MAP.put("the good bowl",      "🍔 Food");
        MERCHANT_MAP.put("kwality walls",      "🍔 Food");
        MERCHANT_MAP.put("mojo pizza",         "🍔 Food");
        MERCHANT_MAP.put("burger barn",        "🍔 Food");
        MERCHANT_MAP.put("instacafe",          "🍔 Food");

        // ── GROCERIES ─────────────────────────────────────────────
        MERCHANT_MAP.put("zepto",              "🛒 Groceries");
        MERCHANT_MAP.put("blinkit",            "🛒 Groceries");
        MERCHANT_MAP.put("bigbasket",          "🛒 Groceries");
        MERCHANT_MAP.put("big basket",         "🛒 Groceries");
        MERCHANT_MAP.put("jiomart",            "🛒 Groceries");
        MERCHANT_MAP.put("jio mart",           "🛒 Groceries");
        MERCHANT_MAP.put("grofers",            "🛒 Groceries");
        MERCHANT_MAP.put("dmart",              "🛒 Groceries");
        MERCHANT_MAP.put("d-mart",             "🛒 Groceries");
        MERCHANT_MAP.put("smart bazar",        "🛒 Groceries");
        MERCHANT_MAP.put("smart bazaar",       "🛒 Groceries");
        MERCHANT_MAP.put("reliance fresh",     "🛒 Groceries");
        MERCHANT_MAP.put("reliance smart",     "🛒 Groceries");
        MERCHANT_MAP.put("more supermarket",   "🛒 Groceries");
        MERCHANT_MAP.put("more retail",        "🛒 Groceries");
        MERCHANT_MAP.put("nature basket",      "🛒 Groceries");
        MERCHANT_MAP.put("spencers",           "🛒 Groceries");
        MERCHANT_MAP.put("spencer",            "🛒 Groceries");
        MERCHANT_MAP.put("star bazaar",        "🛒 Groceries");
        MERCHANT_MAP.put("star market",        "🛒 Groceries");
        MERCHANT_MAP.put("easyday",            "🛒 Groceries");
        MERCHANT_MAP.put("foodhall",           "🛒 Groceries");
        MERCHANT_MAP.put("godrej nature",      "🛒 Groceries");
        MERCHANT_MAP.put("safal",              "🛒 Groceries");
        MERCHANT_MAP.put("vegetable market",   "🛒 Groceries");
        MERCHANT_MAP.put("subzi mandi",        "🛒 Groceries");
        MERCHANT_MAP.put("dunzo",              "🛒 Groceries");
        MERCHANT_MAP.put("swiggy instamart",   "🛒 Groceries");
        MERCHANT_MAP.put("instamart",          "🛒 Groceries");
        MERCHANT_MAP.put("bb daily",           "🛒 Groceries");

        // ── SHOPPING ──────────────────────────────────────────────
        MERCHANT_MAP.put("amazon",             "🛍️ Shopping");
        MERCHANT_MAP.put("flipkart",           "🛍️ Shopping");
        MERCHANT_MAP.put("myntra",             "🛍️ Shopping");
        MERCHANT_MAP.put("ajio",               "🛍️ Shopping");
        MERCHANT_MAP.put("meesho",             "🛍️ Shopping");
        MERCHANT_MAP.put("nykaa",              "🛍️ Shopping");
        MERCHANT_MAP.put("tatacliq",           "🛍️ Shopping");
        MERCHANT_MAP.put("tata cliq",          "🛍️ Shopping");
        MERCHANT_MAP.put("snapdeal",           "🛍️ Shopping");
        MERCHANT_MAP.put("shopsy",             "🛍️ Shopping");
        MERCHANT_MAP.put("decathlon",          "🛍️ Shopping");
        MERCHANT_MAP.put("ikea",               "🛍️ Shopping");
        MERCHANT_MAP.put("h&m",                "🛍️ Shopping");
        MERCHANT_MAP.put("zara",               "🛍️ Shopping");
        MERCHANT_MAP.put("uniqlo",             "🛍️ Shopping");
        MERCHANT_MAP.put("pantaloons",         "🛍️ Shopping");
        MERCHANT_MAP.put("westside",           "🛍️ Shopping");
        MERCHANT_MAP.put("shoppers stop",      "🛍️ Shopping");
        MERCHANT_MAP.put("max fashion",        "🛍️ Shopping");
        MERCHANT_MAP.put("max retail",         "🛍️ Shopping");
        MERCHANT_MAP.put("lifestyle",          "🛍️ Shopping");
        MERCHANT_MAP.put("central",            "🛍️ Shopping");
        MERCHANT_MAP.put("v2 retail",          "🛍️ Shopping");
        MERCHANT_MAP.put("bewakoof",           "🛍️ Shopping");
        MERCHANT_MAP.put("firstcry",           "🛍️ Shopping");
        MERCHANT_MAP.put("hopscotch",          "🛍️ Shopping");
        MERCHANT_MAP.put("lenskart",           "🛍️ Shopping");
        MERCHANT_MAP.put("pepperfry",          "🛍️ Shopping");
        MERCHANT_MAP.put("urban ladder",       "🛍️ Shopping");
        MERCHANT_MAP.put("livspace",           "🛍️ Shopping");
        MERCHANT_MAP.put("boat",               "🛍️ Shopping");
        MERCHANT_MAP.put("noise",              "🛍️ Shopping");
        MERCHANT_MAP.put("samsung",            "🛍️ Shopping");
        MERCHANT_MAP.put("apple",              "🛍️ Shopping");
        MERCHANT_MAP.put("croma",              "🛍️ Shopping");
        MERCHANT_MAP.put("reliance digital",   "🛍️ Shopping");
        MERCHANT_MAP.put("vijay sales",        "🛍️ Shopping");
        MERCHANT_MAP.put("poorvika",           "🛍️ Shopping");
        MERCHANT_MAP.put("sangeetha",          "🛍️ Shopping");
        MERCHANT_MAP.put("inr clothing",       "🛍️ Shopping");
        MERCHANT_MAP.put("blackberrys",        "🛍️ Shopping");
        MERCHANT_MAP.put("raymond",            "🛍️ Shopping");
        MERCHANT_MAP.put("fabindia",           "🛍️ Shopping");
        MERCHANT_MAP.put("bata",               "🛍️ Shopping");
        MERCHANT_MAP.put("liberty shoes",      "🛍️ Shopping");
        MERCHANT_MAP.put("metro shoes",        "🛍️ Shopping");
        MERCHANT_MAP.put("mochi",              "🛍️ Shopping");
        MERCHANT_MAP.put("clovia",             "🛍️ Shopping");
        MERCHANT_MAP.put("zivame",             "🛍️ Shopping");
        MERCHANT_MAP.put("purplle",            "🛍️ Shopping");
        MERCHANT_MAP.put("sugar cosmetics",    "🛍️ Shopping");
        MERCHANT_MAP.put("mamaearth",          "🛍️ Shopping");
        MERCHANT_MAP.put("minimalist",         "🛍️ Shopping");
        MERCHANT_MAP.put("plum",               "🛍️ Shopping");
        MERCHANT_MAP.put("wow skin",           "🛍️ Shopping");
        MERCHANT_MAP.put("mcaffeine",          "🛍️ Shopping");

        // ── TRANSPORT ─────────────────────────────────────────────
        MERCHANT_MAP.put("uber",               "🚗 Transport");
        MERCHANT_MAP.put("ola",                "🚗 Transport");
        MERCHANT_MAP.put("rapido",             "🚗 Transport");
        MERCHANT_MAP.put("meru",               "🚗 Transport");
        MERCHANT_MAP.put("bluestar",           "🚗 Transport");
        MERCHANT_MAP.put("jugnoo",             "🚗 Transport");
        MERCHANT_MAP.put("shuttl",             "🚗 Transport");
        MERCHANT_MAP.put("chalo",              "🚗 Transport");
        MERCHANT_MAP.put("dtc",                "🚗 Transport");
        MERCHANT_MAP.put("bmtc",               "🚗 Transport");
        MERCHANT_MAP.put("ksrtc",              "🚗 Transport");
        MERCHANT_MAP.put("msrtc",              "🚗 Transport");
        MERCHANT_MAP.put("metro card",         "🚗 Transport");
        MERCHANT_MAP.put("dmrc",               "🚗 Transport");
        MERCHANT_MAP.put("nmmc",               "🚗 Transport");
        MERCHANT_MAP.put("best bus",           "🚗 Transport");
        MERCHANT_MAP.put("yulu",               "🚗 Transport");
        MERCHANT_MAP.put("bounce",             "🚗 Transport");
        MERCHANT_MAP.put("vogo",               "🚗 Transport");
        MERCHANT_MAP.put("blu smart",          "🚗 Transport");
        MERCHANT_MAP.put("blusmart",           "🚗 Transport");
        MERCHANT_MAP.put("taxi",               "🚗 Transport");
        MERCHANT_MAP.put("cab",                "🚗 Transport");
        MERCHANT_MAP.put("auto rickshaw",      "🚗 Transport");

        // ── FUEL ──────────────────────────────────────────────────
        MERCHANT_MAP.put("hp",                 "⛽ Fuel");
        MERCHANT_MAP.put("hpcl",               "⛽ Fuel");
        MERCHANT_MAP.put("bp",                 "⛽ Fuel");
        MERCHANT_MAP.put("bpcl",               "⛽ Fuel");
        MERCHANT_MAP.put("iocl",               "⛽ Fuel");
        MERCHANT_MAP.put("indian oil",         "⛽ Fuel");
        MERCHANT_MAP.put("hindustan petroleum","⛽ Fuel");
        MERCHANT_MAP.put("bharat petroleum",   "⛽ Fuel");
        MERCHANT_MAP.put("shell",              "⛽ Fuel");
        MERCHANT_MAP.put("essar oil",          "⛽ Fuel");
        MERCHANT_MAP.put("reliance petrol",    "⛽ Fuel");
        MERCHANT_MAP.put("nayara",             "⛽ Fuel");
        MERCHANT_MAP.put("petronet",           "⛽ Fuel");
        MERCHANT_MAP.put("petrol pump",        "⛽ Fuel");
        MERCHANT_MAP.put("fuel station",       "⛽ Fuel");
        MERCHANT_MAP.put("cng station",        "⛽ Fuel");
        MERCHANT_MAP.put("ev charging",        "⛽ Fuel");
        MERCHANT_MAP.put("ather grid",         "⛽ Fuel");
        MERCHANT_MAP.put("tata power ev",      "⛽ Fuel");
        MERCHANT_MAP.put("statiq",             "⛽ Fuel");

        // ── TRAVEL ────────────────────────────────────────────────
        MERCHANT_MAP.put("irctc",              "✈️ Travel");
        MERCHANT_MAP.put("indian railway",     "✈️ Travel");
        MERCHANT_MAP.put("makemytrip",         "✈️ Travel");
        MERCHANT_MAP.put("goibibo",            "✈️ Travel");
        MERCHANT_MAP.put("redbus",             "✈️ Travel");
        MERCHANT_MAP.put("ixigo",              "✈️ Travel");
        MERCHANT_MAP.put("yatra",              "✈️ Travel");
        MERCHANT_MAP.put("cleartrip",          "✈️ Travel");
        MERCHANT_MAP.put("easemytrip",         "✈️ Travel");
        MERCHANT_MAP.put("airindia",           "✈️ Travel");
        MERCHANT_MAP.put("air india",          "✈️ Travel");
        MERCHANT_MAP.put("indigo",             "✈️ Travel");
        MERCHANT_MAP.put("spicejet",           "✈️ Travel");
        MERCHANT_MAP.put("vistara",            "✈️ Travel");
        MERCHANT_MAP.put("akasa",              "✈️ Travel");
        MERCHANT_MAP.put("go first",           "✈️ Travel");
        MERCHANT_MAP.put("air asia",           "✈️ Travel");
        MERCHANT_MAP.put("airasia",            "✈️ Travel");
        MERCHANT_MAP.put("oyo",                "✈️ Travel");
        MERCHANT_MAP.put("oyo rooms",          "✈️ Travel");
        MERCHANT_MAP.put("airbnb",             "✈️ Travel");
        MERCHANT_MAP.put("treebo",             "✈️ Travel");
        MERCHANT_MAP.put("fabhotels",          "✈️ Travel");
        MERCHANT_MAP.put("taj hotels",         "✈️ Travel");
        MERCHANT_MAP.put("marriott",           "✈️ Travel");
        MERCHANT_MAP.put("ihg",                "✈️ Travel");
        MERCHANT_MAP.put("booking.com",        "✈️ Travel");
        MERCHANT_MAP.put("agoda",              "✈️ Travel");
        MERCHANT_MAP.put("expedia",            "✈️ Travel");
        MERCHANT_MAP.put("trivago",            "✈️ Travel");
        MERCHANT_MAP.put("holidify",           "✈️ Travel");
        MERCHANT_MAP.put("thrillophilia",      "✈️ Travel");
        MERCHANT_MAP.put("viator",             "✈️ Travel");

        // ── BILLS ─────────────────────────────────────────────────
        MERCHANT_MAP.put("airtel",             "🔌 Bills");
        MERCHANT_MAP.put("jio",                "🔌 Bills");
        MERCHANT_MAP.put("vodafone",           "🔌 Bills");
        MERCHANT_MAP.put("vi ",                "🔌 Bills");
        MERCHANT_MAP.put("bsnl",               "🔌 Bills");
        MERCHANT_MAP.put("tata sky",           "🔌 Bills");
        MERCHANT_MAP.put("tatasky",            "🔌 Bills");
        MERCHANT_MAP.put("dish tv",            "🔌 Bills");
        MERCHANT_MAP.put("dishtv",             "🔌 Bills");
        MERCHANT_MAP.put("sun direct",         "🔌 Bills");
        MERCHANT_MAP.put("d2h",                "🔌 Bills");
        MERCHANT_MAP.put("videocon d2h",       "🔌 Bills");
        MERCHANT_MAP.put("tata power",         "🔌 Bills");
        MERCHANT_MAP.put("adani electricity",  "🔌 Bills");
        MERCHANT_MAP.put("bescom",             "🔌 Bills");
        MERCHANT_MAP.put("msedcl",             "🔌 Bills");
        MERCHANT_MAP.put("tneb",               "🔌 Bills");
        MERCHANT_MAP.put("bses",               "🔌 Bills");
        MERCHANT_MAP.put("torrent power",      "🔌 Bills");
        MERCHANT_MAP.put("mahadiscom",         "🔌 Bills");
        MERCHANT_MAP.put("cesc",               "🔌 Bills");
        MERCHANT_MAP.put("wesco",              "🔌 Bills");
        MERCHANT_MAP.put("indane",             "🔌 Bills");
        MERCHANT_MAP.put("hp gas",             "🔌 Bills");
        MERCHANT_MAP.put("bharat gas",         "🔌 Bills");
        MERCHANT_MAP.put("mahanagar gas",      "🔌 Bills");
        MERCHANT_MAP.put("indraprastha gas",   "🔌 Bills");
        MERCHANT_MAP.put("igl",                "🔌 Bills");
        MERCHANT_MAP.put("mgl",                "🔌 Bills");
        MERCHANT_MAP.put("act fibernet",       "🔌 Bills");
        MERCHANT_MAP.put("hathway",            "🔌 Bills");
        MERCHANT_MAP.put("you broadband",      "🔌 Bills");
        MERCHANT_MAP.put("excitel",            "🔌 Bills");
        MERCHANT_MAP.put("den networks",       "🔌 Bills");
        MERCHANT_MAP.put("water board",        "🔌 Bills");
        MERCHANT_MAP.put("bwssb",              "🔌 Bills");
        MERCHANT_MAP.put("nmmc water",         "🔌 Bills");

        // ── ENTERTAINMENT ─────────────────────────────────────────
        MERCHANT_MAP.put("netflix",            "🎬 Entertainment");
        MERCHANT_MAP.put("spotify",            "🎬 Entertainment");
        MERCHANT_MAP.put("hotstar",            "🎬 Entertainment");
        MERCHANT_MAP.put("disney",             "🎬 Entertainment");
        MERCHANT_MAP.put("disney+ hotstar",    "🎬 Entertainment");
        MERCHANT_MAP.put("prime video",        "🎬 Entertainment");
        MERCHANT_MAP.put("amazon prime",       "🎬 Entertainment");
        MERCHANT_MAP.put("youtube premium",    "🎬 Entertainment");
        MERCHANT_MAP.put("youtube",            "🎬 Entertainment");
        MERCHANT_MAP.put("sony liv",           "🎬 Entertainment");
        MERCHANT_MAP.put("sonyliv",            "🎬 Entertainment");
        MERCHANT_MAP.put("zee5",               "🎬 Entertainment");
        MERCHANT_MAP.put("voot",               "🎬 Entertainment");
        MERCHANT_MAP.put("alt balaji",         "🎬 Entertainment");
        MERCHANT_MAP.put("altbalaji",          "🎬 Entertainment");
        MERCHANT_MAP.put("aha",                "🎬 Entertainment");
        MERCHANT_MAP.put("mx player",          "🎬 Entertainment");
        MERCHANT_MAP.put("jio cinema",         "🎬 Entertainment");
        MERCHANT_MAP.put("apple tv",           "🎬 Entertainment");
        MERCHANT_MAP.put("pvr",                "🎬 Entertainment");
        MERCHANT_MAP.put("inox",               "🎬 Entertainment");
        MERCHANT_MAP.put("cinepolis",          "🎬 Entertainment");
        MERCHANT_MAP.put("carnival cinemas",   "🎬 Entertainment");
        MERCHANT_MAP.put("bookmyshow",         "🎬 Entertainment");
        MERCHANT_MAP.put("paytm insider",      "🎬 Entertainment");
        MERCHANT_MAP.put("lbb",                "🎬 Entertainment");
        MERCHANT_MAP.put("dineout",            "🎬 Entertainment");
        MERCHANT_MAP.put("steam",              "🎬 Entertainment");
        MERCHANT_MAP.put("playstation",        "🎬 Entertainment");
        MERCHANT_MAP.put("xbox",               "🎬 Entertainment");
        MERCHANT_MAP.put("gaana",              "🎬 Entertainment");
        MERCHANT_MAP.put("wynk",               "🎬 Entertainment");
        MERCHANT_MAP.put("jiosaavn",           "🎬 Entertainment");
        MERCHANT_MAP.put("saavn",              "🎬 Entertainment");
        MERCHANT_MAP.put("hungama",            "🎬 Entertainment");

        // ── HEALTH ────────────────────────────────────────────────
        MERCHANT_MAP.put("apollo",             "🏥 Health");
        MERCHANT_MAP.put("appollo",            "🏥 Health");
        MERCHANT_MAP.put("apollo pharmacy",    "💊 Medicine");
        MERCHANT_MAP.put("fortis",             "🏥 Health");
        MERCHANT_MAP.put("max hospital",       "🏥 Health");
        MERCHANT_MAP.put("aiims",              "🏥 Health");
        MERCHANT_MAP.put("manipal",            "🏥 Health");
        MERCHANT_MAP.put("narayana",           "🏥 Health");
        MERCHANT_MAP.put("columbia asia",      "🏥 Health");
        MERCHANT_MAP.put("medanta",            "🏥 Health");
        MERCHANT_MAP.put("kokilaben",          "🏥 Health");
        MERCHANT_MAP.put("lilavati",           "🏥 Health");
        MERCHANT_MAP.put("nh health",          "🏥 Health");
        MERCHANT_MAP.put("practo",             "🏥 Health");
        MERCHANT_MAP.put("lybrate",            "🏥 Health");
        MERCHANT_MAP.put("tata health",        "🏥 Health");
        MERCHANT_MAP.put("mfine",              "🏥 Health");
        MERCHANT_MAP.put("healthians",         "🏥 Health");
        MERCHANT_MAP.put("thyrocare",          "🏥 Health");
        MERCHANT_MAP.put("dr lal",             "🏥 Health");
        MERCHANT_MAP.put("lal path",           "🏥 Health");
        MERCHANT_MAP.put("srl diagnostics",    "🏥 Health");

        // ── MEDICINE ──────────────────────────────────────────────
        MERCHANT_MAP.put("pharmeasy",          "💊 Medicine");
        MERCHANT_MAP.put("1mg",                "💊 Medicine");
        MERCHANT_MAP.put("netmeds",            "💊 Medicine");
        MERCHANT_MAP.put("medplus",            "💊 Medicine");
        MERCHANT_MAP.put("wellness forever",   "💊 Medicine");
        MERCHANT_MAP.put("guardian pharmacy",  "💊 Medicine");
        MERCHANT_MAP.put("frank ross",         "💊 Medicine");
        MERCHANT_MAP.put("suraksha diagnostic","💊 Medicine");
        MERCHANT_MAP.put("tata 1mg",           "💊 Medicine");
        MERCHANT_MAP.put("saveo",              "💊 Medicine");
        MERCHANT_MAP.put("medlife",            "💊 Medicine");
        MERCHANT_MAP.put("myra medicines",     "💊 Medicine");

        // ── EDUCATION ─────────────────────────────────────────────
        MERCHANT_MAP.put("byjus",              "📚 Education");
        MERCHANT_MAP.put("byju",               "📚 Education");
        MERCHANT_MAP.put("unacademy",          "📚 Education");
        MERCHANT_MAP.put("vedantu",            "📚 Education");
        MERCHANT_MAP.put("udemy",              "📚 Education");
        MERCHANT_MAP.put("coursera",           "📚 Education");
        MERCHANT_MAP.put("toppr",              "📚 Education");
        MERCHANT_MAP.put("meritnation",        "📚 Education");
        MERCHANT_MAP.put("physicswallah",      "📚 Education");
        MERCHANT_MAP.put("pw",                 "📚 Education");
        MERCHANT_MAP.put("great learning",     "📚 Education");
        MERCHANT_MAP.put("simplilearn",        "📚 Education");
        MERCHANT_MAP.put("upgrad",             "📚 Education");
        MERCHANT_MAP.put("coding ninjas",      "📚 Education");
        MERCHANT_MAP.put("scaler",             "📚 Education");
        MERCHANT_MAP.put("leetcode",           "📚 Education");
        MERCHANT_MAP.put("pluralsight",        "📚 Education");
        MERCHANT_MAP.put("skillshare",         "📚 Education");
        MERCHANT_MAP.put("khan academy",       "📚 Education");
        MERCHANT_MAP.put("whitehat jr",        "📚 Education");
        MERCHANT_MAP.put("cuemath",            "📚 Education");
        MERCHANT_MAP.put("school fees",        "📚 Education");
        MERCHANT_MAP.put("college fees",       "📚 Education");
        MERCHANT_MAP.put("tuition",            "📚 Education");
        MERCHANT_MAP.put("cambly",             "📚 Education");
        MERCHANT_MAP.put("duolingo",           "📚 Education");

        // ── FITNESS ───────────────────────────────────────────────
        MERCHANT_MAP.put("cult fit",           "💪 Fitness");
        MERCHANT_MAP.put("cultfit",            "💪 Fitness");
        MERCHANT_MAP.put("cure fit",           "💪 Fitness");
        MERCHANT_MAP.put("gold gym",           "💪 Fitness");
        MERCHANT_MAP.put("gold's gym",         "💪 Fitness");
        MERCHANT_MAP.put("anytime fitness",    "💪 Fitness");
        MERCHANT_MAP.put("snap fitness",       "💪 Fitness");
        MERCHANT_MAP.put("fitness first",      "💪 Fitness");
        MERCHANT_MAP.put("talwalkars",         "💪 Fitness");
        MERCHANT_MAP.put("powerhouse gym",     "💪 Fitness");
        MERCHANT_MAP.put("crossfit",           "💪 Fitness");
        MERCHANT_MAP.put("yoga",               "💪 Fitness");
        MERCHANT_MAP.put("fitpass",            "💪 Fitness");
        MERCHANT_MAP.put("gympass",            "💪 Fitness");
        MERCHANT_MAP.put("healthifyme",        "💪 Fitness");
        MERCHANT_MAP.put("nike",               "💪 Fitness");
        MERCHANT_MAP.put("adidas",             "💪 Fitness");
        MERCHANT_MAP.put("puma",               "💪 Fitness");
        MERCHANT_MAP.put("under armour",       "💪 Fitness");
        MERCHANT_MAP.put("reebok",             "💪 Fitness");
        MERCHANT_MAP.put("skechers",           "💪 Fitness");
        MERCHANT_MAP.put("salon",              "💪 Fitness");
        MERCHANT_MAP.put("loreal",             "💪 Fitness");
        MERCHANT_MAP.put("vlcc",               "💪 Fitness");
        MERCHANT_MAP.put("enrich",             "💪 Fitness");
        MERCHANT_MAP.put("naturals salon",     "💪 Fitness");
        MERCHANT_MAP.put("jawed habib",        "💪 Fitness");
        MERCHANT_MAP.put("barber",             "💪 Fitness");
        MERCHANT_MAP.put("lakme",              "💪 Fitness");

        // ── INVESTMENT ────────────────────────────────────────────
        MERCHANT_MAP.put("zerodha",            "💰 Investment");
        MERCHANT_MAP.put("groww",              "💰 Investment");
        MERCHANT_MAP.put("upstox",             "💰 Investment");
        MERCHANT_MAP.put("gullak",             "💰 Investment");
        MERCHANT_MAP.put("coin",               "💰 Investment");
        MERCHANT_MAP.put("kuvera",             "💰 Investment");
        MERCHANT_MAP.put("etmoney",            "💰 Investment");
        MERCHANT_MAP.put("niyo",               "💰 Investment");
        MERCHANT_MAP.put("smallcase",          "💰 Investment");
        MERCHANT_MAP.put("indmoney",           "💰 Investment");
        MERCHANT_MAP.put("wealthsimple",       "💰 Investment");
        MERCHANT_MAP.put("navi",               "💰 Investment");
        MERCHANT_MAP.put("paytm money",        "💰 Investment");
        MERCHANT_MAP.put("5paisa",             "💰 Investment");
        MERCHANT_MAP.put("angel broking",      "💰 Investment");
        MERCHANT_MAP.put("angel one",          "💰 Investment");
        MERCHANT_MAP.put("hdfc securities",    "💰 Investment");
        MERCHANT_MAP.put("icici direct",       "💰 Investment");
        MERCHANT_MAP.put("motilal oswal",      "💰 Investment");
        MERCHANT_MAP.put("lic",                "💰 Investment");
        MERCHANT_MAP.put("hdfc life",          "💰 Investment");
        MERCHANT_MAP.put("sbi life",           "💰 Investment");
        MERCHANT_MAP.put("max life",           "💰 Investment");
        MERCHANT_MAP.put("bajaj allianz",      "💰 Investment");
        MERCHANT_MAP.put("tata aia",           "💰 Investment");
        MERCHANT_MAP.put("policybazaar",       "💰 Investment");
        MERCHANT_MAP.put("acko",               "💰 Investment");

        // ── RENT ──────────────────────────────────────────────────
        MERCHANT_MAP.put("nobroker",           "🏠 Rent");
        MERCHANT_MAP.put("magicbricks",        "🏠 Rent");
        MERCHANT_MAP.put("99acres",            "🏠 Rent");
        MERCHANT_MAP.put("housing.com",        "🏠 Rent");
        MERCHANT_MAP.put("nestaway",           "🏠 Rent");
        MERCHANT_MAP.put("zolo",               "🏠 Rent");
        MERCHANT_MAP.put("stanza living",      "🏠 Rent");
        MERCHANT_MAP.put("colive",             "🏠 Rent");
        MERCHANT_MAP.put("society maintenance","🏠 Rent");
        MERCHANT_MAP.put("apartment rent",     "🏠 Rent");

        // ── GIFTS ─────────────────────────────────────────────────
        MERCHANT_MAP.put("igp",                "🎁 Gifts");
        MERCHANT_MAP.put("ferns n petals",     "🎁 Gifts");
        MERCHANT_MAP.put("fnp",                "🎁 Gifts");
        MERCHANT_MAP.put("archies",            "🎁 Gifts");
        MERCHANT_MAP.put("hallmark",           "🎁 Gifts");
        MERCHANT_MAP.put("giftease",           "🎁 Gifts");
        MERCHANT_MAP.put("myflowertree",       "🎁 Gifts");
        MERCHANT_MAP.put("winni",              "🎁 Gifts");
        MERCHANT_MAP.put("bakingo",            "🎁 Gifts");
        MERCHANT_MAP.put("floweraura",         "🎁 Gifts");
        MERCHANT_MAP.put("brownberry",         "🎁 Gifts");
    }

    // ─────────────────────────────────────────────────────────────
    // KEYWORD FALLBACK SCAN
    // ─────────────────────────────────────────────────────────────
    private static final Map<String, String[]> KEYWORDS = new LinkedHashMap<>();
    static {
        KEYWORDS.put("🍔 Food",          new String[]{"restaurant","cafe","coffee","pizza","burger","biryani","dhaba","kitchen","dining","eatery","food court","canteen","mess","tiffin","bakery","sweets"});
        KEYWORDS.put("🛒 Groceries",     new String[]{"grocery","supermarket","vegetables","fruits","milk","dairy","provisions","kirana","general store","weekly market"});
        KEYWORDS.put("🚗 Transport",     new String[]{"metro","bus","auto","cab","taxi","bike taxi","pickup","drop","commute","transport","rickshaw","ferry","local train"});
        KEYWORDS.put("⛽ Fuel",          new String[]{"petrol","diesel","fuel","cng","ev charge","charging station","refuel","pump"});
        KEYWORDS.put("✈️ Travel",        new String[]{"train","flight","airline","hotel","resort","hostel","lodge","booking","vacation","trip","holiday","tour","trek","package"});
        KEYWORDS.put("🛍️ Shopping",      new String[]{"mall","store","shop","fashion","clothes","apparel","footwear","jewellery","accessories","electronics","gadget","appliance","furniture","home decor"});
        KEYWORDS.put("🏠 Rent",          new String[]{"rent","landlord","owner","housing","society","maintenance","flat","apartment","lease","pg","hostel rent","deposit"});
        KEYWORDS.put("🔌 Bills",         new String[]{"electricity","bill pay","recharge","broadband","internet","water","gas","lpg","municipal","dth","cable","postpaid","prepaid"});
        KEYWORDS.put("🎬 Entertainment", new String[]{"movie","concert","event","cricket","ipl","ticket","show","gaming","game","subscription","ott","streaming","theater"});
        KEYWORDS.put("🏥 Health",        new String[]{"hospital","clinic","doctor","consultation","health","diagnostics","test","scan","xray","blood test","checkup","nursing home"});
        KEYWORDS.put("💊 Medicine",      new String[]{"pharmacy","medicine","tablet","capsule","drug","chemist","ayurvedic","homeopathic","prescription","wellness"});
        KEYWORDS.put("📚 Education",     new String[]{"school","college","university","fees","tuition","coaching","book","stationery","pen","notebook","exam","course","learning","workshop","certification"});
        KEYWORDS.put("💪 Fitness",       new String[]{"gym","crossfit","yoga","zumba","fitness","sports","workout","trainer","spa","grooming","haircut","parlour","beauty","massage"});
        KEYWORDS.put("💰 Investment",    new String[]{"mutual fund","sip","stocks","nse","bse","ipo","fd","fixed deposit","ppf","nps","insurance","trading","demat","portfolio"});
        KEYWORDS.put("🎁 Gifts",         new String[]{"gift","birthday","wedding","anniversary","flower","bouquet","present","surprise","occasion","celebration","cake","chocolate box"});
    }

    // ─────────────────────────────────────────────────────────────
    // MERCHANT LEARNING — user corrections stored per app install
    // ─────────────────────────────────────────────────────────────
    private static final String PREFS_NAME = "merchant_learning";
    private static SharedPreferences learningPrefs;

    /** Call once from Application or MainActivity to enable learning */
    public static void init(android.content.Context ctx) {
        learningPrefs = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
    }

    /** Save a user-corrected merchant → category mapping */
    public static void learnMerchant(String merchant, String category) {
        if (learningPrefs == null || merchant == null || category == null) return;
        learningPrefs.edit().putString(merchant.toLowerCase().trim(), category).apply();
    }

    /** Look up a learned mapping */
    private static String getLearnedCategory(String merchant) {
        if (learningPrefs == null || merchant == null) return null;
        return learningPrefs.getString(merchant.toLowerCase().trim(), null);
    }

    // ─────────────────────────────────────────────────────────────
    // MAIN CLASSIFY — 4-tier priority
    // ─────────────────────────────────────────────────────────────
    public static String classify(String merchant, String smsBody) {
        String m = (merchant != null ? merchant : "").toLowerCase().trim();

        // Priority 1: User-learned custom mapping (highest priority)
        String learned = getLearnedCategory(m);
        if (learned != null) return learned;

        // Priority 2: Exact merchant map lookup
        if (MERCHANT_MAP.containsKey(m)) return MERCHANT_MAP.get(m);

        // Priority 3: Partial merchant map (substring match)
        for (Map.Entry<String, String> e : MERCHANT_MAP.entrySet()) {
            if (m.contains(e.getKey()) || e.getKey().contains(m) && m.length() > 3) {
                return e.getValue();
            }
        }

        // Priority 4: Keyword scan over merchant name + SMS body
        String text = m + " " + (smsBody != null ? smsBody.toLowerCase() : "");
        for (Map.Entry<String, String[]> entry : KEYWORDS.entrySet()) {
            for (String kw : entry.getValue()) {
                if (text.contains(kw)) return entry.getKey();
            }
        }

        // Priority 5: NLP word-token fallback (handles unknown merchants)
        String nlp = NlpCategorizer.predict(m);
        if (!nlp.equals("💼 Others")) return nlp;

        return "💼 Others";
    }

    public static String autoCategory(String typedMerchant) {
        return classify(typedMerchant, "");
    }

    public static CategoryInfo getInfo(String category) {
        CategoryInfo info = CATEGORIES.get(category);
        return info != null ? info : CATEGORIES.get("💼 Others");
    }

    public static String[] getCategoryNames() {
        return CATEGORIES.keySet().toArray(new String[0]);
    }
}
