package com.example.clickgui.mixin;

import com.example.clickgui.HandVisuals;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Миксин 1-го лица: ViewModel, Low Shield, Swing Animation и Eat / Drink Animation. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            ),
            require = 0
    )
    private void clickgui$viewModel(
            AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand,
            float swingProgress, ItemStack item, float equipProgress,
            MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, CallbackInfo ci) {
        HandVisuals.transformHeld(player, hand, item, matrices);
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void clickgui$swing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (HandVisuals.applySwing(matrices, arm, swingProgress)) ci.cancel();
    }

    @Inject(method = "applyEatOrDrinkTransformation", at = @At("HEAD"), cancellable = true, require = 0)
    private void clickgui$eat(MatrixStack matrices, float tickProgress, Arm arm, ItemStack stack, PlayerEntity player, CallbackInfo ci) {
        if (HandVisuals.applyEatOrDrink(matrices, tickProgress, arm, stack, player)) ci.cancel();
    }
}
