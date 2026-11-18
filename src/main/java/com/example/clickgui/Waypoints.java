package com.example.clickgui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Waypoints 3D:
 * светящиеся столбы-метки (Death, Base, ручные точки) с дистанцией и автосохранением в waypoints.json.
 */
public final class Waypoints {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("clickgui/waypoints.json");

    public record Waypoint(String name, double x, double y, double z, int color, String dim) {}

    public static final List<Waypoint> LIST = new ArrayList<>();
    private static boolean loaded;
    private static boolean wasDead;

    public static void tick() {
        if (!loaded) {
            load();
            loaded = true;
        }
        Module m = ModuleManager.get("Waypoints");
        if (mc.player == null || mc.world == null) {
            wasDead = false;
            return;
        }

        // Быстрые кнопки прямо из настроек ClickGUI
        if (m.find("Add At Pos") instanceof Setting.Bool addBtn && addBtn.value) {
            addBtn.value = false;
            String dim = mc.world.getRegistryKey().getValue().toString();
            add("WP #" + (LIST.size() + 1), mc.player.getX(), mc.player.getY(), mc.player.getZ(), Theme.accent(), dim);
        }
        if (m.find("Clear All") instanceof Setting.Bool clearBtn && clearBtn.value) {
            clearBtn.value = false;
            LIST.clear();
            save();
        }

        // Авто-метка смерти игрока
        boolean dead = mc.player.isDead() || mc.player.getHealth() <= 0f;
        if (m.enabled && m.bool("Auto Death") && dead && !wasDead) {
            String dim = mc.world.getRegistryKey().getValue().toString();
            LIST.removeIf(w -> w.name().equals("Death"));
            add("Death", mc.player.getX(), mc.player.getY(), mc.player.getZ(), 0xFFFF5555, dim);
        }
        wasDead = dead;
    }

    public static void add(String name, double x, double y, double z, int color, String dim) {
        LIST.add(new Waypoint(name, Math.round(x * 10) / 10.0, Math.round(y * 10) / 10.0, Math.round(z * 10) / 10.0, color, dim));
        save();
    }

    public static String currentDim() {
        return mc.world == null ? "" : mc.world.getRegistryKey().getValue().toString();
    }

    public static void render(VertexConsumer vc) {
        Module m = ModuleManager.get("Waypoints");
        if (!m.enabled || mc.player == null || mc.world == null || LIST.isEmpty()) return;

        String dim = currentDim();
        double maxDist = m.num("Max Dist");
        Vec3d playerPos = mc.player.getEntityPos();

        for (Waypoint wp : LIST) {
            if (!wp.dim().equals(dim)) continue;
            Vec3d p = new Vec3d(wp.x(), wp.y(), wp.z());
            double dist = playerPos.distanceTo(p);
            if (dist > maxDist) continue;

            int col = wp.color();
            int topCol = col & 0x00FFFFFF;
            Vec3d top = p.add(0, 64, 0);

            // Светящийся столб света
            Effects.ribbon(vc, p, top, 0.14, (col & 0x00FFFFFF) | 0xA0000000, topCol);

            // Кольцо у основания
            int seg = 24;
            for (int i = 0; i < seg; i++) {
                double a1 = Math.PI * 2 * i / seg, a2 = Math.PI * 2 * (i + 1) / seg;
                Vec3d r1 = p.add(Math.cos(a1) * 0.55, 0.05, Math.sin(a1) * 0.55);
                Vec3d r2 = p.add(Math.cos(a2) * 0.55, 0.05, Math.sin(a2) * 0.55);
                Effects.ribbon(vc, r1, r2, 0.025, col, col);
            }
        }
    }

    private static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(LIST));
        } catch (Exception ignored) {}
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try {
            List<Waypoint> read = new Gson().fromJson(Files.readString(FILE), new TypeToken<List<Waypoint>>() {}.getType());
            if (read != null) {
                LIST.clear();
                LIST.addAll(read);
            }
        } catch (Exception ignored) {}
    }
}
