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
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
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
    private final SliderWidget aimSmoothening = new SliderWidget("Aim Smoothing", "How much to smooth/interpolate between steps of the smooth aim").withBounds(0, 0.05, 0.9).withAccuracy(3);
    public final SliderWidget.TwoHandedSlider aimOffset = new SliderWidget.TwoHandedSlider("Aim Offset", "Aim target offset (min/max) degrees").withBounds(0, 0, 3).withAccuracy(2);
    public final SliderWidget swayAmount = new SliderWidget("Sway Amount", "Continuous sway amplitude (degrees)").withBounds(0, 0.0, 1.5).withAccuracy(2);
    public final SliderWidget swaySpeed = new SliderWidget("Sway Speed", "How fast the sway oscillates (ticks)").withBounds(20, 40, 200);
    private KeyBinding rightClickKey = null;

    private boolean hasLastAim = false;
    private float lastAimYaw = 0f;
    private float lastAimPitch = 0f;

    private int tickCount = 0;
    private double currentJitterYaw = 0.0, currentJitterPitch = 0.0;
    private double nextJitterYaw = 0.0, nextJitterPitch = 0.0;
    private double wavePhase = 0.0;

    private long nextAttackAtMs = 0L;
    private static final double ATTACK_VARIANCE = 0.15;
    private static final double JITTER_CAP_DEGREES = 2.0;

    private PlayerAimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(autoAttack, autoWeaponSwitch, smoothAim, aimSmoothening, jitterAmount, jitterSpeed, aimOffset, swayAmount, swaySpeed, wTap, testMode, attackCps);
        autoAttack.registerConfigKey(id + ".autoAttack");
        autoWeaponSwitch.registerConfigKey(id + ".autoWeaponSwitch");
        smoothAim.registerConfigKey(id + ".smoothAim");
        aimSmoothening.registerConfigKey(id + ".aimSmoothening");
        aimOffset.registerConfigKey(id + ".aimOffset");
        swayAmount.registerConfigKey(id + ".swayAmount");
        swaySpeed.registerConfigKey(id + ".swaySpeed");
        wTap.registerConfigKey(id + ".wTap");
        jitterAmount.registerConfigKey(id + ".jitter");
        jitterSpeed.registerConfigKey(id + ".jitterSpeed");
//        blockHit.registerConfigKey(id + ".blockHit");
        testMode.registerConfigKey(id + ".testMode");
        attackCps.registerConfigKey(id + ".attackCps");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        tickCount++;
        double swaySpd = Math.max(1.0, swaySpeed.getRawState().doubleValue());
        wavePhase += (2.0 * Math.PI) / swaySpd;

        int jitterTicks = Math.max(1, jitterSpeed.getRawState().intValue());
        if (tickCount % jitterTicks == 0) {
            double jMin = Math.max(0.0, Math.min(jitterAmount.getMinValue(), JITTER_CAP_DEGREES));
            double jMax = Math.max(jMin, Math.min(jitterAmount.getMaxValue(), JITTER_CAP_DEGREES));
            nextJitterYaw = AimingL.randomSigned(jMin, jMax);
            nextJitterPitch = AimingL.randomSigned(jMin * 0.6, jMax * 0.6); // pitch jitter typically smaller
        }
        // interpolate jitter targets towards the next jitter using AimingL
        double asmVal = aimSmoothening.getRawState().doubleValue();
        currentJitterYaw = AimingL.interpolateTowards(currentJitterYaw, nextJitterYaw, asmVal);
        currentJitterPitch = AimingL.interpolateTowards(currentJitterPitch, nextJitterPitch, asmVal);

        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        if (MinecraftClient.getInstance().currentScreen != null) return;

        if(!rightClickKey.isPressed() && !autoAttack.getRawState()) {
            hasLastAim = false;
            return;
        }

        LivingEntity bestTarget = testMode.getRawState() ? getBestEntityTargetFor(player) : getBestTargetFor(player);
        if (bestTarget == null) {
            hasLastAim = false;
            return;
        }

        if (autoWeaponSwitch.getRawState()) {
            double dist = player.distanceTo(bestTarget);
            AimingL.switchToBestPvPWeapon(player, dist);
        }

        boolean isRanged = WeaponHelper.isRanged(player.getMainHandStack());
        boolean shouldAttemptMelee = autoAttack.getRawState() && !isRanged;
        long now = System.currentTimeMillis();
        boolean attackNow = shouldAttemptMelee && now >= nextAttackAtMs;
        if (attackNow) nextAttackAtMs = now + AimingL.computeNextAttackDelayMillis(attackCps.getRawState().intValue(), ATTACK_VARIANCE);

        Vec3d aimPoint = AimingL.getAimPointInsideHitbox(player, bestTarget, attackNow,
                jitterAmount.getMinValue(), jitterAmount.getMaxValue(), JITTER_CAP_DEGREES);
        double reach = getMeleeReach(player);
        boolean inReach = player.getEyePos().squaredDistanceTo(aimPoint) <= (reach * reach) + 1e-6;

        float[] angles = AimingL.anglesFromTo(player.getEyePos(), aimPoint);
        float targetYaw = angles[0];
        float targetPitch = angles[1];

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
                float[] sm = AimingL.smoothStep(lastAimYaw, lastAimPitch, targetYaw, targetPitch, smoothTicks, aimSmoothening.getRawState().doubleValue());
                sendYaw = sm[0];
                sendPitch = sm[1];
            }
        }
        lastAimYaw = sendYaw;
        lastAimPitch = sendPitch;

        double swayAmp = swayAmount.getRawState().doubleValue();
        double[] sway = AimingL.computeSway(wavePhase, swayAmp);
        double swayYaw = sway[0];
        double swayPitch = sway[1];
        double microNoiseYaw = AimingL.randomSigned(0.0, 0.02); // tiny micro movements
        double microNoisePitch = AimingL.randomSigned(0.0, 0.02);

        double aoMin = Math.max(0.0, Math.min(aimOffset.getMinValue(), 5.0));
        double aoMax = Math.max(aoMin, Math.min(aimOffset.getMaxValue(), 5.0));
        double aimOffYaw = AimingL.randomSigned(aoMin, aoMax);
        double aimOffPitch = AimingL.randomSigned(aoMin * 0.6, aoMax * 0.6);

        double attackCap = Math.max(0.5, Math.min(2.0, aimOffset.getMaxValue()));
        double[] combined = AimingL.combineOffsets(currentJitterYaw, currentJitterPitch,
                swayYaw, swayPitch,
                microNoiseYaw, microNoisePitch,
                aimOffYaw, aimOffPitch,
                attackNow, attackCap);
        sendYaw = (float) (sendYaw + combined[0]);
        sendPitch = (float) (sendPitch + combined[1]);

        if (isRanged) {
            player.setYaw(sendYaw);
            player.setPitch(sendPitch);
            player.swingHand(Hand.MAIN_HAND);
        } else {
            if (attackNow && inReach) {
                if (wTap.getRawState()) {
                    AimingL.doSprint(false);
                    AimingL.doSprint(true);
                }
                player.setYaw(sendYaw);
                player.setPitch(sendPitch);

                // centralized attack + swing
                AimingL.sendEntityAttack(player, bestTarget, player.isSneaking());
            } else {
                player.setYaw(sendYaw);
                player.setPitch(sendPitch);
            }
        }
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



    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame != GameDetector.ParentGame.ZOMBIES;
    }
}
