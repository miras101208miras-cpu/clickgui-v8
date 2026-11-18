package com.example.clickgui.mixin;

import com.example.clickgui.Module;
import com.example.clickgui.ModuleManager;
import com.example.clickgui.Setting;
import com.example.clickgui.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chams: стили материалов (Matte, Glass, Pulse) в цвете выбранной темы. */
@Mixin(LivingEntityRenderer.class)
public abstract class ChamsMixin {
    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState state);

    private static boolean applies(LivingEntityRenderState state) {
        Module m = ModuleManager.get("Chams");
        if (!m.enabled || state.invisibleToPlayer) return false;
        boolean isPlayer = state instanceof PlayerEntityRenderState;
        return (isPlayer && m.bool("Players")) || (!isPlayer && m.bool("Mobs"));
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void clickgui$chamsLayer(LivingEntityRenderState state, boolean showBody, boolean translucent,
                                     boolean showOutline, CallbackInfoReturnable<RenderLayer> cir) {
        if (!showBody || !applies(state)) return;
        String mat = ((Setting.Mode) ModuleManager.get("Chams").find("Material")).get();
        Identifier tex = getTexture(state);
        if (mat.equals("Matte")) {
            cir.setReturnValue(RenderLayers.entityTranslucentEmissive(tex));
        } else {
            cir.setReturnValue(RenderLayers.entityTranslucent(tex));
        }
    }

    @Inject(method = "getMixColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void clickgui$chamsTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (!applies(state) || MinecraftClient.getInstance().player == null) return;
        Module m = ModuleManager.get("Chams");
        String mat = ((Setting.Mode) m.find("Material")).get();
        int baseAlpha = (int) m.num("Alpha");
        int alpha = mat.equals("Pulse")
                ? (int) Math.max(45, Math.min(255, baseAlpha * (0.55 + 0.45 * Math.sin(System.currentTimeMillis() / 260.0))))
                : (mat.equals("Matte") ? Math.max(baseAlpha, 210) : baseAlpha);
        int rgb = Theme.color(System.currentTimeMillis(), 0.25f, alpha);
        cir.setReturnValue(rgb);
    }
}
