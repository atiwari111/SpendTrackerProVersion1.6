package com.spendtracker.pro;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.*;

/**
 * MerchantLogoProvider v1.5
 * - Loads merchant → logo filename from assets/merchant_logo_map.json
 * - getLogo(merchant): returns logo asset filename if mapped, null otherwise
 * - getLogoDrawable(merchant): returns a colored-initial avatar as fallback
 *   when no real PNG exists (avoids crashes from missing drawables)
 */
public class MerchantLogoProvider {

    private static final Map<String, String> logoMap = new HashMap<>();
    private static boolean loaded = false;

    /** Call once from MainActivity or Application onCreate */
    public static void load(Context context) {
        if (loaded) return;
        try {
            InputStream is = context.getAssets().open("merchant_logo_map.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            JSONObject obj = new JSONObject(new String(buffer, "UTF-8"));
            Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                logoMap.put(key.toLowerCase(), obj.getString(key));
            }
            loaded = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the PNG asset filename for a merchant, or null if not mapped.
     * Use this if you want to load actual PNG logos from assets.
     */
    public static String getLogo(String merchant) {
        if (merchant == null) return null;
        String m = merchant.toLowerCase();
        for (String key : logoMap.keySet()) {
            if (m.contains(key)) return logoMap.get(key);
        }
        return null;
    }

    /**
     * Returns a ColorDrawable circle with the merchant's first initial.
     * Useful as a reliable fallback when real PNG logos are not bundled.
     *
     * Color is deterministically picked from the merchant name so the same
     * merchant always gets the same color across sessions.
     */
    public static Drawable getInitialDrawable(String merchant) {
        String display = (merchant != null && !merchant.isEmpty())
                ? String.valueOf(merchant.charAt(0)).toUpperCase()
                : "?";

        int color = pickColor(merchant);

        // Build a 44dp circle with white initial text
        int size = 88; // pixels (44dp @ 2x)
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Circle background
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bg);

        // Initial letter
        Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        txt.setColor(Color.WHITE);
        txt.setTextSize(size * 0.44f);
        txt.setTypeface(Typeface.DEFAULT_BOLD);
        txt.setTextAlign(Paint.Align.CENTER);
        float yPos = size / 2f - ((txt.descent() + txt.ascent()) / 2f);
        canvas.drawText(display, size / 2f, yPos, txt);

        return new BitmapDrawable(null, bmp);
    }

    // Deterministic color from merchant name (cycles through pleasant palette)
    private static final int[] PALETTE = {
        0xFF6366F1, 0xFF8B5CF6, 0xFFEC4899, 0xFF14B8A6,
        0xFFF59E0B, 0xFF10B981, 0xFFEF4444, 0xFF3B82F6,
        0xFFD97706, 0xFF059669, 0xFF7C3AED, 0xFFDB2777
    };

    private static int pickColor(String merchant) {
        if (merchant == null || merchant.isEmpty()) return PALETTE[0];
        int hash = 0;
        for (char c : merchant.toCharArray()) hash = hash * 31 + c;
        return PALETTE[Math.abs(hash) % PALETTE.length];
    }
}
