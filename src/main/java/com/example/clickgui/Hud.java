package com.example.clickgui;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.*;
import java.time.LocalTime;
import java.util.*;

/** HUD: rendering of HUD-tab modules + movable positions (config/clickgui/hud.json). */
public final class Hud {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final StyleSpriteSource FONT = new StyleSpriteSource.Font(Identifier.of("clickgui", "main"));
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("clickgui/hud.json");
    private static final int BG = 0x99101620, WHITE = 0xFFFFFFFF, GRAY = 0xFFB4BECB, DARK = 0xFF1A1D24;

    /** Elements that can be moved. Name = module name in ModuleManager. */
    public static final String[] ELEMENTS = {
            "Watermark", "Coordinates", "FPS Counter", "Armor HUD", "Keystrokes", "Armor Notifier",
            "Target HUD", "Cooldowns HUD", "Potion HUD", "ArrayList", "Totem Counter", "Armor HUD Pro",
            "Ping & TPS", "Speedometer", "Kill & Combo", "Inventory Viewer", "Compass Bar",
            "Biome Badge", "Staff & Friends", "Music Player", "Pickup Feed", "Notifications"
    };
    private static final float[][] DEFAULTS = {
            {0.01f, 0.01f}, {0.01f, 0.08f}, {0.01f, 0.14f}, {0.5f, 0.86f}, {0.01f, 0.75f}, {0.5f, 0.18f},
            {0.62f, 0.56f}, {0.01f, 0.34f}, {0.01f, 0.52f}, {0.99f, 0.02f}, {0.59f, 0.88f}, {0.01f, 0.22f},
            {0.18f, 0.01f}, {0.01f, 0.19f}, {0.34f, 0.01f}, {0.72f, 0.72f}, {0.50f, 0.02f},
            {0.01f, 0.65f}, {0.84f, 0.24f}, {0.82f, 0.08f}, {0.98f, 0.66f}, {0.98f, 0.88f}
    };
    private static final Set<EquipmentSlot> lowArmorWarned = EnumSet.noneOf(EquipmentSlot.class);

    private static final Map<String, float[]> POS = new LinkedHashMap<>();   // relX, relY (0..1)
    private static final Map<String, int[]> SIZE = new HashMap<>();         // w, h of last frame
    private static final ArrayDeque<Long> lmb = new ArrayDeque<>(), rmb = new ArrayDeque<>();
    private static float lastTd;
    private static boolean lmbWas, rmbWas;

    public static void init() {
        for (int i = 0; i < ELEMENTS.length; i++) POS.put(ELEMENTS[i], DEFAULTS[i].clone());
        load();
        HudElementRegistry.addLast(Identifier.of("clickgui", "hud"), (ctx, tick) -> {
            if (mc.player == null || mc.options.hudHidden) return;
            float td = tick.getTickProgress(true);
            lastTd = td;
            HudWorld.renderLowHealthVignette(ctx);
            Nametags.render(ctx, td);
            HandVisuals.renderHudOverlays(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
            if (mc.currentScreen instanceof HudEditorScreen) return; // editor draws HUD itself
            renderAll(ctx);
        });
    }

    // ---------------- positions ----------------
    public static int w(String n) { int[] s = SIZE.get(n); return s == null ? 40 : s[0]; }
    public static int h(String n) { int[] s = SIZE.get(n); return s == null ? 12 : s[1]; }
    public static int x(String n) { return Math.round(POS.get(n)[0] * Math.max(0, mc.getWindow().getScaledWidth() - w(n))); }
    public static int y(String n) { return Math.round(POS.get(n)[1] * Math.max(0, mc.getWindow().getScaledHeight() - h(n))); }

    public static void setPos(String n, int x, int y) {
        int mw = Math.max(1, mc.getWindow().getScaledWidth() - w(n)), mh = Math.max(1, mc.getWindow().getScaledHeight() - h(n));
        POS.put(n, new float[]{clamp((float) x / mw), clamp((float) y / mh)});
    }

    public static void reset(String n) {
        for (int i = 0; i < ELEMENTS.length; i++) if (ELEMENTS[i].equals(n)) POS.put(n, DEFAULTS[i].clone());
    }

    public static boolean visible(String n) { return ModuleManager.get(n).enabled; }

    // ---------------- rendering ----------------
    public static void renderAll(DrawContext ctx) {
        for (String n : ELEMENTS) {
            if (!visible(n)) continue;
            int[] wh = draw(ctx, n, x(n), y(n));
            SIZE.put(n, wh);
        }
        if (!(mc.currentScreen instanceof HudEditorScreen)) {
            renderHitRange(ctx);
            renderMaceAndTraps(ctx);
        }
    }

    private static int[] draw(DrawContext c, String n, int x, int y) {
        return switch (n) {
            case "Watermark" -> watermark(c, x, y);
            case "Coordinates" -> {
                var p = mc.player.getBlockPos();
                yield label(c, x, y, "XYZ", p.getX() + " " + p.getY() + " " + p.getZ());
            }
            case "FPS Counter" -> label(c, x, y, "FPS", String.valueOf(mc.getCurrentFps()));
            case "Armor HUD" -> armor(c, x, y);
            case "Keystrokes" -> keystrokes(c, x, y);
            case "Armor Notifier" -> armorNotifier(c, x, y);
            default -> {
                int[] combat = HudCombat.draw(c, n, x, y, lastTd);
                yield (combat[0] > 0 || combat[1] > 0) ? combat : HudWorld.draw(c, n, x, y);
            }
        };
    }

    private static int[] watermark(DrawContext c, int x, int y) {
        Module m = ModuleManager.get("Watermark");
        String s = "ClickGUI  |  " + mc.getCurrentFps() + " fps";
        if (m.bool("Time")) { LocalTime t = LocalTime.now(); s += String.format("  |  %02d:%02d", t.getHour(), t.getMinute()); }
        int w = tw(s) + 12, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        c.fill(x + 3, y, x + w - 3, y + 1, Theme.accent());
        txt(c, s, x + 6, y + 3, WHITE);
        return new int[]{w, h};
    }

    public static int[] labelPublic(DrawContext c, int x, int y, String key, String val) { return label(c, x, y, key, val); }
    private static int[] label(DrawContext c, int x, int y, String key, String val) {
        int w = tw(key) + tw(val) + 16, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        txt(c, key, x + 6, y + 3, Theme.accent());
        txt(c, val, x + 10 + tw(key), y + 3, WHITE);
        return new int[]{w, h};
    }

    private static int[] armor(DrawContext c, int x, int y) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        int w = slots.length * 18 + 4, h = 22;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        for (int i = 0; i < slots.length; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            if (st.isEmpty()) continue;
            int ix = x + 3 + i * 18, iy = y + 3;
            c.drawItem(st, ix, iy);
            c.drawStackOverlay(mc.textRenderer, st, ix, iy);
        }
        return new int[]{w, h};
    }

    private static int[] keystrokes(DrawContext c, int x, int y) {
        var o = mc.options;
        long now = System.currentTimeMillis();
        boolean l = o.attackKey.isPressed(), r = o.useKey.isPressed();
        if (l && !lmbWas) lmb.add(now);
        if (r && !rmbWas) rmb.add(now);
        lmbWas = l; rmbWas = r;
        while (!lmb.isEmpty() && now - lmb.peekFirst() > 1000) lmb.pollFirst();
        while (!rmb.isEmpty() && now - rmb.peekFirst() > 1000) rmb.pollFirst();
        boolean cps = ModuleManager.get("Keystrokes").bool("CPS");

        int k = 18, g = 2, w = k * 3 + g * 2;
        key(c, x + k + g, y, k, k, "W", o.forwardKey.isPressed());
        key(c, x, y + k + g, k, k, "A", o.leftKey.isPressed());
        key(c, x + k + g, y + k + g, k, k, "S", o.backKey.isPressed());
        key(c, x + (k + g) * 2, y + k + g, k, k, "D", o.rightKey.isPressed());
        int my = y + (k + g) * 2, half = (w - g) / 2;
        key(c, x, my, half, k, cps ? lmb.size() + " CPS" : "LMB", l);
        key(c, x + half + g, my, half, k, cps ? rmb.size() + " CPS" : "RMB", r);
        key(c, x, my + k + g, w, 10, "___", o.jumpKey.isPressed());
        return new int[]{w, (k + g) * 3 + 10};
    }

    private static void key(DrawContext c, int x, int y, int w, int h, String s, boolean down) {
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, down ? Theme.accent() : BG);
        txt(c, s, x + (w - tw(s)) / 2, y + (h - 8) / 2, down ? DARK : WHITE);
    }


    private static int[] armorNotifier(DrawContext c, int x, int y) {
        Module m = ModuleManager.get("Armor Notifier");
        int threshold = (int) m.num("Threshold %");
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"Helmet", "Chestplate", "Leggings", "Boots"};
        List<String> low = new ArrayList<>();
        for (int i = 0; i < armorSlots.length; i++) {
            EquipmentSlot slot = armorSlots[i];
            ItemStack st = mc.player.getEquippedStack(slot);
            if (!st.isEmpty() && st.isDamageable() && st.getMaxDamage() > 0) {
                int pct = (int) Math.round(100.0 * (st.getMaxDamage() - st.getDamage()) / st.getMaxDamage());
                if (pct <= threshold) {
                    low.add(labels[i] + " " + pct + "%");
                    if (lowArmorWarned.add(slot)) {
                        Notifications.push("LOW ARMOR", labels[i] + " " + pct + "%", 0xFFFF5555);
                        if (m.bool("Sound") && mc.world != null) {
                        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.8f, 1.5f, false);
                        }
                    }
                } else {
                    lowArmorWarned.remove(slot);
                }
            } else {
                lowArmorWarned.remove(slot);
            }
        }
        if (low.isEmpty()) {
            if (mc.currentScreen instanceof HudEditorScreen) {
                String demo = "LOW ARMOR: Helmet 12%";
                int w = tw(demo) + 14, h = 14;
                ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
                txt(c, demo, x + 7, y + 3, 0xFFFF6B6B);
                return new int[]{w, h};
            }
            return new int[]{90, 14};
        }
        String text = "LOW ARMOR: " + String.join(", ", low);
        int w = tw(text) + 14, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, 0xCC2A0D12);
        c.fill(x + 3, y, x + w - 3, y + 1, 0xFFFF5555);
        txt(c, text, x + 7, y + 3, 0xFFFF7A7A);
        return new int[]{w, h};
    }

    private static void renderHitRange(DrawContext c) {
        Module m = ModuleManager.get("Hit Range");
        if (!m.enabled || mc.world == null) return;
        LivingEntity target = findRangeTarget();
        if (target == null) return;

        Vec3d eye = mc.player.getEyePos();
        Box box = target.getBoundingBox();
        double cx = Math.max(box.minX, Math.min(eye.x, box.maxX));
        double cy = Math.max(box.minY, Math.min(eye.y, box.maxY));
        double cz = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
        double dist = eye.distanceTo(new Vec3d(cx, cy, cz));

        double reach = m.num("Reach");
        boolean inRange = dist <= reach;
        int col = inRange ? 0xFF4ADE80 : 0xFFF59E0B;
        String mode = ((Setting.Mode) m.find("Mode")).get();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int mx = sw / 2, my = sh / 2;

        if (mode.equals("Crosshair") || mode.equals("Both")) {
            int g = 5, l = 4;
            c.fill(mx - g - l, my, mx - g, my + 1, col);
            c.fill(mx + g + 1, my, mx + g + l + 1, my + 1, col);
            c.fill(mx, my - g - l, mx + 1, my - g, col);
            c.fill(mx, my + g + 1, mx + 1, my + g + l + 1, col);
        }
        if (mode.equals("Text") || mode.equals("Both")) {
            String s = String.format(Locale.ROOT, "%.2fm", dist);
            int w = tw(s);
            ClickGuiScreen.roundRect(c, mx - w / 2 - 4, my + 10, w + 8, 12, 3, 0x99101620);
            txt(c, s, mx - w / 2, my + 12, col);
        }
    }

    private static LivingEntity findRangeTarget() {
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le
                && le != mc.player && le.isAlive()) {
            return le;
        }
        Vec3d eye = mc.player.getEyePos();
        Vec3d look = mc.player.getRotationVector().normalize();
        LivingEntity best = null;
        double bestDist = 6.5;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()) continue;
            Vec3d center = le.getBoundingBox().getCenter();
            double d = eye.distanceTo(center);
            if (d > bestDist) continue;
            Vec3d dir = center.subtract(eye).normalize();
            if (dir.dotProduct(look) > 0.85) {
                bestDist = d;
                best = le;
            }
        }
        return best;
    }

    private static void renderMaceAndTraps(DrawContext c) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int y = sh / 2 + 26;
        if (MaceHelper.ready) {
            String s = String.format(Locale.ROOT, "MACE SMASH  %.1fm", MaceHelper.fallDistance);
            int w = tw(s) + 12;
            int x = (sw - w) / 2;
            ClickGuiScreen.roundRect(c, x, y, w, 14, 4, BG);
            c.fill(x + 3, y, x + w - 3, y + 1, Theme.accent());
            txt(c, s, x + 6, y + 3, Theme.accent());
            y += 18;
        }
        Module pvp = ModuleManager.get("PvP Save");
        if (pvp.enabled && pvp.bool("HUD Timer") && !PvpSave.ZONES.isEmpty()) {
            long now = System.currentTimeMillis();
            for (PvpSave.TrapZone z : PvpSave.ZONES) {
                long rem = z.expiresAt() - now;
                if (rem <= 0) continue;
                String s = String.format(Locale.ROOT, "%s 5x5  %.1fs", z.label(), rem / 1000.0);
                int w = tw(s) + 12;
                int x = (sw - w) / 2;
                int col = rem < 3000 ? 0xFFFF5555 : Theme.accent();
                ClickGuiScreen.roundRect(c, x, y, w, 14, 4, BG);
                txt(c, s, x + 6, y + 3, col);
                y += 17;
            }
        }
    }

    // ---------------- save / load ----------------
    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (var e : POS.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("x", e.getValue()[0]);
                o.addProperty("y", e.getValue()[1]);
                root.add(e.getKey(), o);
            }
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(root));
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            for (String n : ELEMENTS) if (root.has(n)) {
                JsonObject o = root.getAsJsonObject(n);
                POS.put(n, new float[]{clamp(o.get("x").getAsFloat()), clamp(o.get("y").getAsFloat())});
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ---------------- utils ----------------
    static Text t(String s) { return Text.literal(s).setStyle(Style.EMPTY.withFont(FONT)); }
    static int tw(String s) { return mc.textRenderer.getWidth(t(s)); }
    static void txt(DrawContext c, String s, int x, int y, int col) { c.drawText(mc.textRenderer, t(s), x, y, col, false); }
    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
