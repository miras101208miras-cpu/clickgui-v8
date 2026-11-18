package com.example.clickgui.mixin;

import com.example.clickgui.HudCombat;
import com.example.clickgui.ModuleManager;
import com.example.clickgui.ScoreboardHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Перехватывает ванильный хотбар, скорборд и иконки зелий при включённых кастомных HUD. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void clickgui$hotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (ModuleManager.get("Hotbar Customizer").enabled) {
            HudCombat.renderCustomHotbar(context, tickCounter.getTickProgress(true));
            ci.cancel();
        }
    }

    @Inject(
            method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void clickgui$scoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        if (ModuleManager.get("Scoreboard").enabled) {
            ScoreboardHud.render(context, objective);
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void clickgui$potionOverlay(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        var m = ModuleManager.get("Potion HUD");
        if (m.enabled && m.bool("Hide Vanilla")) {
            ci.cancel();
        }
    }
}
