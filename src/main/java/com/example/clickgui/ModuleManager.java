package com.example.clickgui;

import java.util.*;

/** Реестр всех модулей и их настроек по вкладкам. */
public final class ModuleManager {
    public static final List<Module> MODULES = new ArrayList<>();

    static {
        // ---- VISUALS & COSMETICS (ЧАСТЬ 1) ----
        MODULES.add(new Module("Target ESP", Category.VISUALS,
                new Setting.Multi("Style", List.of(
                        "Ring", "Orbit", "Souls", "Pulse", "Glow", "Skeleton", "Halo", "Lock-On", "Circle", "Health", "Frost",
                        "DNA Spiral", "Pentagram", "Prism", "Vortex", "Box 3D"
                ), "Souls"),
                new Setting.Slider("Speed", 1.0, 0.2, 3.0, 0.1),
                new Setting.Slider("Size", 1.0, 0.5, 2.0, 0.05)));
        MODULES.add(new Module("China Hat", Category.VISUALS,
                new Setting.Mode("Style", List.of("Classic Cone", "Halo Ring", "Horns", "Cat Ears", "Crown"), 0),
                new Setting.Slider("Radius", 0.6, 0.3, 1.0, 0.05),
                new Setting.Slider("Height", 0.3, 0.1, 0.8, 0.05)));
        MODULES.add(new Module("Custom Cosmetics", Category.VISUALS,
                new Setting.Mode("Style", List.of("Wings", "Cape", "Both"), 0),
                new Setting.Slider("Scale", 1.0, 0.5, 1.8, 0.05)));
        MODULES.add(new Module("ViewModel", Category.VISUALS,
                new Setting.Slider("Main X", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Main Y", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Main Z", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Off X", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Off Y", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Off Z", 0.0, -0.6, 0.6, 0.02),
                new Setting.Slider("Scale", 1.0, 0.3, 1.6, 0.05),
                new Setting.Slider("Rotate", 0.0, -45.0, 45.0, 1.0)));
        MODULES.add(new Module("Swing Animation", Category.VISUALS,
                new Setting.Mode("Style", List.of("Smooth", "Spin", "Side Slash", "Power"), 0),
                new Setting.Slider("Speed", 1.0, 0.4, 2.5, 0.1)));
        MODULES.add(new Module("Eat Animation", Category.VISUALS,
                new Setting.Bool("Progress Ring", true)));
        MODULES.add(new Module("Enchant Glint", Category.VISUALS,
                new Setting.Mode("Mode", List.of("Theme", "Rainbow", "Neon", "Gold"), 0)));
        MODULES.add(new Module("Held Item Glow", Category.VISUALS,
                new Setting.Slider("Radius", 26, 12, 48, 2)));
        MODULES.add(new Module("Low Shield", Category.VISUALS,
                new Setting.Slider("Drop Y", 0.24, 0.05, 0.45, 0.02)));
        MODULES.add(new Module("Bow Pull Color", Category.VISUALS));
        MODULES.add(new Module("Jump Circles", Category.VISUALS,
                new Setting.Slider("Radius", 1.2, 0.5, 3.0, 0.1),
                new Setting.Slider("Time", 1.0, 0.3, 3.0, 0.1)));
        MODULES.add(new Module("Trails", Category.VISUALS,
                new Setting.Slider("Length", 1.0, 0.2, 3.0, 0.1),
                new Setting.Slider("Height", 0.6, 0.1, 1.8, 0.1)));
        MODULES.add(new Module("Hit Particles", Category.VISUALS,
                new Setting.Multi("Type", List.of("Snow", "Hearts", "Stars"), "Hearts"),
                new Setting.Slider("Count", 14, 4, 40, 1)));
        MODULES.add(new Module("World Particles", Category.VISUALS,
                new Setting.Multi("Type", List.of("Snow", "Hearts", "Stars"), "Snow"),
                new Setting.Slider("Count", 60, 10, 200, 5),
                new Setting.Slider("Radius", 12, 4, 32, 1)));
        MODULES.add(new Module("Hit Color", Category.VISUALS,
                new Setting.Slider("Red", 170, 0, 255, 1),
                new Setting.Slider("Green", 80, 0, 255, 1),
                new Setting.Slider("Blue", 255, 0, 255, 1),
                new Setting.Slider("Alpha", 140, 0, 255, 1)));
        MODULES.add(new Module("Kill Effect", Category.VISUALS,
                new Setting.Multi("Effect", List.of("Lightning", "Particles"), "Lightning")));
        MODULES.add(new Module("Self Nametag", Category.VISUALS));
        MODULES.add(new Module("Arrow Nametag", Category.VISUALS,
                new Setting.Bool("Type", true), new Setting.Bool("Distance", true)));
        MODULES.add(new Module("Render Tweaks", Category.VISUALS,
                new Setting.Bool("No Hurt Cam", true), new Setting.Bool("No Fire Overlay", true)));
        MODULES.add(new Module("Hit Range", Category.VISUALS,
                new Setting.Mode("Mode", List.of("Both", "Text", "Crosshair"), 0),
                new Setting.Slider("Reach", 3.0, 2.0, 6.0, 0.1)));
        MODULES.add(new Module("Prediction", Category.VISUALS,
                new Setting.Bool("Bow", true),
                new Setting.Bool("Pearls", true),
                new Setting.Bool("Potions", true),
                new Setting.Bool("Landing", true)));
        MODULES.add(new Module("PvP Save", Category.VISUALS,
                new Setting.Slider("Radius", 16, 6, 32, 1),
                new Setting.Slider("Time", 15, 5, 60, 1),
                new Setting.Bool("HUD Timer", true)));
        MODULES.add(new Module("3D Hitboxes", Category.VISUALS,
                new Setting.Bool("Players", true),
                new Setting.Bool("Mobs", true),
                new Setting.Bool("Items", false),
                new Setting.Bool("Look Vector", true)));
        MODULES.add(new Module("Outlines", Category.VISUALS,
                new Setting.Bool("Players", true),
                new Setting.Bool("Mobs", true),
                new Setting.Bool("Items", true),
                new Setting.Bool("Theme Color", true)));
        MODULES.add(new Module("Chams", Category.VISUALS,
                new Setting.Mode("Material", List.of("Matte", "Glass", "Pulse"), 1),
                new Setting.Slider("Alpha", 150, 40, 255, 5),
                new Setting.Bool("Players", true),
                new Setting.Bool("Mobs", false)));
        MODULES.add(new Module("Pearl Tracker", Category.VISUALS,
                new Setting.Bool("Trajectory", true),
                new Setting.Bool("Air Time", true)));
        MODULES.add(new Module("Nametags Pro", Category.VISUALS,
                new Setting.Bool("Armor", true),
                new Setting.Bool("Enchants", true),
                new Setting.Bool("Ping", true),
                new Setting.Bool("Health", true)));
        MODULES.add(new Module("Health Bar 3D", Category.VISUALS,
                new Setting.Bool("Players", true),
                new Setting.Bool("Mobs", true),
                new Setting.Bool("Damage Trail", true)));
        MODULES.add(new Module("Potion Ring", Category.VISUALS,
                new Setting.Bool("Self", true),
                new Setting.Bool("Others", true)));

        // ---- WORLD ----
        MODULES.add(new Module("Full Bright", Category.WORLD));
        MODULES.add(new Module("Fuse Radius", Category.WORLD,
                new Setting.Bool("Sphere", true),
                new Setting.Bool("Timer", true)));
        MODULES.add(new Module("Block Outline", Category.WORLD,
                new Setting.Slider("Width", 2.0, 1.0, 5.0, 0.5),
                new Setting.Bool("Fill", true)));
        MODULES.add(new Module("Break Progress", Category.WORLD,
                new Setting.Bool("Percent", true)));
        MODULES.add(new Module("Waypoints", Category.WORLD,
                new Setting.Bool("Auto Death", true),
                new Setting.Bool("Add At Pos", false),
                new Setting.Bool("Clear All", false),
                new Setting.Slider("Max Dist", 1000, 100, 5000, 100)));
        MODULES.add(new Module("Time Changer", Category.WORLD, new Setting.Slider("Time", 6.0, 0.0, 24.0, 0.5)));
        MODULES.add(new Module("Weather Changer", Category.WORLD,
                new Setting.Mode("Weather", List.of("Clear", "Rain", "Thunder"), 0)));
        MODULES.add(new Module("Custom Sky", Category.WORLD,
                new Setting.Mode("Sky", List.of("Aurora Night", "Galaxy", "Sakura Sunset", "Blood Moon", "Neon Dusk", "Cotton Candy",
                        "Black Hole", "Ice Dawn", "Thunderstorm", "Golden Hour", "Moonlit Night", "Pink Nebula",
                        "Violet Cosmos", "Pastel Planets", "Anime Clouds"), 0),
                new Setting.Slider("Spin", 0.0, 0.0, 1.0, 0.05)));
        MODULES.add(new Module("World Customizer", Category.WORLD,
                new Setting.Slider("Sky R", 120, 0, 255, 1), new Setting.Slider("Sky G", 80, 0, 255, 1), new Setting.Slider("Sky B", 220, 0, 255, 1),
                new Setting.Slider("Fog R", 255, 0, 255, 1), new Setting.Slider("Fog G", 150, 0, 255, 1), new Setting.Slider("Fog B", 200, 0, 255, 1)));

        // ---- HUD (ЧАСТЬ 2 & 3 — все двигаются в HudEditorScreen) ----
        MODULES.add(new Module("Watermark", Category.HUD, new Setting.Bool("Time", true)));
        MODULES.add(new Module("Coordinates", Category.HUD));
        MODULES.add(new Module("FPS Counter", Category.HUD));
        MODULES.add(new Module("Armor HUD", Category.HUD));
        MODULES.add(new Module("Keystrokes", Category.HUD, new Setting.Bool("CPS", true)));
        MODULES.add(new Module("Armor Notifier", Category.HUD,
                new Setting.Slider("Threshold %", 15, 5, 50, 5),
                new Setting.Bool("Sound", true)));
        MODULES.add(new Module("Target HUD", Category.HUD));
        MODULES.add(new Module("Cooldowns HUD", Category.HUD));
        MODULES.add(new Module("Potion HUD", Category.HUD,
                new Setting.Bool("Hide Vanilla", true)));
        MODULES.add(new Module("ArrayList", Category.HUD));
        MODULES.add(new Module("Totem Counter", Category.HUD));
        MODULES.add(new Module("Armor HUD Pro", Category.HUD,
                new Setting.Mode("Layout", List.of("Horizontal", "Vertical"), 0)));
        MODULES.add(new Module("Hotbar Customizer", Category.HUD));
        MODULES.add(new Module("Ping & TPS", Category.HUD));
        MODULES.add(new Module("Speedometer", Category.HUD,
                new Setting.Bool("3D / Elytra", true)));
        MODULES.add(new Module("Kill & Combo", Category.HUD));
        MODULES.add(new Module("Inventory Viewer", Category.HUD));
        MODULES.add(new Module("Shulker Preview", Category.HUD));
        MODULES.add(new Module("Compass Bar", Category.HUD));
        MODULES.add(new Module("Biome Badge", Category.HUD));
        MODULES.add(new Module("Staff & Friends", Category.HUD,
                new Setting.Bool("Middle Click", true)));
        MODULES.add(new Module("Music Player", Category.HUD));
        MODULES.add(new Module("Pickup Feed", Category.HUD));
        MODULES.add(new Module("Notifications", Category.HUD,
                new Setting.Bool("Keybinds", true),
                new Setting.Bool("Target", true)));
        MODULES.add(new Module("Low Health Vignette", Category.HUD,
                new Setting.Slider("Threshold", 6.0, 2.0, 12.0, 0.5),
                new Setting.Bool("Theme Color", false)));
        MODULES.add(new Module("Scoreboard", Category.HUD,
                new Setting.Bool("Hide IP", true),
                new Setting.Bool("Hide Numbers", true)));

        // ---- CAMERA ----
        MODULES.add(new Module("Zoom", Category.CAMERA, new Setting.Slider("Zoom", 3.0, 1.5, 10.0, 0.5)));
        MODULES.add(new Module("FOV Changer", Category.CAMERA, new Setting.Slider("FOV", 90, 30, 120, 1)));
        MODULES.add(new Module("Free Look", Category.CAMERA,
                new Setting.Bool("Auto 3rd Person", true),
                new Setting.Bool("Invert Pitch", false)));

        // ---- MISC ----
        MODULES.add(new Module("Mace Helper", Category.MISC,
                new Setting.Bool("Auto Switch", true),
                new Setting.Bool("Sound", true),
                new Setting.Slider("Min Fall", 1.5, 1.0, 5.0, 0.5)));
        MODULES.add(new Module("Fake Player", Category.MISC,
                new Setting.Bool("Copy Armor", true),
                new Setting.Bool("Look At Me", false)));
        MODULES.add(new Module("Theme", Category.MISC,
                new Setting.Mode("Theme", List.of(Theme.NAMES), 0),
                new Setting.Slider("Hue", 0.85, 0.0, 1.0, 0.01),
                new Setting.Bool("Rainbow", false)));
    }

    public static List<Module> byCategory(Category c) {
        return MODULES.stream().filter(m -> m.category == c).toList();
    }

    public static Module get(String name) {
        for (Module m : MODULES) if (m.name.equals(name)) return m;
        throw new IllegalArgumentException(name);
    }
}
