package dev.cigarette.lib;

import dev.cigarette.helper.WeaponHelper;
import dev.cigarette.mixin.ClientWorldAccessor;
import dev.cigarette.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class AimingL {
    /**
     * Compute yaw/pitch angles (in degrees) to look from 'from' to 'to'.
     * Yaw is in [-180..180], pitch is in [-90..90].
     */
    public static float[] anglesFromTo(Vec3d from, Vec3d to) {
        Vec3d d = to.subtract(from);
        double lenSq = d.lengthSquared();
        if (!isFinite(d.x) || !isFinite(d.y) || !isFinite(d.z) || lenSq < 1.0e-12) {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            float fallbackYaw = p != null ? MathHelper.wrapDegrees(p.getYaw()) : 0f;
            float fallbackPitch = p != null ? MathHelper.clamp(p.getPitch(), -90f, 90f) : 0f;
            return new float[]{fallbackYaw, fallbackPitch};
        }
        Vec3d v = d.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.asin(-v.y));
        yaw = sanitizeYaw(yaw);
        pitch = sanitizePitch(pitch);
        yaw = limitAccuracy(yaw, 100);
        pitch = limitAccuracy(pitch, 100);
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            ClientPlayerEntity p = MinecraftClient.getInstance().player;
            float fallbackYaw = p != null ? MathHelper.wrapDegrees(p.getYaw()) : 0f;
            float fallbackPitch = p != null ? MathHelper.clamp(p.getPitch(), -90f, 90f) : 0f;
            return new float[]{fallbackYaw, fallbackPitch};
        }
        return new float[]{yaw, pitch};
    }

    /**
     * Generate a random signed double in the range [-max, -min] U [min, max].
     * If max <= 0, returns 0.0.
     */
    public static double randomSigned(double min, double max) {
        if (max <= 0) return 0.0;
        double magnitude = min + (Math.random() * (max - min));
        return (Math.random() < 0.5 ? -1 : 1) * magnitude;
    }

    /**
     * Clamp point p to be inside the given box, with the given inset from each face.
     */
    public static Vec3d clampToBox(Vec3d p, Box box, double inset) {
        double minX = box.minX + inset, minY = box.minY + inset, minZ = box.minZ + inset;
        double maxX = box.maxX - inset, maxY = box.maxY - inset, maxZ = box.maxZ - inset;
        return new Vec3d(
                MathHelper.clamp(p.x, minX, maxX),
                MathHelper.clamp(p.y, minY, maxY),
                MathHelper.clamp(p.z, minZ, maxZ)
        );
    }

    /**
     * Clamp point p to be inside the given box, with no inset.
     */
    public static Vec3d clampToBox(Vec3d p, Box box) {
        return clampToBox(p, box, 0.0);
    }

    /**
     * Get an aim point inside the target's hitbox, with optional jitter when attacking.
     * If attackNow is false, returns the closest point on the hitbox to the player's eye position.
     * If attackNow is true, returns a jittered point towards the center of the hitbox.
     * Jitter is controlled by jitterMinDeg, jitterMaxDeg and jitterCapDegrees parameters.
     */
    public static Vec3d getAimPointInsideHitbox(ClientPlayerEntity player, LivingEntity target, boolean attackNow,
                                                 double jitterMinDeg, double jitterMaxDeg, double jitterCapDegrees) {
        Box box = target.getBoundingBox();
        Vec3d eye = player.getEyePos();
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        if (!attackNow) {
            double vy = minY + (maxY - minY) * 0.5;
            double vx = (minX + maxX) * 0.5;
            double vz = (minZ + maxZ) * 0.5;
            Vec3d center = new Vec3d(vx, vy, vz);
            if (center.squaredDistanceTo(eye) < 1.0e-12) {
                center = center.add(0.0, 1.0e-4, 0.0);
            }
            return center;
        }

        double height = maxY - minY;
        double region = Math.random();
        double pickY;
        if (region < 0.6) {
            pickY = minY + height * (0.4 + Math.random() * 0.2);
        } else if (region < 0.85) {
            pickY = minY + height * (0.78 + Math.random() * 0.18);
        } else {
            pickY = minY + height * (0.12 + Math.random() * 0.18);
        }

        double cx = (minX + maxX) * 0.5;
        double cz = (minZ + maxZ) * 0.5;
        double halfX = (maxX - minX) * 0.5;
        double halfZ = (maxZ - minZ) * 0.5;
        double ox = (Math.random() - 0.5) * halfX * 0.6;
        double oz = (Math.random() - 0.5) * halfZ * 0.6;

        Vec3d pick = new Vec3d(cx + ox, pickY, cz + oz);
        if (pick.squaredDistanceTo(eye) < 1.0e-12) {
            pick = pick.add(0.0, 1.0e-4, 0.0);
        }
        return clampToBox(pick, box, 1e-3);
    }

    /**
     * Start or stop sprinting based on the 'sprint' parameter.
     * Only start sprinting if on ground and moving forward.
     */
    public static void doSprint(boolean sprint) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        KeyBinding forwardKey = KeyBinding.byId("key.forward");
        boolean forwardPressed = InputOverride.isActive ? InputOverride.forwardKey : (forwardKey != null && forwardKey.isPressed());
        if (!player.isOnGround() || !forwardPressed) return;
        if (sprint) {
            if (!player.isSprinting() && !player.isUsingItem() && !player.isSneaking()) {
                player.setSprinting(true);
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING));
            }
        } else {
            if (player.isSprinting()) {
                player.setSprinting(false);
                player.networkHandler.sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
    }

    /**
     * Switch to the best PvP weapon based on distance to target.
     * If distance > 7.5, prefer ranged weapon, else prefer melee weapon.
     * If no suitable weapon found, do nothing.
     * Returns true if a switch was made or already on best weapon, or false if no suitable weapon found.
     */
    public static void switchToBestPvPWeapon(ClientPlayerEntity player, double distanceToTarget) {
        if (player == null) return;
        int current = player.getInventory().getSelectedSlot();
        int bestSlot;
        if (distanceToTarget > 7.5) {
            bestSlot = WeaponHelper.getBestRangedSlot(player);
            if (bestSlot == -1) bestSlot = WeaponHelper.getBestMeleeSlot(player);
        } else {
            bestSlot = WeaponHelper.getBestMeleeSlot(player);
            if (bestSlot == -1) bestSlot = WeaponHelper.getBestRangedSlot(player);
        }
        if (bestSlot == -1) return;
        if (bestSlot == current) return;
        player.getInventory().setSelectedSlot(bestSlot);
    }

    /**
     * Send a PlayerInteractItemC2SPacket with the given yaw/pitch and a new sequence number.
     * Uses PendingUpdateManager to ensure proper sequencing.
     */
    public static void sendAimPacket(ClientWorld world, ClientPlayerEntity player, float yaw, float pitch) {
        if (world == null || player == null) return;
        // Keep yaw continuous relative to current player yaw to avoid modulo-360 jumps
        float cur = player.getYaw();
        float syaw = Float.isFinite(yaw) ? (cur + MathHelper.wrapDegrees(yaw - cur)) : cur;
        float spitch = sanitizePitch(pitch);
        ClientWorldAccessor accessor = (ClientWorldAccessor) world;
        try (PendingUpdateManager pum = accessor.getPendingUpdateManager().incrementSequence()) {
            int seq = pum.getSequence();
            player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, syaw, spitch));
        }
    }

    /**
     * Send a look packet followed by an attack and a hand swing. Useful for revive/auras that need to set look client-side.
     */
    public static void lookAndAttack(ClientWorld world, ClientPlayerEntity player, LivingEntity target, float yaw, float pitch) {
        if (player == null || target == null || world == null) return;
        HitResult hitResult = MinecraftClient.getInstance().crosshairTarget;
        KeyBinding attackKey = KeyBinding.byId("key.attack");
        KeyBindingAccessor attackKeyAccessor = (KeyBindingAccessor) attackKey;
        if (hitResult == null || attackKey == null || !attackKey.isPressed()) return;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;
            if (livingEntity.hurtTime > 1) return;
            attackKeyAccessor.setTimesPressed(attackKeyAccessor.getTimesPressed() + 1);
        }
    }

    // Helpers
    private static boolean isFinite(double d) {
        return !Double.isNaN(d) && !Double.isInfinite(d);
    }
    private static float sanitizeYaw(float yaw) {
        if (!Float.isFinite(yaw)) yaw = 0f;
        return MathHelper.wrapDegrees(yaw);
    }
    // Keep for API parity even if unused internally
    private static float limitAccuracy(float yaw, int precision) {
        if (precision <= 0) return yaw;
        float factor = 1f / precision;
        return Math.round(yaw * precision) * factor;
    }
    private static float sanitizePitch(float pitch) {
        if (!Float.isFinite(pitch)) pitch = 0f;
        return MathHelper.clamp(pitch, -90f, 90f);
    }
}