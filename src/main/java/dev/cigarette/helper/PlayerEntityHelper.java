package dev.cigarette.helper;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Helper class for {@link PlayerEntity} related methods.
 */
public class PlayerEntityHelper {
    /**
     * Computes the rotation vector (yaw, pitch) required to look in the direction of the given vector.
     *
     * @param vector The direction vector.
     * @return A float array where index 0 is yaw and index 1 is pitch.
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

    /**
     * Sets the player's rotation to look in the direction of the given vector.
     *
     * @param player The player entity.
     * @param vector The direction vector.
     */
    public static void setRotationVector(PlayerEntity player, Vec3d vector) {
        float[] yawPitch = getRotationVectorInDirection(vector);
        player.setYaw(yawPitch[0]);
        player.setPitch(yawPitch[1]);
    }

    /**
     * Calculates the angle difference between two sets of yaw and pitch angles
     *
     * @param yaw    The first yaw angle
     * @param pitch  The first pitch angle
     * @param yaw2   The second yaw angle
     * @param pitch2 The second pitch angle
     * @return The difference between the two sets of angles in degrees
     */
    public static float angleBetween(float yaw, float pitch, float yaw2, float pitch2) {
        float yawDiff = Math.abs(((yaw2 - yaw + 180) % 360) - 180);
        float pitchDiff = Math.abs(pitch2 - pitch);
        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff) % 360;
    }
}
