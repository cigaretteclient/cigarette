package dev.cigarette.helper;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class PlayerEntityHelper {
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

    public static void setRotationVector(PlayerEntity player, Vec3d vector) {
        float[] yawPitch = getRotationVectorInDirection(vector);
        player.setYaw(yawPitch[0]);
        player.setPitch(yawPitch[1]);
    }

    public static float angleBetween(float yaw, float pitch, float yaw2, float pitch2) {
        float yawDiff = Math.abs(((yaw2 - yaw + 180) % 360) - 180);
        float pitchDiff = Math.abs(pitch2 - pitch);
        return (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff) % 360;
    }
}
