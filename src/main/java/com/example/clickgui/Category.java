package com.example.clickgui;

public enum Category {
    VISUALS("Visuals", "Визуал"), HUD("HUD", "HUD"), CAMERA("Camera", "Камера"),
    WORLD("World", "Мир"), MISC("Misc", "Разное");

    private final String en, ru;
    Category(String en, String ru) { this.en = en; this.ru = ru; }
    public String title(boolean russian) { return russian ? ru : en; }
}
