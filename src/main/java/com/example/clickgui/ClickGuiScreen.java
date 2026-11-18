package com.example.clickgui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.IntConsumer;

public class ClickGuiScreen extends Screen {
    private static final int PAD = 8, TOP = 22, GAP = 6, PANEL_H = 230, ROW = 20, SUB = 16, MIN_W = 360, RADIUS = 10;
    private static final int GLASS = 0x99101620, BORDER = 0x26FFFFFF, TINT = 0x40000000,
            WHITE = 0xFFFFFFFF, SOFT = 0xD9FFFFFF, DARK = 0xFF1A1D24, GRAY = 0xFFB4BECB;
    private static final Identifier BG = Identifier.of("clickgui", "textures/gui/background.png");
    private static final int BG_W = 1600, BG_H = 900;
    private static final StyleSpriteSource FONT = new StyleSpriteSource.Font(Identifier.of("clickgui", "main"));
    private static final String[] ICONS = {"\u25C9", "\u25A3", "\u2316", "\u2600", "\u273F"};

    private static Category current = Category.VISUALS;
    private static boolean russian = false;

    private record Hit(int x, int y, int w, int h, IntConsumer action) {
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private double scroll, targetScroll, clickX;
    private int W = MIN_W, IW = MIN_W - PAD * 2, contentH, clipTop, clipBottom;
    private Setting.Slider dragSlider; private int sliderX, sliderW;
    private Module binding;

    public ClickGuiScreen() { super(Text.literal("ClickGUI")); }

    private Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    private int tw(String s) { return textRenderer.getWidth(t(s)); }
    private void txt(DrawContext c, String s, int x, int y, int col) { c.drawText(textRenderer, t(s), x, y, col, false); }
    private void ctxt(DrawContext c, String s, int cx, int y, int col) { txt(c, s, cx - tw(s) / 2, y, col); }
    private String tabLabel(Category c) { return ICONS[c.ordinal()] + "  " + c.title(russian); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta); // обычный размытый мир вне меню
        hits.clear();
        int need = TOP + 4;
        for (Category c : Category.values()) need += tw(tabLabel(c)) + 20 + 4;
        W = Math.max(MIN_W, need + 12 + 56 + PAD * 2);
        IW = W - PAD * 2;
        int H = PAD + TOP + GAP + PANEL_H + PAD;
        int x = (width - W) / 2, y = (height - H) / 2;

        // картинка только внутри меню, со скруглёнными углами
        roundRect(ctx, x - 1, y - 1, W + 2, H + 2, RADIUS + 1, BORDER);
        roundTexture(ctx, BG, x, y, W, H, RADIUS);
        roundRect(ctx, x, y, W, H, RADIUS, TINT);

        clipTop = 0; clipBottom = height;
        renderTopBar(ctx, x + PAD, y + PAD);
        renderPanel(ctx, x + PAD, y + PAD + TOP + GAP, mx, my);
    }

    private void renderTopBar(DrawContext ctx, int x, int y) {
        glass(ctx, x, y, TOP, TOP, 5);
        ctxt(ctx, "\u2726", x + TOP / 2, y + 7, WHITE);
        int cx = x + TOP + 4;
        for (Category c : Category.values()) {
            String s = tabLabel(c);
            int w = tw(s) + 20;
            boolean on = c == current;
            if (on) roundRect(ctx, cx, y, w, TOP, 5, Theme.accent()); else glass(ctx, cx, y, w, TOP, 5);
            txt(ctx, s, cx + 10, y + 7, on ? DARK : WHITE);
            hit(cx, y, w, TOP, b -> { current = c; targetScroll = scroll = 0; });
            cx += w + 4;
        }
        int lw = 28, lx = x + IW - lw * 2;
        glass(ctx, lx, y, lw * 2, TOP, 5);
        roundRect(ctx, russian ? lx + 2 : lx + lw, y + 2, lw - 2, TOP - 4, 4, Theme.accent());
        ctxt(ctx, "RUS", lx + lw / 2 + 1, y + 7, russian ? DARK : WHITE);
        ctxt(ctx, "ENG", lx + lw + lw / 2, y + 7, russian ? WHITE : DARK);
        hit(lx, y, lw * 2, TOP, b -> russian = !russian);
    }

    private void renderPanel(DrawContext ctx, int x, int y, int mx, int my) {
        glass(ctx, x, y, IW, PANEL_H, 8);
        scroll += (targetScroll - scroll) * 0.3;
        clipTop = y + 4; clipBottom = y + PANEL_H - 4;
        ctx.enableScissor(x, clipTop, x + IW, clipBottom);
        int start = y + 6 - (int) scroll, cy = start;
        // ---- кнопка редактора HUD во вкладке HUD ----
        if (current == Category.HUD) {
            int bx = x + 6, bw = IW - 14, bh = ROW - 2;
            boolean hv = inside(mx, my, bx, cy, bw, bh);
            if (hv) roundRect(ctx, bx, cy, bw, bh, 5, Theme.accent()); else glass(ctx, bx, cy, bw, bh, 5);
            ctxt(ctx, russian ? "Редактировать HUD" : "Edit HUD", bx + bw / 2, cy + 5, hv ? DARK : WHITE);
            hit(bx, cy, bw, bh, b -> client.setScreen(new HudEditorScreen()));
            cy += ROW + 4;
        }
        for (Module m : ModuleManager.byCategory(current)) cy = renderModule(ctx, m, x + 6, cy, IW - 14, mx, my);
        ctx.disableScissor();
        contentH = cy - start + 6;
        double maxScroll = Math.max(0, contentH - PANEL_H);
        if (targetScroll > maxScroll) targetScroll = maxScroll;
        if (scroll > maxScroll) scroll = maxScroll;
        if (contentH > PANEL_H) {
            int barH = Math.max(20, PANEL_H * PANEL_H / contentH);
            int barY = y + 4 + (int) ((PANEL_H - 8 - barH) * (scroll / (contentH - PANEL_H)));
            roundRect(ctx, x + IW - 4, barY, 2, barH, 1, 0xB3FFFFFF);
        }
    }

    private int renderModule(DrawContext ctx, Module m, int x, int y, int w, int mx, int my) {
        m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.25f;
        boolean hover = inside(mx, my, x, y, w, ROW);
        if (m.enabled || m.expanded || hover)
            ctx.fillGradient(x, y, x + w, y + ROW, hover ? 0x22FFFFFF : 0x16FFFFFF, hover ? 0x14FFFFFF : 0x0AFFFFFF);
        txt(ctx, m.name, x + 10, y + 6, m.enabled ? WHITE : SOFT);
        int sx = x + w - 28;
        drawSwitch(ctx, sx, y + 5, m.anim);
        drawGear(ctx, sx - 14, y + 6, m.expanded ? WHITE : GRAY);
        hit(sx - 16, y, 12, ROW, b -> m.expanded = !m.expanded);
        hit(sx - 2, y, 26, ROW, b -> m.toggle());
        hit(x, y, w, ROW, b -> { if (b == 1) m.expanded = !m.expanded; else m.toggle(); });
        y += ROW;
        if (m.expanded) {
            for (Setting s : m.settings) y = renderSetting(ctx, s, x + 10, y, w - 10);
            String key = binding == m ? "..." : m.key < 0 ? "-" :
                    InputUtil.Type.KEYSYM.createFromCode(m.key).getLocalizedText().getString();
            txt(ctx, "Keybind", x + 18, y + 4, GRAY);
            txt(ctx, key, x + w - 12 - tw(key), y + 4, WHITE);
            hit(x, y, w, SUB, b -> binding = m);
            y += SUB + 2;
        }
        return y + 2;
    }

    private int renderSetting(DrawContext ctx, Setting s, int x, int y, int w) {
        if (s instanceof Setting.Bool b) {
            b.anim += ((b.value ? 1f : 0f) - b.anim) * 0.25f;
            txt(ctx, b.name, x + 8, y + 4, GRAY);
            drawSwitch(ctx, x + w - 28, y + 3, b.anim);
            hit(x, y, w, SUB, k -> b.value = !b.value);
            return y + SUB;
        }
        if (s instanceof Setting.Slider sl) {
            txt(ctx, sl.name, x + 8, y + 2, GRAY);
            String fmt = sl.step >= 1 ? "%.0f" : (sl.step < 0.1 ? "%.2f" : "%.1f");
            txt(ctx, String.format(Locale.ROOT, fmt, sl.value), x + 12 + tw(sl.name), y + 2, WHITE);
            int lx = x + 8, lw = w - 20, ly = y + 14;
            roundRect(ctx, lx, ly, lw, 2, 1, 0x33FFFFFF);
            roundRect(ctx, lx, ly, Math.max(2, (int) (lw * sl.percent())), 2, 1, Theme.accent());
            hit(lx, y, lw, SUB + 4, k -> {
                dragSlider = sl; sliderX = lx; sliderW = lw;
                sl.setFromPercent((clickX - lx) / lw);
            });
            return y + SUB + 6;
        }
        if (s instanceof Setting.Multi mu) {
            txt(ctx, mu.name, x + 8, y + 2, GRAY);
            txt(ctx, mu.selected.size() + "/" + mu.options.size(), x + 12 + tw(mu.name), y + 2, WHITE);
            int cx = x + 8, cy = y + 13;
            for (String o : mu.options) {
                int cw = tw(o) + 12;
                if (cx + cw > x + w - 4) { cx = x + 8; cy += 16; }
                boolean on = mu.selected.contains(o);
                if (on) roundRect(ctx, cx, cy, cw, 13, 4, Theme.accent()); else glass(ctx, cx, cy, cw, 13, 4);
                txt(ctx, o, cx + 6, cy + 3, on ? DARK : WHITE);
                hit(cx, cy, cw, 13, k -> mu.toggle(o));
                cx += cw + 4;
            }
            return cy + 19;
        }
        if (s instanceof Setting.Mode md) {
            txt(ctx, md.name, x + 8, y + 2, GRAY);
            txt(ctx, md.get(), x + 12 + tw(md.name), y + 2, WHITE);
            int cx = x + 8, cy = y + 13;
            for (int i = 0; i < md.options.size(); i++) {
                String o = md.options.get(i);
                int cw = tw(o) + 12;
                if (cx + cw > x + w - 4) { cx = x + 8; cy += 16; }
                boolean on = md.index == i;
                int idx = i;
                if (on) roundRect(ctx, cx, cy, cw, 13, 4, Theme.accent()); else glass(ctx, cx, cy, cw, 13, 4);
                txt(ctx, o, cx + 6, cy + 3, on ? DARK : WHITE);
                hit(cx, cy, cw, 13, k -> md.index = idx);
                cx += cw + 4;
            }
            return cy + 19;
        }
        return y;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        clickX = click.x();
        for (Hit h : hits) if (h.in(click.x(), click.y())) { h.action.accept(click.button()); return true; }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragSlider != null) { dragSlider.setFromPercent((click.x() - sliderX) / sliderW); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) { dragSlider = null; return super.mouseReleased(click); }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        targetScroll = Math.max(0, Math.min(Math.max(0, contentH - PANEL_H), targetScroll - v * 20));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (binding != null) {
            int k = input.key();
            binding.key = (k == GLFW.GLFW_KEY_ESCAPE || k == GLFW.GLFW_KEY_DELETE) ? -1 : k;
            binding = null;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override public boolean shouldPause() { return false; }

    private void hit(int x, int y, int w, int h, IntConsumer a) {
        int top = Math.max(y, clipTop), bottom = Math.min(y + h, clipBottom);
        if (bottom > top) hits.add(new Hit(x, top, w, bottom - top, a));
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void glass(DrawContext c, int x, int y, int w, int h, int r) {
        roundRect(c, x - 1, y - 1, w + 2, h + 2, r + 1, BORDER);
        roundRect(c, x, y, w, h, r, GLASS);
    }

    private void drawSwitch(DrawContext c, int x, int y, float t) {
        int acc = Theme.accent();
        roundRect(c, x - 1, y - 1, 22, 12, 6, lerp(0x40FFFFFF, acc, t));
        roundRect(c, x, y, 20, 10, 5, lerp(0xFF20283A, acc, t));
        roundRect(c, x + 1 + (int) (t * 10), y + 1, 8, 8, 4, lerp(GRAY, WHITE, t));
    }

    private void drawGear(DrawContext c, int x, int y, int col) {
        c.fill(x + 3, y, x + 5, y + 8, col);
        c.fill(x, y + 3, x + 8, y + 5, col);
        c.fill(x + 1, y + 1, x + 7, y + 7, col);
        c.fill(x + 3, y + 3, x + 5, y + 5, 0xFF20283A);
    }

    /** Картинка в скруглённом прямоугольнике, обрезка по размеру (cover). */
    private void roundTexture(DrawContext c, Identifier id, int x, int y, int w, int h, int r) {
        float sa = (float) w / h, ta = (float) BG_W / BG_H;
        float rw = BG_W, rh = BG_H, u0 = 0, v0 = 0;
        if (ta > sa) { rw = BG_H * sa; u0 = (BG_W - rw) / 2f; } else { rh = BG_W / sa; v0 = (BG_H - rh) / 2f; }
        float kx = rw / w, ky = rh / h;
        blit(c, id, x, y + r, 0, r, w, h - 2 * r, u0, v0, kx, ky);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            blit(c, id, x + dx, y + i, dx, i, w - 2 * dx, 1, u0, v0, kx, ky);
            blit(c, id, x + dx, y + h - i - 1, dx, h - i - 1, w - 2 * dx, 1, u0, v0, kx, ky);
        }
    }

    private void blit(DrawContext c, Identifier id, int sx, int sy, int lx, int ly, int w, int h,
                      float u0, float v0, float kx, float ky) {
        if (w <= 0 || h <= 0) return;
        c.drawTexture(RenderPipelines.GUI_TEXTURED, id, sx, sy, u0 + lx * kx, v0 + ly * ky, w, h,
                Math.max(1, Math.round(w * kx)), Math.max(1, Math.round(h * ky)), BG_W, BG_H);
    }

    static void roundRect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        r = Math.min(r, Math.min(w, h) / 2);
        c.fill(x + r, y, x + w - r, y + h, col);
        c.fill(x, y + r, x + r, y + h - r, col);
        c.fill(x + w - r, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int dx = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + dx, y + i, x + r, y + i + 1, col);
            c.fill(x + w - r, y + i, x + w - dx, y + i + 1, col);
            c.fill(x + dx, y + h - i - 1, x + r, y + h - i, col);
            c.fill(x + w - r, y + h - i - 1, x + w - dx, y + h - i, col);
        }
    }

    static int lerp(int a, int b, float t) {
        int r = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
            r |= ((int) (ca + (cb - ca) * t) & 0xFF) << s;
        }
        return r;
    }
}
