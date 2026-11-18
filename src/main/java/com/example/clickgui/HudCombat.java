package com.example.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/**
 * ЧАСТЬ 2 — Боевые, статусные и системные HUD-виджеты:
 * Target HUD, Cooldowns HUD, Potion HUD, ArrayList (кэшированная сортировка),
 * Totem Counter, Armor HUD Pro, Hotbar Customizer, Ping & TPS, Speedometer, Kill & Combo.
 */
public final class HudCombat {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final int BG = 0xAA101620, WHITE = 0xFFFFFFFF, GRAY = 0xFFB4BECB;

    // Target HUD animation state
    private static double smoothTargetHp = 20, trailTargetHp = 20;
    private static int lastTargetId = -1;

    // Hotbar smooth highlight slot
    private static float smoothSlot = 0f;

    // Cached ArrayList sorting (dirty-flag)
    private static final List<Module> sortedModules = new ArrayList<>();
    private static boolean arrayListDirty = true;

    // Pre-allocated Cooldown items (0 heap alloc per frame)
    private static final ItemStack[] CD_STACKS = {
            new ItemStack(Items.ENDER_PEARL),
            new ItemStack(Items.CHORUS_FRUIT),
            new ItemStack(Items.GOLDEN_APPLE),
            new ItemStack(Items.SHIELD),
            new ItemStack(Items.WIND_CHARGE)
    };
    private static final String[] CD_NAMES = {"Pearl", "Chorus", "Gapple", "Shield", "Wind"};

    public static void markArrayListDirty() { arrayListDirty = true; }

    public static int[] draw(DrawContext c, String n, int x, int y, float td) {
        return switch (n) {
            case "Target HUD" -> targetHud(c, x, y);
            case "Cooldowns HUD" -> cooldownsHud(c, x, y, td);
            case "Potion HUD" -> potionHud(c, x, y);
            case "ArrayList" -> arrayList(c, x, y);
            case "Totem Counter" -> totemCounter(c, x, y);
            case "Armor HUD Pro" -> armorPro(c, x, y);
            case "Ping & TPS" -> pingTps(c, x, y);
            case "Speedometer" -> speedometer(c, x, y);
            case "Kill & Combo" -> killCombo(c, x, y);
            default -> new int[]{0, 0};
        };
    }

    // ---------------- 1. Target HUD ----------------
    private static int[] targetHud(DrawContext c, int x, int y) {
        LivingEntity le = Effects.lastTarget();
        boolean editor = mc.currentScreen instanceof HudEditorScreen;
        if (le == null) {
            if (!editor) return new int[]{142, 42};
            le = mc.player;
        }
        int w = 148, h = 44;
        ClickGuiScreen.roundRect(c, x, y, w, h, 5, BG);
        c.fill(x + 4, y, x + w - 4, y + 1, Theme.accent());

        // Head portrait (3D skin head for players)
        if (le instanceof PlayerEntity pe && mc.getNetworkHandler() != null) {
            PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(pe.getUuid());
            if (entry != null) {
                PlayerSkinDrawer.draw(c, entry.getSkinTextures(), x + 6, y + 6, 32);
            } else {
                ClickGuiScreen.roundRect(c, x + 6, y + 6, 32, 32, 4, 0xFF263042);
            }
        } else {
            ClickGuiScreen.roundRect(c, x + 6, y + 6, 32, 32, 4, 0xFF263042);
            String ini = le.getName().getString().substring(0, Math.min(2, le.getName().getString().length())).toUpperCase(Locale.ROOT);
            Hud.txt(c, ini, x + 15, y + 18, Theme.accent());
        }

        double max = Math.max(1.0, le.getMaxHealth());
        double cur = Math.max(0.0, le.getHealth());
        double abs = Math.max(0.0, le.getAbsorptionAmount());
        if (le.getId() != lastTargetId) {
            lastTargetId = le.getId();
            smoothTargetHp = cur;
            trailTargetHp = cur;
        } else {
            smoothTargetHp += (cur - smoothTargetHp) * 0.25;
            if (trailTargetHp < cur) trailTargetHp = cur;
            else trailTargetHp = Math.max(cur, trailTargetHp - 0.08);
        }

        // Nick + Win/Lose badge
        String name = le.getName().getString();
        if (name.length() > 13) name = name.substring(0, 12) + "..";
        Hud.txt(c, name, x + 43, y + 6, WHITE);

        float myTotal = mc.player.getHealth() + mc.player.getAbsorptionAmount() + mc.player.getArmor() * 0.5f;
        float enTotal = (float) (cur + abs + le.getArmor() * 0.5f);
        String status = myTotal > enTotal + 1f ? "WIN" : (enTotal > myTotal + 1f ? "LOSE" : "EVEN");
        int stCol = status.equals("WIN") ? 0xFF4ADE80 : status.equals("LOSE") ? 0xFFEF4444 : 0xFFFACC15;
        Hud.txt(c, status, x + w - Hud.tw(status) - 6, y + 6, stCol);

        // Armor mini-row + HP number
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int ax = x + 43;
        for (EquipmentSlot s : slots) {
            ItemStack st = le.getEquippedStack(s);
            if (!st.isEmpty()) {
                c.getMatrices().pushMatrix();
                c.getMatrices().translate(ax, y + 16);
                c.getMatrices().scale(0.68f, 0.68f);
                c.drawItem(st, 0, 0);
                c.getMatrices().popMatrix();
                ax += 12;
            }
        }
        String hpStr = String.format(Locale.ROOT, "%.1f", cur + abs);
        Hud.txt(c, hpStr, x + w - Hud.tw(hpStr) - 6, y + 18, abs > 0 ? 0xFFFBBF24 : WHITE);

        // Smooth HP bar + damage delay trail
        int bx = x + 43, by = y + 32, bw = w - 49, bh = 5;
        c.fill(bx, by, bx + bw, by + bh, 0xFF1F2937);
        int tw = (int) Math.round(bw * Math.min(1.0, trailTargetHp / max));
        int hw = (int) Math.round(bw * Math.min(1.0, smoothTargetHp / max));
        if (tw > hw) c.fill(bx, by, bx + tw, by + bh, 0xCCFFFFFF);
        if (hw > 0) c.fill(bx, by, bx + hw, by + bh, Theme.accent());
        if (abs > 0) {
            int aw = (int) Math.round(bw * Math.min(1.0, abs / max));
            c.fill(bx, by - 2, bx + aw, by - 1, 0xFFFBBF24);
        }
        return new int[]{w, h};
    }

    // ---------------- 2. Cooldowns HUD ----------------
    private static int[] cooldownsHud(DrawContext c, int x, int y, float td) {
        ItemCooldownManager cd = mc.player.getItemCooldownManager();
        boolean editor = mc.currentScreen instanceof HudEditorScreen;
        int row = 0, w = 88;
        int startY = y;
        for (int i = 0; i < CD_STACKS.length; i++) {
            float prog = cd.getCooldownProgress(CD_STACKS[i], td);
            if (prog <= 0f && !(editor && i < 2)) continue;
            if (editor && prog <= 0f) prog = 0.65f - i * 0.25f;
            int ry = startY + row * 18;
            ClickGuiScreen.roundRect(c, x, ry, w, 16, 4, BG);
            c.getMatrices().pushMatrix();
            c.getMatrices().translate(x + 3, ry + 2);
            c.getMatrices().scale(0.75f, 0.75f);
            c.drawItem(CD_STACKS[i], 0, 0);
            c.getMatrices().popMatrix();
            Hud.txt(c, CD_NAMES[i], x + 18, ry + 4, WHITE);
            String pct = Math.round(prog * 100) + "%";
            Hud.txt(c, pct, x + w - Hud.tw(pct) - 5, ry + 4, Theme.accent());
            int barW = Math.round((w - 6) * prog);
            c.fill(x + 3, ry + 14, x + 3 + barW, ry + 15, Theme.accent());
            row++;
        }
        return new int[]{w, Math.max(16, row * 18)};
    }

    // ---------------- 3. Potion Effects HUD ----------------
    private static int[] potionHud(DrawContext c, int x, int y) {
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        boolean editor = mc.currentScreen instanceof HudEditorScreen;
        if (effects.isEmpty() && !editor) return new int[]{110, 14};
        int w = 114, row = 0;
        if (effects.isEmpty()) {
            ClickGuiScreen.roundRect(c, x, y, w, 15, 4, BG);
            Hud.txt(c, "Strength II", x + 5, y + 4, WHITE);
            Hud.txt(c, "0:42", x + w - Hud.tw("0:42") - 5, y + 4, Theme.accent());
            return new int[]{w, 15};
        }
        for (StatusEffectInstance se : effects) {
            int ry = y + row * 16;
            String raw = se.getEffectType().getIdAsString().replace("minecraft:", "").replace('_', ' ');
            String name = raw.isEmpty() ? "Effect" : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
            if (se.getAmplifier() > 0) name += " " + roman(se.getAmplifier() + 1);
            if (name.length() > 14) name = name.substring(0, 13) + ".";

            int ticks = se.getDuration();
            boolean inf = se.isInfinite();
            String dur = inf ? "**:**" : String.format(Locale.ROOT, "%d:%02d", (ticks / 20) / 60, (ticks / 20) % 60);
            boolean warn = !inf && ticks < 200; // < 10s
            int tCol = warn ? (((System.currentTimeMillis() / 250) % 2 == 0) ? 0xFFFF5555 : 0xFFFCA5A5) : GRAY;

            ClickGuiScreen.roundRect(c, x, ry, w, 14, 4, BG);
            c.fill(x, ry + 2, x + 2, ry + 12, warn ? 0xFFFF5555 : Theme.accent());
            Hud.txt(c, name, x + 6, ry + 3, WHITE);
            Hud.txt(c, dur, x + w - Hud.tw(dur) - 5, ry + 3, tCol);
            row++;
        }
        return new int[]{w, Math.max(14, row * 16)};
    }

    // ---------------- 4. ArrayList (Dirty-Cached) ----------------
    private static int[] arrayList(DrawContext c, int x, int y) {
        if (arrayListDirty) {
            sortedModules.clear();
            for (Module m : ModuleManager.MODULES) {
                if (m.enabled && !m.name.equals("ArrayList")) sortedModules.add(m);
            }
            sortedModules.sort((a, b) -> Integer.compare(Hud.tw(b.name), Hud.tw(a.name)));
            arrayListDirty = false;
        }
        int sw = mc.getWindow().getScaledWidth();
        boolean alignRight = x + 40 > sw / 2;
        int maxW = 60, ry = y;
        for (int i = 0; i < sortedModules.size(); i++) {
            Module m = sortedModules.get(i);
            int mw = Hud.tw(m.name) + 10;
            if (mw > maxW) maxW = mw;
            int rx = alignRight ? (x + maxW - mw) : x;
            ClickGuiScreen.roundRect(c, rx, ry, mw, 13, 3, BG);
            int col = Theme.color(System.currentTimeMillis(), i * 0.06f, 255);
            if (alignRight) c.fill(rx + mw - 2, ry + 1, rx + mw, ry + 12, col);
            else c.fill(rx, ry + 1, rx + 2, ry + 12, col);
            Hud.txt(c, m.name, rx + (alignRight ? 4 : 6), ry + 3, WHITE);
            ry += 14;
        }
        return new int[]{maxW, Math.max(14, ry - y)};
    }

    // ---------------- 5. Totem Counter ----------------
    private static int[] totemCounter(DrawContext c, int x, int y) {
        int count = ServerMetrics.totems();
        String s = String.valueOf(count);
        int w = 24 + Hud.tw(s), h = 20;
        boolean low = count <= 1;
        boolean blink = low && (System.currentTimeMillis() / 320) % 2 == 0;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, blink ? 0xAA450A0A : BG);
        c.drawItem(new ItemStack(Items.TOTEM_OF_UNDYING), x + 3, y + 2);
        Hud.txt(c, s, x + 21, y + 6, low ? 0xFFFF5555 : WHITE);
        return new int[]{w, h};
    }

    // ---------------- 6. Armor HUD Pro ----------------
    private static int[] armorPro(DrawContext c, int x, int y) {
        Module m = ModuleManager.get("Armor HUD Pro");
        boolean vert = m.find("Layout") instanceof Setting.Mode md && md.get().equals("Vertical");
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        int w = vert ? 54 : 112;
        int h = vert ? 76 : 26;
        ClickGuiScreen.roundRect(c, x, y, w, h, 5, BG);
        for (int i = 0; i < slots.length; i++) {
            ItemStack st = mc.player.getEquippedStack(slots[i]);
            int ix = vert ? x + 4 : x + 4 + i * 26;
            int iy = vert ? y + 4 + i * 18 : y + 3;
            if (st.isEmpty()) continue;
            c.drawItem(st, ix, iy);
            if (st.isDamageable() && st.getMaxDamage() > 0) {
                double pct = Math.max(0.0, 1.0 - (double) st.getDamage() / st.getMaxDamage());
                int col = pct > 0.5 ? 0xFF4ADE80 : (pct > 0.2 ? 0xFFFACC15 : 0xFFEF4444);
                String pTxt = Math.round(pct * 100) + "%";
                if (vert) {
                    Hud.txt(c, pTxt, ix + 19, iy + 4, col);
                    c.fill(ix + 19, iy + 13, ix + 19 + (int) Math.round(28 * pct), iy + 14, col);
                } else {
                    c.fill(ix + 1, iy + 17, ix + 1 + (int) Math.round(14 * pct), iy + 19, col);
                    Hud.txt(c, pTxt, ix, iy + 20, col);
                }
            }
        }
        return new int[]{w, h};
    }

    // ---------------- 7. Hotbar Customizer ----------------
    public static void renderCustomHotbar(DrawContext c, float td) {
        if (mc.player == null) return;
        int sw = c.getScaledWindowWidth(), sh = c.getScaledWindowHeight();
        int bw = 182, bh = 22;
        int x = (sw - bw) / 2, y = sh - bh - 3;

        ClickGuiScreen.roundRect(c, x, y, bw, bh, 5, 0xB8101620);
        c.fill(x + 4, y, x + bw - 4, y + 1, 0x40FFFFFF);

        PlayerInventory inv = mc.player.getInventory();
        int sel = inv.getSelectedSlot();
        smoothSlot += (sel - smoothSlot) * 0.35f;
        int hx = x + 2 + Math.round(smoothSlot * 20);
        ClickGuiScreen.roundRect(c, hx, y + 2, 18, 18, 4, 0x44FFFFFF);
        c.fill(hx + 3, y + 19, hx + 15, y + 20, Theme.accent());

        for (int i = 0; i < 9; i++) {
            ItemStack st = inv.getStack(i);
            if (!st.isEmpty()) {
                int ix = x + 3 + i * 20;
                int iy = y + 3;
                c.drawItem(st, ix, iy);
                c.drawStackOverlay(mc.textRenderer, st, ix, iy);
            }
        }

        ItemStack off = mc.player.getOffHandStack();
        if (!off.isEmpty()) {
            int ox = x - 26;
            ClickGuiScreen.roundRect(c, ox, y, 22, 22, 5, 0xB8101620);
            c.drawItem(off, ox + 3, y + 3);
            c.drawStackOverlay(mc.textRenderer, off, ox + 3, y + 3);
        }
    }

    // ---------------- 8. Ping & TPS / Speedometer / Kill & Combo ----------------
    private static int[] pingTps(DrawContext c, int x, int y) {
        float tps = ServerMetrics.tps();
        int ping = ServerMetrics.ping();
        String s = String.format(Locale.ROOT, "%dms  |  %.1f TPS", ping, tps);
        int w = Hud.tw(s) + 12, h = 14;
        ClickGuiScreen.roundRect(c, x, y, w, h, 4, BG);
        int col = tps >= 18.5f ? 0xFF4ADE80 : (tps >= 15f ? 0xFFFACC15 : 0xFFEF4444);
        c.fill(x, y + 3, x + 2, y + h - 3, col);
        Hud.txt(c, s, x + 6, y + 3, WHITE);
        return new int[]{w, h};
    }

    private static int[] speedometer(DrawContext c, int x, int y) {
        String val = String.format(Locale.ROOT, "%.2f b/s", ServerMetrics.bps());
        return Hud.labelPublic(c, x, y, "BPS", val);
    }

    private static int[] killCombo(DrawContext c, int x, int y) {
        int k = ServerMetrics.kills(), d = ServerMetrics.deaths(), cb = ServerMetrics.combo();
        double kd = d == 0 ? k : (double) k / d;
        String val = String.format(Locale.ROOT, "%dC · %dK · %.2f K/D", cb, k, kd);
        return Hud.labelPublic(c, x, y, "PVP", val);
    }

    private static String roman(int n) {
        return switch (n) {
            case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> n > 5 ? String.valueOf(n) : "I";
        };
    }
}
