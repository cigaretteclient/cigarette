package dev.cigarette.lib;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Vec3d;

public class PlayerEntityL {
    /*
     * Returns yaw and pitch to look in the direction of the given vector.
     */
    public static float[] getRotationVectorInDirection(Vec3d vector) {
        Vec3d normalized = vector.normalize();
        double pitchRadians = Math.asin(-normalized.y);
        float pitch = (float) Math.toDegrees(pitchRadians);
        double yawRadians = Math.atan2(normalized.z, normalized.x);
        float yaw = (float) Math.toDegrees(yawRadians) - 90f;
        yaw %= 360f;
        if (yaw < 0) yaw += 360f;
        return new float[]{yaw, pitch};
    }

    /*
     * Sets the player's yaw and pitch to look in the direction of the given vector.
     */
    public static void setRotationVector(PlayerEntity player, Vec3d vector) {
        float[] yawPitch = getRotationVectorInDirection(vector);
        player.setYaw(yawPitch[0]);
        player.setPitch(yawPitch[1]);
    }

//    public static float getDistance(PlayerEntity player, PlayerEntity other) {
//        return (float) player.getPos().distanceTo(other.getPos());
//    }
}
