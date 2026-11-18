package com.example.clickgui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/** No Hurt Cam, Time Changer, Weather Changer. Всё только на клиенте. */
public final class Tweaks {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static Double savedTilt;

    public static void init() { ClientTickEvents.END_CLIENT_TICK.register(c -> tick()); }

    private static void tick() {
        // ---- No Hurt Cam: ванильная настройка "Наклон при уроне" = 0 ----
        Module rt = ModuleManager.get("Render Tweaks");
        var tilt = mc.options.getDamageTiltStrength();
        if (rt.enabled && rt.bool("No Hurt Cam")) {
            if (savedTilt == null) savedTilt = tilt.getValue();
            if (tilt.getValue() != 0.0) tilt.setValue(0.0);
        } else if (savedTilt != null) {
            tilt.setValue(savedTilt);
            savedTilt = null;
        }
        if (mc.world == null) return;

        // ---- Time Changer: Time = часы (0..24), 6 = утро, 12 = полдень, 0 = полночь ----
        Module tc = ModuleManager.get("Time Changer");
        if (tc.enabled) {
            long ticks = (long) ((((tc.num("Time") - 6.0) * 1000) % 24000 + 24000) % 24000);
            mc.world.getLevelProperties().setTimeOfDay(ticks);
        }

        // ---- Weather Changer ----
        Module wc = ModuleManager.get("Weather Changer");
        if (wc.enabled) {
            String w = ((Setting.Mode) wc.find("Weather")).get();
            mc.world.setRainGradient(w.equals("Clear") ? 0f : 1f);
            mc.world.setThunderGradient(w.equals("Thunder") ? 1f : 0f);
        }
    }
}
