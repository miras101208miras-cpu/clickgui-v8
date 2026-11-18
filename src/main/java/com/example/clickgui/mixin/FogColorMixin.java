package com.example.clickgui.mixin;

import com.example.clickgui.SkyBox;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Цвет тумана под выбранное небо (дальний туман, вода и лава не трогаются). */
@Mixin(targets = "net.minecraft.client.render.fog.FogRenderer")
public abstract class FogColorMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), require = 0)
    private void clickgui$fogColor(CallbackInfoReturnable<Object> cir) {
        float[] c = SkyBox.fogColor();
        if (c != null && cir.getReturnValue() instanceof Vector4f v) v.set(c[0], c[1], c[2], v.w);
    }
}
