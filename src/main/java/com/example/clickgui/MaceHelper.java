package com.example.clickgui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Mace Helper: отслеживает падение (fallDistance > Min Fall) при наличии булавы,
 * подаёт сигнал и переключает слот перед касанием земли для критического удара.
 */
public final class MaceHelper {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean ready;
    public static double fallDistance;
    private static boolean alertedThisFall;

    public static void tick() {
        Module m = ModuleManager.get("Mace Helper");
        if (!m.enabled || mc.player == null || mc.world == null) {
            ready = false;
            fallDistance = 0;
            alertedThisFall = false;
            return;
        }
        PlayerInventory inv = mc.player.getInventory();
        int maceSlot = findMaceSlot(inv);
        boolean hasMace = maceSlot >= 0 || mc.player.getOffHandStack().isOf(Items.MACE);

        if (mc.player.isOnGround() || mc.player.isGliding()) {
            ready = false;
            fallDistance = 0;
            alertedThisFall = false;
            return;
        }

        fallDistance = mc.player.fallDistance;
        double minFall = m.num("Min Fall");
        ready = hasMace && fallDistance > minFall;

        if (ready) {
            if (!alertedThisFall) {
                alertedThisFall = true;
                if (m.bool("Sound")) {
                    mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.35f);
                }
            }
            if (m.bool("Auto Switch") && maceSlot >= 0 && inv.getSelectedSlot() != maceSlot
                    && mc.player.getVelocity().y < -0.15 && nearGround(3.5)) {
                inv.setSelectedSlot(maceSlot);
            }
        }
    }

    private static int findMaceSlot(PlayerInventory inv) {
        if (inv.getSelectedStack().isOf(Items.MACE)) return inv.getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.MACE)) return i;
        }
        return -1;
    }

    private static boolean nearGround(double dist) {
        Vec3d pos = mc.player.getEntityPos();
        HitResult hr = mc.world.raycast(new RaycastContext(
                pos, pos.subtract(0, dist, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
        return hr.getType() == HitResult.Type.BLOCK;
    }
}
