package dev.cigarette.module.combat;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.gui.widget.ToggleKeybindWidget;
import dev.cigarette.lib.AimingL;
import dev.cigarette.lib.ServerL;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

public class PlayerAimbot extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerAimbot INSTANCE = new PlayerAimbot("combat.playerAimbot", "PlayerAimbot", "Automatically aims at players. The legitest killaura.");

    private final ToggleWidget autoAttack = new ToggleWidget("Auto Attack", "Automatically hits players").withDefaultState(true);
    public final SliderWidget smoothAim = new SliderWidget("Smooth Aim", "How quickly to snap to target in ticks").withBounds(1, 5, 20);
    public final ToggleWidget wTap = new ToggleWidget("W-Tap", "Automatically sprint before attacking").withDefaultState(false);
    private final ToggleWidget testMode = new ToggleWidget("Test Mode", "Allows targeting villagers regardless of team").withDefaultState(false);

    public final SliderWidget attackCps = new SliderWidget("Attack CPS", "Clicks per second for auto attack").withBounds(1, 8, 15);

    public final ToggleWidget ignoreTeammates = new ToggleWidget("Ignore Teammates", "Don't target players on your team").withDefaultState(true);

    private final ToggleKeybindWidget lockOnKeybind = new ToggleKeybindWidget(
            "Lock-On Trigger", "Press key to lock to best target. Releases only when target dies"
    ).withDefaultState(false);

    private final ToggleWidget murderMysteryMode = new ToggleWidget("Murder Mystery", "Prefer murderer and detective targets").withDefaultState(false);
    private final ToggleWidget detectiveAim = new ToggleWidget("Detective Aim", "Aim at a detective when no Murderer is available (or you are the mur)").withDefaultState(true);

    public final SliderWidget jitterViolence = new SliderWidget("Jitter Aggression", "Scale of aim jitter (0 disables)").withBounds(0.0, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget driftViolence = new SliderWidget("Drift Aggression", "Scale of slow drift vs jitter").withBounds(0.0, 0.6, 2.0).withAccuracy(2);
    public final SliderWidget aimToleranceDeg = new SliderWidget("Aim Tolerance (deg)", "Max degrees off for attack").withBounds(0.5, 2.0, 6.0).withAccuracy(1);

    public final SliderWidget smoothingMultiplier = new SliderWidget("Smoothing Multiplier", "Scales duration based on angle").withBounds(0.5, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget bezierInfluence = new SliderWidget("Bezier Influence", "0 = linear, 1 = full bezier curve").withBounds(0.0, 1.0, 1.0).withAccuracy(2);
    public final SliderWidget controlJitterScale = new SliderWidget("Control Jitter", "Randomness of control points").withBounds(0.0, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget interpolationMode = new SliderWidget("Interpolation Mode", "0=Linear 1=Cubic 2=Cos 3=Smoothstep").withBounds(0, 1, 3).withAccuracy(0);

    public final SliderWidget interferenceAngleDeg = new SliderWidget("Interference Angle (deg)", "Delta that triggers pause").withBounds(2.0, 6.0, 20.0).withAccuracy(1);
    public final SliderWidget interferenceGraceTicks = new SliderWidget("Interference Grace (ticks)", "Pause length while moving").withBounds(2, 8, 30).withAccuracy(0);

    public final SliderWidget aimRange = new SliderWidget("Aim Range", "Maximum range to target players").withBounds(3, 6, 20).withAccuracy(1);

    private PlayerAimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(autoAttack, smoothAim, aimRange, wTap, attackCps, ignoreTeammates, lockOnKeybind, testMode,
                murderMysteryMode, detectiveAim,
                jitterViolence, driftViolence, aimToleranceDeg, smoothingMultiplier, bezierInfluence, controlJitterScale, interpolationMode,
                interferenceAngleDeg, interferenceGraceTicks);
        autoAttack.registerConfigKey(id + ".autoAttack");
        smoothAim.registerConfigKey(id + ".smoothAim");
        aimRange.registerConfigKey(id + ".aimRange");
        wTap.registerConfigKey(id + ".wTap");
        attackCps.registerConfigKey(id + ".attackCps");
        ignoreTeammates.registerConfigKey(id + ".ignoreTeammates");
        lockOnKeybind.registerConfigKey(id + ".lockOnKeybind");
        testMode.registerConfigKey(id + ".testMode");
        murderMysteryMode.registerConfigKey(id + ".murderMysteryMode");
        detectiveAim.registerConfigKey(id + ".detectiveAim");
        jitterViolence.registerConfigKey(id + ".jitterViolence");
        driftViolence.registerConfigKey(id + ".driftViolence");
        aimToleranceDeg.registerConfigKey(id + ".aimToleranceDeg");
        smoothingMultiplier.registerConfigKey(id + ".smoothingMultiplier");
        bezierInfluence.registerConfigKey(id + ".bezierInfluence");
        controlJitterScale.registerConfigKey(id + ".controlJitterScale");
        interpolationMode.registerConfigKey(id + ".interpolationMode");
        interferenceAngleDeg.registerConfigKey(id + ".interferenceAngleDeg");
        interferenceGraceTicks.registerConfigKey(id + ".interferenceGraceTicks");
    }

    // Bezier aim plan state
    private @Nullable LivingEntity activeTarget = null;
    private @Nullable UUID activeTargetId = null;
    private int planTicksTotal = 0;
    private int planTicksElapsed = 0;
    private float startYaw = 0f, startPitch = 0f;
    private float c1Yaw = 0f, c1Pitch = 0f;
    private float c2Yaw = 0f, c2Pitch = 0f;
    private float endYaw = 0f, endPitch = 0f;
    private final Random rng = new Random();

    // CPS scheduling
    private int ticksUntilNextAttack = 0;

    // Reach assumption (entityAttackRange not exposed here)
    private static final double MELEE_REACH = 3.2;

    private int suppressTicks = 0;
    private boolean lastAppliedValid = false;
    private float lastAppliedYaw = 0f, lastAppliedPitch = 0f;

    private boolean lockOnEngaged = false;

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (MinecraftClient.getInstance().currentScreen != null) return;

        if (!lockOnKeybind.getRawState() && lockOnEngaged) {
            lockOnEngaged = false;
            clearPlan();
        }
        boolean keyPressedEvent = lockOnKeybind.getKeybind().wasPressed();
        boolean keyHeld = lockOnKeybind.getKeybind().isPressed();
        LivingEntity target;
        if (keyPressedEvent || keyHeld) {
            if (lockOnKeybind.getRawState() && lockOnKeybind.getKeybind().wasPressed()) {
                AbstractClientPlayerEntity best = getBestTargetFor(player);
                if (best != null) {
                    lockOnEngaged = true;
                    activeTarget = best;
                    activeTargetId = best.getUuid();
                    planTicksTotal = 0;
                    planTicksElapsed = 0;
                }
            }

            target = null;
            if (handleInterferenceSuppression(player)) return;

            if (lockOnKeybind.getRawState()) {
                if (lockOnEngaged) {
                    if (activeTarget != null && activeTarget.isAlive() && !activeTarget.isRemoved()
                            && player.squaredDistanceTo(activeTarget) <= Math.pow(aimRange.getRawState(), 2)) {
                        target = activeTarget;
                    } else {
                        lockOnEngaged = false;
                        clearPlan();
                        return;
                    }
                } else {
                    clearPlan();
                    return;
                }
            } else {
                target = pickOrValidateTarget(world, player);
            }

            if (target == null) {
                clearPlan();
                return;
            }
        } else {
            target = pickOrValidateTarget(world, player);
        }

        if (target == null) {
            clearPlan();
            return;
        }

        Vec3d aimPoint = computeAimPoint(player, target, false);
        float[] targetAngles = AimingL.anglesFromTo(player.getEyePos(), aimPoint);
        float curYaw = getContinuousYaw(player);
        float curPitch = MathHelper.clamp(player.getPitch(), -90f, 90f);

        if (!isPlanActive() || !Objects.equals(target.getUuid(), activeTargetId) || planTargetChangedSignificantly(targetAngles)) {
            buildBezierPlan(curYaw, curPitch, targetAngles[0], targetAngles[1]);
            activeTarget = target;
            activeTargetId = target.getUuid();
        }

        float[] stepAngles = evalPlanStep();
        player.setYaw(stepAngles[0]);
        player.setPitch(stepAngles[1]);
        lastAppliedYaw = stepAngles[0];
        lastAppliedPitch = stepAngles[1];
        lastAppliedValid = true;

        if (autoAttack.getRawState()) {
            if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;
            boolean withinRange = player.distanceTo(target) <= MELEE_REACH;
            boolean aimAligned = aimAlignmentOk(player, target);
            if (withinRange && aimAligned && ticksUntilNextAttack <= 0) {
                if (wTap.getRawState()) AimingL.doSprint(true);
                Vec3d atkAim = computeAimPoint(player, target, true);
                float[] atkAngles = AimingL.anglesFromTo(player.getEyePos(), atkAim);
                AimingL.lookAndAttack(world, player, target, atkAngles[0], atkAngles[1]);
                scheduleNextAttack();
            }
        }
    }

    private boolean handleInterferenceSuppression(ClientPlayerEntity player) {
        float yaw = getContinuousYaw(player);
        float pitch = player.getPitch();
        float angleDelta = Math.max(Math.abs(yaw - lastAppliedYaw), Math.abs(pitch - lastAppliedPitch));
        float trigger = (float) Math.max(0.5, interferenceAngleDeg.getRawState());

        if (suppressTicks > 0) {
            if (angleDelta > trigger) {
                suppressTicks = (int) Math.max(1, Math.round(interferenceGraceTicks.getRawState()));
            } else {
                suppressTicks--;
            }
            lastAppliedYaw = yaw;
            lastAppliedPitch = pitch;
            lastAppliedValid = true;
            return true;
        }

        if (lastAppliedValid && angleDelta > trigger) {
            suppressTicks = (int) Math.max(1, Math.round(interferenceGraceTicks.getRawState()));
            lastAppliedYaw = yaw;
            lastAppliedPitch = pitch;
            return true;
        }
        return false;
    }

    private void scheduleNextAttack() {
        double cps = Math.max(1.0, attackCps.getRawState());
        double idealTicks = 20.0 / cps;
        double jitter = idealTicks * (0.15 + rng.nextDouble() * 0.20);
        ticksUntilNextAttack = (int) Math.max(1, Math.round(idealTicks + (rng.nextBoolean() ? jitter : -jitter * 0.5)));
    }

    private boolean aimAlignmentOk(ClientPlayerEntity player, LivingEntity target) {
        Vec3d aimPoint = computeAimPoint(player, target, false);
        float[] want = AimingL.anglesFromTo(player.getEyePos(), aimPoint);
        float dyaw = MathHelper.wrapDegrees(player.getYaw() - want[0]);
        float dpitch = want[1] - player.getPitch();
        float tol = (float) Math.max(0.1, aimToleranceDeg.getRawState());
        return Math.abs(dyaw) < tol && Math.abs(dpitch) < tol;
    }

    private float[] evalPlanStep() {
        if (!isPlanActive()) return new float[]{startYaw, startPitch};
        planTicksElapsed = Math.min(planTicksElapsed + 1, planTicksTotal);
        float t = planTicksTotal == 0 ? 1f : (float) planTicksElapsed / (float) planTicksTotal;
        float et = applyInterpolation(t);
        float linYaw = startYaw + (endYaw - startYaw) * et;
        float linPitch = startPitch + (endPitch - startPitch) * et;
        float bezYaw = cubicBezier(startYaw, c1Yaw, c2Yaw, endYaw, et);
        float bezPitch = cubicBezier(startPitch, c1Pitch, c2Pitch, endPitch, et);
        float inf = (float) Math.max(0.0, Math.min(1.0, bezierInfluence.getRawState()));
        float yaw = linYaw + (bezYaw - linYaw) * inf;
        float pitch = linPitch + (bezPitch - linPitch) * inf;

        float angleSpan = Math.abs(MathHelper.wrapDegrees(startYaw - endYaw)) + Math.abs(endPitch - startPitch);
        float baseJitter = Math.max(0.0f, Math.min(0.5f, angleSpan * 0.003f));
        float jitterMag = (float) (baseJitter * Math.max(0.0, jitterViolence.getRawState()));
        float driftMag = (float) (jitterMag * Math.max(0.0, driftViolence.getRawState()));
        float fineJitterYaw = (float) (rng.nextGaussian() * jitterMag * (1.0 - et));
        float fineJitterPitch = (float) (rng.nextGaussian() * jitterMag * (1.0 - et));
        float driftYaw = (float) (rng.nextGaussian() * driftMag * 0.2);
        float driftPitch = (float) (rng.nextGaussian() * driftMag * 0.2);

        float outYaw = yaw + fineJitterYaw + driftYaw;
        float outPitch = MathHelper.clamp(pitch + fineJitterPitch + driftPitch, -90f, 90f);
        return new float[]{outYaw, outPitch};
    }

    private void buildBezierPlan(float curYaw, float curPitch, float targetYaw, float targetPitch) {
        float tYaw = unwrapTowards(targetYaw, curYaw);
        startYaw = curYaw;
        startPitch = curPitch;
        endYaw = tYaw;
        endPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        float yawDelta = tYaw - curYaw;
        float pitchDelta = endPitch - curPitch;
        float angleMag = Math.abs(yawDelta) + Math.abs(pitchDelta);

        int baseTicks = (int) Math.max(1, Math.round(smoothAim.getRawState()));
        double mult = Math.max(0.1, smoothingMultiplier.getRawState());
        planTicksTotal = (int) Math.max(baseTicks, Math.min(baseTicks * 4L, Math.round(baseTicks * mult * (0.5 + angleMag / 45f))));
        planTicksElapsed = 0;

        float c1t = 0.30f + (float) rng.nextDouble() * 0.15f;
        float c2t = 0.70f - (float) rng.nextDouble() * 0.15f;
        float ctrlJitter = (float) (Math.max(0.0f, Math.min(3.5f, angleMag * 0.05f)) * Math.max(0.0, controlJitterScale.getRawState()));
        c1Yaw = curYaw + yawDelta * c1t + (float) (rng.nextGaussian() * ctrlJitter);
        c2Yaw = curYaw + yawDelta * c2t + (float) (rng.nextGaussian() * ctrlJitter);
        c1Pitch = curPitch + pitchDelta * c1t + (float) (rng.nextGaussian() * ctrlJitter * 0.6f);
        c2Pitch = curPitch + pitchDelta * c2t + (float) (rng.nextGaussian() * ctrlJitter * 0.6f);

        c1Yaw = unwrapTowards(c1Yaw, startYaw);
        c2Yaw = unwrapTowards(c2Yaw, startYaw);
        endYaw = unwrapTowards(endYaw, startYaw);
    }

    private float applyInterpolation(float t) {
        int mode = (int) Math.round(interpolationMode.getRawState());
        switch (mode) {
            case 0: // Linear
                return t;
            case 1: // EaseInOutCubic
                return easeInOutCubic(t);
            case 2: // Cosine
                return (float) (0.5 - 0.5 * Math.cos(Math.PI * t));
            case 3: // Smoothstep
                return t * t * (3f - 2f * t);
            default:
                return t;
        }
    }

    private boolean isPlanActive() { return planTicksElapsed < planTicksTotal; }

    private boolean planTargetChangedSignificantly(float[] targetAngles) {
        float dyaw = Math.abs(MathHelper.wrapDegrees(endYaw - targetAngles[0]));
        float dpitch = Math.abs(endPitch - targetAngles[1]);
        return dyaw > 3.0f || dpitch > 3.0f;
    }

    private void clearPlan() {
        activeTarget = null;
        activeTargetId = null;
        planTicksTotal = 0;
        planTicksElapsed = 0;
    }

    private static float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u*u*u*p0 + 3*u*u*t*p1 + 3*u*t*t*p2 + t*t*t*p3;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    private static float unwrapTowards(float target, float reference) {
        float t = target;
        while (t - reference > 180f) t -= 360f;
        while (t - reference < -180f) t += 360f;
        return t;
    }

    private float getContinuousYaw(ClientPlayerEntity player) {
        float wrappedCurrent = MathHelper.wrapDegrees(player.getYaw());
        return lastAppliedValid ? unwrapTowards(wrappedCurrent, lastAppliedYaw) : wrappedCurrent;
    }

    private Vec3d computeAimPoint(ClientPlayerEntity player, LivingEntity target, boolean attackNow) {
        return AimingL.getAimPointInsideHitbox(player, target, attackNow, 0.1, 0.6, 2.5);
    }

    private boolean isMurderMysteryActive() {
        return murderMysteryMode.getRawState();
    }

    private boolean shouldFilterTeammates() {
        return !isMurderMysteryActive() && ignoreTeammates.getRawState();
    }

    private @Nullable AbstractClientPlayerEntity detectMurderer(@NotNull ClientPlayerEntity self) {
        if (!isMurderMysteryActive()) return null;
        float curYaw = getContinuousYaw(self);
        return MurderMysteryAgent.getVisiblePlayers().stream()
                .filter(pp -> pp != null && pp.playerEntity != null && pp.playerEntity.isAlive())
                .filter(pp -> pp.role == MurderMysteryAgent.PersistentPlayer.Role.MURDERER)
                .map(pp -> pp.playerEntity)
                .filter(pe -> pe != self)
                .filter(pe -> self.squaredDistanceTo(pe) <= Math.pow(aimRange.getRawState(), 2))
                .filter(pe -> pe instanceof AbstractClientPlayerEntity)
                .map(pe -> (AbstractClientPlayerEntity) pe)
                .min(Comparator.comparingDouble(p -> scoreTarget(self, curYaw, p)))
                .orElse(null);
    }

    private @Nullable AbstractClientPlayerEntity detectDetective(@NotNull ClientPlayerEntity self) {
        if (!isMurderMysteryActive() || !detectiveAim.getRawState()) return null;
        float curYaw = getContinuousYaw(self);
        return MurderMysteryAgent.getVisiblePlayers().stream()
                .filter(pp -> pp != null && pp.playerEntity != null && pp.playerEntity.isAlive())
                .filter(pp -> pp.role == MurderMysteryAgent.PersistentPlayer.Role.DETECTIVE)
                .map(pp -> pp.playerEntity)
                .filter(pe -> pe != self)
                .filter(pe -> self.squaredDistanceTo(pe) <= Math.pow(aimRange.getRawState(), 2))
                .filter(pe -> pe instanceof AbstractClientPlayerEntity)
                .map(pe -> (AbstractClientPlayerEntity) pe)
                .min(Comparator.comparingDouble(p -> scoreTarget(self, curYaw, p)))
                .orElse(null);
    }

    private @Nullable LivingEntity pickOrValidateTarget(ClientWorld world, ClientPlayerEntity player) {
        if (isMurderMysteryActive()) {
            AbstractClientPlayerEntity mm = detectMurderer(player);
            if (mm != null) return mm;
            AbstractClientPlayerEntity det = detectDetective(player);
            if (det != null) return det;
            return null;
        }

        if (activeTarget != null && activeTarget.isAlive() && !activeTarget.isRemoved()) {
            if (activeTarget.squaredDistanceTo(player) <= (MELEE_REACH + 4.0) * (MELEE_REACH + 4.0)) {
                if (!(activeTarget instanceof AbstractClientPlayerEntity p) || !shouldFilterTeammates() || !ServerL.playerOnSameTeam(player, p)) {
                    return activeTarget;
                }
            }
        }

        Stream<LivingEntity> players = world.getPlayers().stream()
                .filter(p -> p != player)
                .filter(p -> p.isAlive() && !p.isRemoved() && !p.isSpectator())
                .filter(p -> !shouldFilterTeammates() || !ServerL.playerOnSameTeam(player, p))
                .map(p -> (LivingEntity) p);

        Stream<LivingEntity> villagers = Stream.empty();
        if (testMode.getRawState()) {
            List<VillagerEntity> vs = world.getEntitiesByClass(VillagerEntity.class, player.getBoundingBox().expand(12.0), v -> v.isAlive() && !v.isRemoved());
            villagers = vs.stream().map(v -> (LivingEntity) v);
        }

        float curYaw = getContinuousYaw(player);
        return Stream.concat(players, villagers)
                .sorted(Comparator.comparingDouble(player::squaredDistanceTo))
                .filter(e -> player.squaredDistanceTo(e) <= Math.pow(aimRange.getRawState(), 2))
                .min(Comparator.comparingDouble(e -> scoreTarget(player, curYaw, e)))
                .orElse(null);
    }

    private double scoreTarget(ClientPlayerEntity player, float curYaw, LivingEntity e) {
        double dist = player.distanceTo(e);
        double dx = e.getX() - player.getX();
        double dz = e.getZ() - player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double ang = Math.abs(MathHelper.wrapDegrees((float) (curYaw - targetYaw)));
        double behindPenalty = ang > 120 ? 1000 : 0;
        return dist + ang * 0.02 + behindPenalty;
    }

    public static @Nullable AbstractClientPlayerEntity getBestTargetFor(ClientPlayerEntity self) {
        if (self == null || self.clientWorld == null) return null;
        ClientWorld world = self.clientWorld;
        float curYaw = self.getYaw();
        double range2 = Math.pow(INSTANCE.aimRange.getRawState(), 2);

        if (INSTANCE.isMurderMysteryActive()) {
            AbstractClientPlayerEntity mm = INSTANCE.detectMurderer(self);
            if (mm != null) return mm;
            AbstractClientPlayerEntity det = INSTANCE.detectDetective(self);
            if (det != null) return det;
            return null;
        }

        boolean filterTeams = !INSTANCE.isMurderMysteryActive() && INSTANCE.ignoreTeammates.getRawState();
        return world.getPlayers().stream()
                .filter(p -> p != self)
                .filter(p -> p.isAlive() && !p.isRemoved() && !p.isSpectator())
                .filter(p -> self.squaredDistanceTo(p) <= range2)
                .filter(p -> !filterTeams || !ServerL.playerOnSameTeam(self, p))
                .min(Comparator.comparingDouble(p -> INSTANCE.scoreTarget(self, curYaw, p)))
                .orElse(null);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        clearPlan();
        lockOnEngaged = false;
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame != GameDetector.ParentGame.ZOMBIES;
    }
}
