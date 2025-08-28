package dev.cigarette.module.combat;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.WeaponHelper;
import dev.cigarette.lib.*;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerAimbot extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerAimbot INSTANCE = new PlayerAimbot("combat.playerAimbot", "PlayerAimbot", "Automatically aims at players.");

//    private final ToggleWidget silentAim = new ToggleWidget("Silent Aim", "Doesn't snap your camera client-side.").withDefaultState(true);
    private final ToggleWidget autoAttack = new ToggleWidget("Auto Attack", "Automatically hits players").withDefaultState(true);
    private final ToggleWidget autoWeaponSwitch = new ToggleWidget("Auto Weapon Switch", "Automatically switch weapons").withDefaultState(true);
    public final SliderWidget smoothAim = new SliderWidget("Smooth Aim", "How quickly to snap to target in ticks").withBounds(1, 5, 20);
    public final ToggleWidget wTap = new ToggleWidget("W-Tap", "Automatically sprint before attacking").withDefaultState(false);
    public final SliderWidget.TwoHandedSlider jitterAmount = new SliderWidget.TwoHandedSlider("Jitter", "Random jitter range (min/max) degrees").withBounds(0, 0, 4).withAccuracy(2);
    public final SliderWidget jitterSpeed = new SliderWidget("Jitter Speed", "How fast jitter target changes (ticks)").withBounds(5, 10, 40);
//    private final ToggleWidget blockHit = new ToggleWidget("Block-Hit", "Briefly block just before attacking").withDefaultState(false);
    private final ToggleWidget testMode = new ToggleWidget("Test Mode", "Allows targeting villagers regardless of team").withDefaultState(false);
    public final SliderWidget attackCps = new SliderWidget("Attack CPS", "Clicks per second for auto attack").withBounds(1, 8, 15);

    private KeyBinding rightClickKey = null;

    private boolean hasLastAim = false;
    private float lastAimYaw = 0f;
    private float lastAimPitch = 0f;

    private long nextAttackAtMs = 0L;
    private static final double ATTACK_VARIANCE = 0.15;
    private static final double JITTER_CAP_DEGREES = 2.0;

    private PlayerAimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(autoAttack, autoWeaponSwitch, smoothAim, jitterAmount, jitterSpeed, wTap, testMode, attackCps);
        autoAttack.registerConfigKey(id + ".autoAttack");
        autoWeaponSwitch.registerConfigKey(id + ".autoWeaponSwitch");
        smoothAim.registerConfigKey(id + ".smoothAim");
        wTap.registerConfigKey(id + ".wTap");
        jitterAmount.registerConfigKey(id + ".jitter");
        jitterSpeed.registerConfigKey(id + ".jitterSpeed");
//        blockHit.registerConfigKey(id + ".blockHit");
        testMode.registerConfigKey(id + ".testMode");
        attackCps.registerConfigKey(id + ".attackCps");
    }

    private static Vec3d getClosestBodyPos(LivingEntity from, LivingEntity to) {
        Vec3d eyePos = from.getEyePos();
        Vec3d toPos = to.getPos();
        Vec3d toEyePos = to.getEyePos();
        Vec3d toFeetPos = new Vec3d(toPos.x, to.getY(), toPos.z);
        Vec3d toMidPos = new Vec3d(toPos.x, to.getY() + to.getStandingEyeHeight() / 2, toPos.z);
        Vec3d toHeadPos = new Vec3d(toPos.x, toEyePos.y, toPos.z);
        Vec3d[] candidates = new Vec3d[] {toFeetPos, toMidPos, toHeadPos};
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

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        if (MinecraftClient.getInstance().currentScreen != null) return;

        boolean active = rightClickKey.isPressed() || autoAttack.getRawState();
        if (active) {
            LivingEntity bestTarget = testMode.getRawState() ? getBestEntityTargetFor(player) : getBestTargetFor(player);
            if (bestTarget == null) {
                hasLastAim = false;
                return;
            }

            if (autoWeaponSwitch.getRawState()) {
                double dist = player.distanceTo(bestTarget);
                switchToBestPvPWeapon(player, dist);
            }

            boolean isRanged = WeaponHelper.isRanged(player.getMainHandStack());
            boolean shouldAttemptMelee = autoAttack.getRawState() && !isRanged;
            long now = System.currentTimeMillis();
            boolean attackNow = shouldAttemptMelee && now >= nextAttackAtMs;
            if (attackNow) scheduleNextAttack(now);

            Vec3d aimPoint = getAimPointInsideHitbox(player, bestTarget, attackNow);
            double reach = getMeleeReach(player);
            boolean inReach = player.getEyePos().squaredDistanceTo(aimPoint) <= (reach * reach) + 1e-6;

            Vec3d vector = aimPoint.subtract(player.getEyePos()).normalize();
            float targetYaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
            float targetPitch = (float) Math.toDegrees(Math.asin(-vector.y));

            int smoothTicks = smoothAim.getRawState().intValue();
            float sendYaw;
            float sendPitch;
            if (attackNow) {
                sendYaw = targetYaw;
                sendPitch = targetPitch;
                hasLastAim = true;
            } else {
                if (!hasLastAim) {
                    lastAimYaw = targetYaw;
                    lastAimPitch = targetPitch;
                    hasLastAim = true;
                }
                if (smoothTicks <= 1) {
                    sendYaw = targetYaw;
                    sendPitch = targetPitch;
                } else {
                    float yawDiff = MathHelper.wrapDegrees(targetYaw - lastAimYaw);
                    float pitchDiff = targetPitch - lastAimPitch;
                    float stepYaw = yawDiff / smoothTicks;
                    float stepPitch = pitchDiff / smoothTicks;
                    sendYaw = lastAimYaw + stepYaw;
                    sendPitch = lastAimPitch + stepPitch;
                }
            }
            lastAimYaw = sendYaw;
            lastAimPitch = sendPitch;

            if (isRanged) {
                player.setYaw(sendYaw);
                player.setPitch(sendPitch);
                player.swingHand(Hand.MAIN_HAND);
            } else {
                if (attackNow && inReach) {
                    if (wTap.getRawState()) {
                        doSprint(false);
                        doSprint(true);
                    }
                    player.setYaw(sendYaw);
                    player.setPitch(sendPitch);

                    player.swingHand(Hand.MAIN_HAND);
                    player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(bestTarget, player.isSneaking()));
                } else {
                    player.setYaw(sendYaw);
                    player.setPitch(sendPitch);
                }
            }
        } else {
            hasLastAim = false;
        }
    }

    private Vec3d getAimPointInsideHitbox(ClientPlayerEntity player, LivingEntity target, boolean attackNow) {
        Box box = target.getBoundingBox();
        Vec3d eye = player.getEyePos();
        double cx = (box.minX + box.maxX) * 0.5; double cy = (box.minY + box.maxY) * 0.5; double cz = (box.minZ + box.maxZ) * 0.5;
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
        double jMin = Math.max(0.0, Math.min(jitterAmount.getMinValue(), JITTER_CAP_DEGREES));
        double jMax = Math.max(jMin, Math.min(jitterAmount.getMaxValue(), JITTER_CAP_DEGREES));
        double jitterScale = jMax <= 0 ? 0 : Math.min(1.0, jMax / JITTER_CAP_DEGREES);
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

    private static Vec3d clampToBox(Vec3d p, Box box) {
        return clampToBox(p, box, 0);
    }

    private static Vec3d clampToBox(Vec3d p, Box box, double inset) {
        double minX = box.minX + inset, minY = box.minY + inset, minZ = box.minZ + inset;
        double maxX = box.maxX - inset, maxY = box.maxY - inset, maxZ = box.maxZ - inset;
        return new Vec3d(
            MathHelper.clamp(p.x, minX, maxX),
            MathHelper.clamp(p.y, minY, maxY),
            MathHelper.clamp(p.z, minZ, maxZ)
        );
    }

    private static double getMeleeReach(ClientPlayerEntity player) {
        return player.getAbilities().creativeMode ? 5.0 : 3.0;
    }

    public static AbstractClientPlayerEntity getBestTargetFor(ClientPlayerEntity player) {
        return WorldL.getRealPlayers().stream().sorted((p1, p2) -> {
            Vec3d eyePos = player.getEyePos();
            Vec3d p1Pos = p1.getEyePos().subtract(eyePos);
            Vec3d p2Pos = p2.getEyePos().subtract(eyePos);
            double p1Dist = p1Pos.lengthSquared();
            double p2Dist = p2Pos.lengthSquared();

            if (p1.isSneaking() && !p2.isSneaking()) p1Dist *= 0.8;
            else if (!p1.isSneaking() && p2.isSneaking()) p2Dist *= 0.8;

            if (p1.getPose() == EntityPose.CROUCHING && p2.getPose() != EntityPose.CROUCHING) p1Dist *= 0.9;
            else if (p1.getPose() != EntityPose.CROUCHING && p2.getPose() == EntityPose.CROUCHING) p2Dist *= 0.9;

            if (p1.isSprinting() && !p2.isSprinting()) p1Dist *= 1.1;
            else if (!p1.isSprinting() && p2.isSprinting()) p2Dist *= 1.1;
            if (p1.getVelocity().lengthSquared() > 0.01 && p2.getVelocity().lengthSquared() < 0.01) p1Dist *= 1.1;
            else if (p1.getVelocity().lengthSquared() < 0.01 && p2.getVelocity().lengthSquared() > 0.01) p2Dist *= 1.1;
            return Double.compare(p1Dist, p2Dist);
        }).filter(p -> {
            if (p == player) return false;
            if (!INSTANCE.testMode.getRawState() && (player.isTeammate(p) || ServerL.playerOnSameTeam(player, p))) return false;
            if (p.isDead() || p.getHealth() <= 0) return false;
            if (p.isInvulnerable()) return false;
            if (p.age < 20) return false;
            if (p.distanceTo(player) > 6) return false;
            return player.canSee(p);
        }).findFirst().orElse(null);
    }

    public static LivingEntity getBestEntityTargetFor(ClientPlayerEntity player) {
        ClientWorld world = (ClientWorld) player.getWorld();
        Vec3d eyePos = player.getEyePos();
        double maxRange = 6.0;
        Box search = player.getBoundingBox().expand(maxRange + 1.0);

        List<AbstractClientPlayerEntity> players = WorldL.getRealPlayers();
        List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, search,
                v -> v.isAlive() && v.distanceTo(player) <= maxRange);
        List<WanderingTraderEntity> traders = world.getEntitiesByClass(WanderingTraderEntity.class, search,
                t -> t.isAlive() && t.distanceTo(player) <= maxRange);
        List<ZombieVillagerEntity> zombieVillagers = world.getEntitiesByClass(ZombieVillagerEntity.class, search,
                z -> z.isAlive() && z.distanceTo(player) <= maxRange);

        List<LivingEntity> villagerLike = new ArrayList<>();
        villagerLike.addAll(villagers);
        villagerLike.addAll(traders);
        villagerLike.addAll(zombieVillagers);

        if (!villagerLike.isEmpty()) {
            villagerLike.sort(Comparator.comparingDouble(e -> eyePos.squaredDistanceTo(e.getEyePos())));
            return villagerLike.get(0);
        }

        List<LivingEntity> candidates = new ArrayList<>();
        for (AbstractClientPlayerEntity p : players) {
            if (p == player) continue;
            if (player.isTeammate(p) || ServerL.playerOnSameTeam(player, p)) continue;
            if (p.isDead() || p.getHealth() <= 0) continue;
            if (p.isInvulnerable()) continue;
            if (p.age < 20) continue;
            if (p.distanceTo(player) > maxRange) continue;
            if (!player.canSee(p)) continue;
            candidates.add(p);
        }
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(e -> eyePos.squaredDistanceTo(e.getEyePos())));
        return candidates.get(0);
    }

    private double randomSigned(double min, double max) {
        if (max <= 0) return 0;
        double magnitude = min + (Math.random() * (max - min));
        return (Math.random() < 0.5 ? -1 : 1) * magnitude;
    }
    
    private static void doSprint(boolean sprint) {
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

    private void scheduleNextAttack(long nowMs) {
        int cps = Math.max(1, attackCps.getRawState().intValue());
        double baseInterval = 1000.0 / cps;
        double factor = 1.0 + ((Math.random() * 2 - 1) * ATTACK_VARIANCE);
        long delay = (long) Math.max(35, baseInterval * factor);
        nextAttackAtMs = nowMs + delay;
    }

    public static void playerBlockWithSword(ClientPlayerEntity player) {
        player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, 1,
                player.getYaw(), player.getPitch()));
    }

    /**
     * Switch to the best PvP weapon for players. For close range, prefer melee (best sword/axe in hotbar).
     * For long range, prefer bow/crossbow if present.
     * Returns true if a switch was made or the best weapon is already selected.
     */
    public static boolean switchToBestPvPWeapon(ClientPlayerEntity player, double distanceToTarget) {
        if (player == null) return false;
        int current = player.getInventory().getSelectedSlot();
        int bestSlot;
        if (distanceToTarget > 7.5) {
            bestSlot = WeaponHelper.getBestRangedSlot(player);
            if (bestSlot == -1) bestSlot = WeaponHelper.getBestMeleeSlot(player);
        } else {
            bestSlot = WeaponHelper.getBestMeleeSlot(player);
            if (bestSlot == -1) bestSlot = WeaponHelper.getBestRangedSlot(player);
        }
        if (bestSlot == -1) return false;
        if (bestSlot == current) return true;
        player.getInventory().setSelectedSlot(bestSlot);
        return true;
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame != GameDetector.ParentGame.ZOMBIES;
    }
}
