package com.example.clickgui;

import java.awt.Color;

/** Цветовые темы: акцент меню и цвет всех эффектов. */
public final class Theme {
    public static final String[] NAMES = {"Sakura", "Ocean", "Amethyst", "Inferno", "Emerald", "Custom"};
    private static final int[] HEX = {0xFF4FCB, 0x00BFFF, 0xA855F7, 0xFF453A, 0x39E75F};

    private static float[] hsb(long now) {
        Module m = ModuleManager.get("Theme");
        if (m.bool("Rainbow")) return new float[]{(now % 4000) / 4000f, 0.6f, 1f};
        int i = ((Setting.Mode) m.find("Theme")).index;
        if (i >= HEX.length) return new float[]{(float) m.num("Hue"), 0.6f, 1f};
        int c = HEX[i];
        return Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
    }

    /** Сплошной акцентный цвет для меню. */
    public static int accent() {
        float[] h = hsb(System.currentTimeMillis());
        return 0xFF000000 | (Color.HSBtoRGB(h[0], h[1], h[2]) & 0xFFFFFF);
    }

    /** Цвет для эффектов: offset 0..1 даёт лёгкий градиент вокруг темы. */
    public static int color(long now, float offset, int alpha) {
        float[] h = hsb(now);
        boolean rainbow = ModuleManager.get("Theme").bool("Rainbow");
        float hue = h[0] + offset * (rainbow ? 1f : 0.08f);
        float bri = Math.min(1f, h[2] + offset * 0.1f);
        int rgb = Color.HSBtoRGB(hue, h[1], bri) & 0xFFFFFF;
        return (Math.max(0, Math.min(255, alpha)) << 24) | rgb;
    }
}