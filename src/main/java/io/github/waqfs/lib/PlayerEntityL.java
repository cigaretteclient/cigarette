package io.github.waqfs.lib;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class PlayerEntityL {
    public static @Nullable ItemStack getHeldItem(PlayerEntity player) {
        ItemStack stack = player.getInventory().getSelectedStack();
        if (stack.isEmpty()) return null;
        return stack;
    }

    public static void setRotationVector(PlayerEntity player, Vec3d vector) {
        Vec3d normalized = vector.normalize();
        double pitchRadians = Math.asin(-normalized.y);
        float pitch = (float) Math.toDegrees(pitchRadians);
        double yawRadians = Math.atan2(normalized.z, normalized.x);
        float yaw = (float) Math.toDegrees(yawRadians) - 90f;
        yaw %= 360f;
        if (yaw < 0) yaw += 360f;
        player.setYaw(yaw);
        player.setPitch(pitch);
    }
}
