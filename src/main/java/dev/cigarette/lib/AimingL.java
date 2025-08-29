package dev.cigarette.lib;

import dev.cigarette.helper.WeaponHelper;
import dev.cigarette.mixin.ClientWorldAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AimingL {
    /**
     * Compute yaw/pitch angles (in degrees) to look from 'from' to 'to'.
     * Yaw is in [-180..180], pitch is in [-90..90].
     */
    public static float[] anglesFromTo(Vec3d from, Vec3d to) {
        Vec3d v = to.subtract(from).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-v.x, v.z));
        float pitch = (float) Math.toDegrees(Math.asin(-v.y));
        return new float[]{yaw, pitch};
    }

    /**
     * Smoothly step from lastYaw/lastPitch to targetYaw/targetPitch over smoothTicks steps,
     * applying aimSmoothening in [0..0.9] as a final lerp factor to reduce abruptness.
     * Returns the new (yaw, pitch) after one step.
     */
    public static float[] smoothStep(float lastYaw, float lastPitch, float targetYaw, float targetPitch, int smoothTicks, double aimSmoothening) {
        if (smoothTicks <= 1) return new float[]{targetYaw, targetPitch};
        float yawDiff = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float pitchDiff = targetPitch - lastPitch;
        float stepYaw = yawDiff / smoothTicks;
        float stepPitch = pitchDiff / smoothTicks;
        float steppedYaw = lastYaw + stepYaw;
        float steppedPitch = lastPitch + stepPitch;
        double asm = Math.max(0.0, Math.min(0.9, aimSmoothening));
        double blend = 1.0 - asm;
        float sendYaw = lastYaw + MathHelper.wrapDegrees(steppedYaw - lastYaw) * (float) blend;
        float sendPitch = lastPitch + (steppedPitch - lastPitch) * (float) blend;
        return new float[]{sendYaw, sendPitch};
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
        double cx = (box.minX + box.maxX) * 0.5;
        double cy = (box.minY + box.maxY) * 0.5;
        double cz = (box.minZ + box.maxZ) * 0.5;
        double px = MathHelper.clamp(eye.x, box.minX, box.maxX);
        double py = MathHelper.clamp(eye.y, box.minY, box.maxY);
        double pz = MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        Vec3d closest = new Vec3d(px, py, pz);
        Vec3d dirToCenter = new Vec3d(Math.copySign(1e-3, cx - px), Math.copySign(1e-3, cy - py), Math.copySign(1e-3, cz - pz));
        Vec3d basePoint = clampToBox(closest.add(dirToCenter), box);

        if (!attackNow) return basePoint;

        double halfX = (box.maxX - box.minX) * 0.5;
        double halfY = (box.maxY - box.minY) * 0.5;
        double halfZ = (box.maxZ - box.minZ) * 0.5;
        double maxFrac = 0.3;
        double jMin = Math.max(0.0, Math.min(jitterMinDeg, jitterCapDegrees));
        double jMax = Math.max(jMin, Math.min(jitterMaxDeg, jitterCapDegrees));
        double jitterScale = jMax <= 0 ? 0 : Math.min(1.0, jMax / jitterCapDegrees);
        double fx = maxFrac * jitterScale;
        double fy = (maxFrac * 0.6) * jitterScale;
        double fz = maxFrac * jitterScale;

        double ox = randomSigned(0, halfX * fx);
        double oy = randomSigned(0, halfY * fy);
        double oz = randomSigned(0, halfZ * fz);

        Vec3d center = new Vec3d(cx, cy, cz);
        Vec3d jittered = center.add(ox, oy, oz);
        return clampToBox(jittered, box, 1e-3);
    }

    /**
     * Get the closest point on the target's body (feet, mid, head) to the attacker's eye position.
     * This helps minimize the distance for aiming calculations.
     */
    public static Vec3d getClosestBodyPos(LivingEntity from, LivingEntity to) {
        Vec3d eyePos = from.getEyePos();
        Vec3d toPos = to.getPos();
        Vec3d toEyePos = to.getEyePos();
        Vec3d toFeetPos = new Vec3d(toPos.x, to.getY(), toPos.z);
        Vec3d toMidPos = new Vec3d(toPos.x, to.getY() + to.getStandingEyeHeight() / 2, toPos.z);
        Vec3d toHeadPos = new Vec3d(toPos.x, toEyePos.y, toPos.z);
        Vec3d[] candidates = new Vec3d[]{toFeetPos, toMidPos, toHeadPos};
        Vec3d bestPos = toMidPos;
        double bestDist = eyePos.distanceTo(bestPos);
        for (Vec3d candidate : candidates) {
            double dist = eyePos.distanceTo(candidate);
            if (dist < bestDist) {
                bestDist = dist;
                bestPos = candidate;
            }
        }
        return bestPos;
    }

    /**
     * Compute next attack delay in milliseconds based on CPS and variance.
     * Ensures at least 35ms delay to avoid spamming.
     * Variance is a fraction [0..1] representing max +/- variation around base interval.
     */
    public static long computeNextAttackDelayMillis(int cps, double variance) {
        int safeCps = Math.max(1, cps);
        double baseInterval = 1000.0 / safeCps;
        double factor = 1.0 + ((Math.random() * 2 - 1) * variance);
        return (long) Math.max(35, baseInterval * factor);
    }

    /**
     * Compute next attack delay in milliseconds based on CPS, variance, and a minimum delay.
     * Ensures at least 'min_delay'ms delay between attacks.
     * Variance is a fraction [0..1] representing max +/- variation around base interval.
     */
    public static long computeNextAttackDelayMillis(int cps, double variance, int min_delay) {
        int safeCps = Math.max(1, cps);
        double baseInterval = 1000.0 / safeCps;
        double factor = 1.0 + ((Math.random() * 2 - 1) * variance);
        return (long) Math.max(min_delay, baseInterval * factor);
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
        ClientWorldAccessor accessor = (ClientWorldAccessor) world;
        try (PendingUpdateManager pum = accessor.getPendingUpdateManager().incrementSequence()) {
            int seq = pum.getSequence();
            player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, yaw, pitch));
        }
    }

    /**
     * Compute low-frequency sway offsets (yaw, pitch) from a wave phase and amplitude.
     */
    public static double[] computeSway(double wavePhase, double swayAmp) {
        double swayYaw = Math.sin(wavePhase) * swayAmp;
        double swayPitch = Math.cos(wavePhase * 0.9) * (swayAmp * 0.6);
        return new double[]{swayYaw, swayPitch};
    }

    /**
     * Interpolate 'current' towards 'target' using aimSmoothening in [0..0.9] as the lerp factor.
     */
    public static double interpolateTowards(double current, double target, double aimSmoothening) {
        double asm = Math.max(0.0, Math.min(0.9, aimSmoothening));
        return current + (target - current) * asm;
    }

    /**
     * Combine jitter, sway, micro-noise and aim-offset into final yaw/pitch offsets. Optionally cap when attacking.
     */
    public static double[] combineOffsets(double jitterYaw, double jitterPitch,
                                          double swayYaw, double swayPitch,
                                          double microYaw, double microPitch,
                                          double aimOffYaw, double aimOffPitch,
                                          boolean attackNow, double attackCap) {
        double combinedYaw = jitterYaw + swayYaw + microYaw + aimOffYaw;
        double combinedPitch = jitterPitch + swayPitch + microPitch + aimOffPitch;
        if (attackNow) {
            double cap = Math.max(0.0, attackCap);
            combinedYaw = Math.max(-cap, Math.min(cap, combinedYaw));
            combinedPitch = Math.max(-cap, Math.min(cap, combinedPitch));
        }
        return new double[]{combinedYaw, combinedPitch};
    }

    /**
     * Send a PlayerInteractEntityC2SPacket attack and swing the player's hand.
     */
    public static void sendEntityAttack(ClientPlayerEntity player, LivingEntity target, boolean sneaking) {
        if (player == null || target == null) return;
        player.networkHandler.sendPacket(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.attack(target, sneaking));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.HandSwingC2SPacket(net.minecraft.util.Hand.MAIN_HAND));
    }

    /**
     * Send a look packet followed by an attack and a hand swing. Useful for revive/auras that need to set look client-side.
     */
    public static void lookAndAttack(ClientWorld world, ClientPlayerEntity player, LivingEntity target, float yaw, float pitch) {
        if (player == null || target == null || world == null) return;
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
        player.networkHandler.sendPacket(net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.attack(target, player.isSneaking()));
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.HandSwingC2SPacket(net.minecraft.util.Hand.MAIN_HAND));
    }
}
