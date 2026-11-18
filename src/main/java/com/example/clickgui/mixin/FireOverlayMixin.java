package com.example.clickgui.mixin;

import com.example.clickgui.ModuleManager;
import com.example.clickgui.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Render Tweaks -> No Fire Overlay: не рисуем огонь на экране (сам огонь и урон остаются). */
@Mixin(targets = "net.minecraft.client.gui.hud.InGameOverlayRenderer")
public abstract class FireOverlayMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void clickgui$noFire(CallbackInfo ci) {
        Module m = ModuleManager.get("Render Tweaks");
        if (m.enabled && m.bool("No Fire Overlay")) ci.cancel();
    }
}
