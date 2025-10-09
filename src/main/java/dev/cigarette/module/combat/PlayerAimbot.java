package dev.cigarette.module.combat;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.gui.widget.ToggleKeybindWidget;
import dev.cigarette.lib.AimingL;
import dev.cigarette.lib.ServerL;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public class PlayerAimbot extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerAimbot INSTANCE = new PlayerAimbot("combat.playerAimbot", "PlayerAimbot", "Automatically aims at players. The legitest killaura.");

    private final ToggleWidget autoAttack = new ToggleWidget("Auto Attack", "Automatically hits players").withDefaultState(true);
    public final SliderWidget smoothAim = new SliderWidget("Smooth Aim", "How quickly to snap to target in ticks").withBounds(1, 5, 20);
    public final ToggleWidget wTap = new ToggleWidget("W-Tap", "Automatically sprint before attacking").withDefaultState(false);

    public final SliderWidget attackCps = new SliderWidget("Attack CPS", "Clicks per second for auto attack").withBounds(1, 8, 15);

    public final ToggleWidget ignoreTeammates = new ToggleWidget("Ignore Teammates", "Don't target players on your team").withDefaultState(true);

    private final ToggleKeybindWidget lockOnKeybind = new ToggleKeybindWidget(
            "Lock-On Trigger", "Press key to lock to best target. Releases only when target dies"
    ).withDefaultState(false);

    public final ToggleWidget murderMysteryMode = new ToggleWidget("Murder Mystery", "Prefer murderer and detective targets").withDefaultState(false);
    public final ToggleWidget detectiveAim = new ToggleWidget("Detective Aim", "Aim at a detective when no Murderer is available (or you are the mur)").withDefaultState(true);

    public final SliderWidget jitterViolence = new SliderWidget("Jitter Aggression", "Scale of aim jitter (0 disables)").withBounds(0.0, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget driftViolence = new SliderWidget("Drift Aggression", "Scale of slow drift vs jitter").withBounds(0.0, 0.6, 2.0).withAccuracy(2);
    public final SliderWidget aimToleranceDeg = new SliderWidget("Aim Tolerance (deg)", "Max angular distance (hypot of yaw/pitch) off for attack").withBounds(0.5, 2.0, 6.0).withAccuracy(1);

    public final SliderWidget smoothingMultiplier = new SliderWidget("Smoothing Multiplier", "Scales duration based on angle").withBounds(0.5, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget bezierInfluence = new SliderWidget("Bezier Influence", "0 = linear, 1 = full bezier curve").withBounds(0.0, 1.0, 1.0).withAccuracy(2);
    public final SliderWidget controlJitterScale = new SliderWidget("Control Jitter", "Randomness of control points").withBounds(0.0, 1.0, 3.0).withAccuracy(2);
    public final SliderWidget interpolationMode = new SliderWidget("Interpolation Mode", "0=Linear 1=Cubic 2=Cos 3=Smoothstep").withBounds(0, 1, 3).withAccuracy(0);

    public final SliderWidget aimRange = new SliderWidget("Aim Range", "Maximum range to target players").withBounds(3, 6, 20).withAccuracy(1);

    public final ToggleWidget prediction = new ToggleWidget("Prediction", "Predict target position").withDefaultState(false);
    public final SliderWidget predictionTicks = new SliderWidget("Prediction Ticks", "Ticks ahead to predict").withBounds(0, 5, 20).withAccuracy(1);
    public final SliderWidget engageAimAngle = new SliderWidget("Engage Angle (deg)", "Only begin aim plan when target angle gap >= this; also scales prediction").withBounds(1.0, 6.0, 30.0).withAccuracy(1);

    private PlayerAimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(autoAttack, smoothAim, aimRange, prediction, predictionTicks, engageAimAngle, wTap, attackCps, ignoreTeammates, lockOnKeybind,
                murderMysteryMode, detectiveAim,
                jitterViolence, driftViolence, aimToleranceDeg, smoothingMultiplier, bezierInfluence, controlJitterScale, interpolationMode);
        autoAttack.registerConfigKey(id + ".autoAttack");
        smoothAim.registerConfigKey(id + ".smoothAim");
        aimRange.registerConfigKey(id + ".aimRange");
        prediction.registerConfigKey(id + ".prediction");
        predictionTicks.registerConfigKey(id + ".predictionTicks");
        engageAimAngle.registerConfigKey(id + ".engageAimAngle");
        wTap.registerConfigKey(id + ".wTap");
        attackCps.registerConfigKey(id + ".attackCps");
        ignoreTeammates.registerConfigKey(id + ".ignoreTeammates");
        lockOnKeybind.registerConfigKey(id + ".lockOnKeybind");
        murderMysteryMode.registerConfigKey(id + ".murderMysteryMode");
        detectiveAim.registerConfigKey(id + ".detectiveAim");
        jitterViolence.registerConfigKey(id + ".jitterViolence");
        driftViolence.registerConfigKey(id + ".driftViolence");
        aimToleranceDeg.registerConfigKey(id + ".aimToleranceDeg");
        smoothingMultiplier.registerConfigKey(id + ".smoothingMultiplier");
        bezierInfluence.registerConfigKey(id + ".bezierInfluence");
        controlJitterScale.registerConfigKey(id + ".controlJitterScale");
        interpolationMode.registerConfigKey(id + ".interpolationMode");
    }

    // Bezier aim plan state
    public @Nullable LivingEntity activeTarget = null;
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

    private boolean lastAppliedValid = false;
    private float lastAppliedYaw = 0f, lastAppliedPitch = 0f;

    private boolean lockOnEngaged = false;
    private static final float SOFT_REPLAN_ANGLE_DEG = 12f;

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (MinecraftClient.getInstance().currentScreen != null) return;
        boolean lockOnMode = lockOnKeybind.getRawState();
        if (!lockOnMode && lockOnEngaged) { lockOnEngaged = false; clearPlan(); }
        LivingEntity target = null;

        if (lockOnMode) {
            if (!lockOnEngaged && lockOnKeybind.getKeybind().wasPressed()) {
                AbstractClientPlayerEntity best = getBestTargetFor(player);
                if (best != null) {
                    lockOnEngaged = true; activeTarget = best; activeTargetId = best.getUuid(); planTicksTotal = 0; planTicksElapsed = 0;
                }
            }
            if (lockOnEngaged) {
                if (activeTarget != null && activeTarget.isAlive() && !activeTarget.isRemoved() && player.squaredDistanceTo(activeTarget) <= Math.pow(aimRange.getRawState(), 2)) {
                    target = activeTarget;
                } else { lockOnEngaged = false; clearPlan(); return; }
            } else { clearPlan(); return; }
        } else { target = pickOrValidateTarget(world, player); }

        if (target == null) { clearPlan(); return; }

        double angleGap = computeAngleGap(player, target);
        double engageThresh = Math.max(0.5, engageAimAngle.getRawState());
        int basePred = (int) Math.round(predictionTicks.getRawState());
        int dynamicPred = basePred;
        if (prediction.getRawState() && angleGap >= engageThresh) {
            double factor = (angleGap - engageThresh) / engageThresh;
            int extra = (int) Math.min(10, Math.max(0, Math.round(factor * 4)));
            dynamicPred = Math.min(30, basePred + extra);
        }

        Vec3d aimPoint = computeAimPoint(player, target, false, dynamicPred);
        float[] targetAngles = AimingL.anglesFromTo(player.getEyePos(), aimPoint);
        float curYaw = getContinuousYaw(player);
        float curPitch = MathHelper.clamp(player.getPitch(), -90f, 90f);

        boolean planActive = isPlanActive();
        boolean sameTarget = Objects.equals(target.getUuid(), activeTargetId);
        float dyawToEnd = Math.abs(MathHelper.wrapDegrees(endYaw - targetAngles[0]));
        float dpitchToEnd = Math.abs(endPitch - targetAngles[1]);

        boolean largeChange = dyawToEnd > SOFT_REPLAN_ANGLE_DEG || dpitchToEnd > SOFT_REPLAN_ANGLE_DEG;
        boolean minorChange = (dyawToEnd > 0.2f || dpitchToEnd > 0.2f) && !largeChange;

        if (!planActive || !sameTarget) {
            if (angleGap >= engageThresh || !planActive) {
                buildBezierPlan(curYaw, curPitch, targetAngles[0], targetAngles[1]);
                activeTarget = target; activeTargetId = target.getUuid();
            }
        } else {
            if (largeChange) {
                buildBezierPlan(curYaw, curPitch, targetAngles[0], targetAngles[1]);
            } else if (minorChange) {
                updatePlanEndpoint(targetAngles[0], targetAngles[1]);
            }
        }

        if (!isPlanActive() && angleGap < engageThresh) {
            float snapYaw = unwrapTowards(targetAngles[0], curYaw);
            player.setYaw(snapYaw);
            player.setPitch(targetAngles[1]);
            lastAppliedYaw = snapYaw; lastAppliedPitch = targetAngles[1]; lastAppliedValid = true;
        } else {
            float[] stepAngles = evalPlanStep();
            player.setYaw(stepAngles[0]);
            player.setPitch(stepAngles[1]);
            lastAppliedYaw = stepAngles[0]; lastAppliedPitch = stepAngles[1]; lastAppliedValid = true;
        }

        if (autoAttack.getRawState()) {
            if (ticksUntilNextAttack > 0) ticksUntilNextAttack--;
            boolean withinRange = player.distanceTo(target) <= MELEE_REACH;
            boolean aimAligned = aimAlignmentOk(player, target);
            if (withinRange && aimAligned && ticksUntilNextAttack <= 0) {
                if (wTap.getRawState()) AimingL.doSprint(true);
                Vec3d atkAim = computeAimPoint(player, target, true, dynamicPred);
                float[] atkAngles = AimingL.anglesFromTo(player.getEyePos(), atkAim);
                AimingL.lookAndAttack(world, player, target, atkAngles[0], atkAngles[1]);
                buildBezierPlan(atkAngles[0], atkAngles[1], targetAngles[0], targetAngles[1]);
                lastAppliedYaw = atkAngles[0]; lastAppliedPitch = atkAngles[1];
                planTicksElapsed = Math.min(1, planTicksElapsed);
                scheduleNextAttack();
            }
        }
    }

    private @Nullable LivingEntity pickOrValidateTarget(ClientWorld world, ClientPlayerEntity player) {
        if (activeTarget != null && activeTarget.isAlive() && !activeTarget.isRemoved()
                && player.squaredDistanceTo(activeTarget) <= Math.pow(aimRange.getRawState(), 2)) {
            if (shouldFilterTeammates() && activeTarget instanceof AbstractClientPlayerEntity acp
                    && ServerL.playerOnSameTeam(player, acp)) {
                return getBestTargetFor(player);
            }
            return activeTarget;
        }

        return getBestTargetFor(player);
    }

    private double scoreTarget(ClientPlayerEntity self, float selfYaw, AbstractClientPlayerEntity other) {
        double d2 = self.squaredDistanceTo(other);
        if (d2 < 0.01) d2 = 0.01;
        float[] angles = AimingL.anglesFromTo(self.getEyePos(), other.getEyePos().add(0, other.getHeight() * 0.5, 0));
        float yawDiff = MathHelper.wrapDegrees(angles[0] - selfYaw);
        float pitchDiff = angles[1] - self.getPitch();
        double angleDiff = Math.hypot(yawDiff, pitchDiff);
        if (angleDiff < 0.1) angleDiff = 0.1;
        return d2 * angleDiff;
    }

    private @Nullable AbstractClientPlayerEntity detectMurderer(ClientPlayerEntity self) {
        List<AbstractClientPlayerEntity> players = self.clientWorld.getPlayers();
        Stream<AbstractClientPlayerEntity> stream = players.stream()
                .filter(p -> p != self)
                .filter(p -> p.isAlive() && !p.isRemoved() && !p.isSpectator())
                .filter(p -> self.squaredDistanceTo(p) <= Math.pow(aimRange.getRawState(), 2));
        if (shouldFilterTeammates()) {
            stream = stream.filter(p -> !ServerL.playerOnSameTeam(self, p));
        }
        return stream
                .filter(
                        p -> MurderMysteryAgent.getRole(p) == MurderMysteryAgent.PersistentPlayer.Role.MURDERER)
                .min(Comparator.comparingDouble(p -> scoreTarget(self, self.getYaw(), p)))
                .orElse(null);
    }

    private void scheduleNextAttack() {
        double cps = Math.max(1.0, attackCps.getRawState());
        double idealTicks = 20.0 / cps;
        double jitter = idealTicks * (0.15 + rng.nextDouble() * 0.20);
        ticksUntilNextAttack = (int) Math.max(1, Math.round(idealTicks + (rng.nextBoolean() ? jitter : -jitter * 0.5)));
    }

    private boolean aimAlignmentOk(ClientPlayerEntity player, LivingEntity target) {
        Vec3d aimPoint = computeAimPoint(player, target);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d toTarget = aimPoint.subtract(player.getEyePos());
        if (toTarget.lengthSquared() == 0) return true;
        lookVec = lookVec.normalize();
        Vec3d toNorm = toTarget.normalize();
        double dot = Math.max(-1.0, Math.min(1.0, lookVec.dotProduct(toNorm)));
        double angleDeg = Math.toDegrees(Math.acos(dot));
        double tol = Math.max(0.1, aimToleranceDeg.getRawState());
        return angleDeg <= tol;
    }

    private void updatePlanEndpoint(float newYaw, float newPitch) {
        endYaw = unwrapTowards(newYaw, startYaw);
        endPitch = MathHelper.clamp(newPitch, -90f, 90f);
        float minPitch = Math.min(startPitch, endPitch);
        float maxPitch = Math.max(startPitch, endPitch);
        c1Pitch = MathHelper.clamp(c1Pitch, minPitch, maxPitch);
        c2Pitch = MathHelper.clamp(c2Pitch, minPitch, maxPitch);
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
        float baseJitter = Math.max(0.0f, Math.min(0.4f, angleSpan * 0.0025f));
        float jitterScale = (float) Math.max(0.0, jitterViolence.getRawState());
        float driftScale = (float) Math.max(0.0, driftViolence.getRawState());
        float jitterMagYaw = baseJitter * jitterScale;
        float jitterMagPitch = baseJitter * 0.55f * jitterScale;
        float driftMagYaw = jitterMagYaw * 0.5f * driftScale;
        float driftMagPitch = jitterMagPitch * 0.5f * driftScale;
        float fineJitterYaw = (float) (rng.nextGaussian() * jitterMagYaw * (1.0 - et));
        float fineJitterPitch = (float) (rng.nextGaussian() * jitterMagPitch * (1.0 - et));
        float driftYaw = (float) (rng.nextGaussian() * driftMagYaw * 0.2);
        float driftPitch = (float) (rng.nextGaussian() * driftMagPitch * 0.2);

        float outYaw = yaw + fineJitterYaw + driftYaw;
        float outPitch = pitch + fineJitterPitch + driftPitch;
        float minBand = Math.min(startPitch, endPitch) - 2.0f;
        float maxBand = Math.max(startPitch, endPitch) + 2.0f;
        outPitch = MathHelper.clamp(outPitch, minBand, maxBand);
        outPitch = MathHelper.clamp(outPitch, -90f, 90f);
        return new float[]{outYaw, outPitch};
    }

    private void buildBezierPlan(float curYaw, float curPitch, float targetYaw, float targetPitch) {
        startYaw = curYaw;
        startPitch = curPitch;
        float tYaw = unwrapTowards(targetYaw, curYaw);
        endYaw = tYaw;
        endPitch = MathHelper.clamp(targetPitch, -90f, 90f);

        float yawDelta = endYaw - startYaw;
        float pitchDelta = endPitch - startPitch;
        float angleMag = Math.abs(yawDelta) + Math.abs(pitchDelta);

        int baseTicks = (int) Math.max(1, Math.round(smoothAim.getRawState()));
        double mult = Math.max(0.1, smoothingMultiplier.getRawState());
        planTicksTotal = (int) Math.max(baseTicks, Math.min(baseTicks * 4L, Math.round(baseTicks * mult * (0.5 + angleMag / 45f))));
        planTicksElapsed = 0;

        float c1t = 0.30f + (float) rng.nextDouble() * 0.15f;
        float c2t = 0.70f - (float) rng.nextDouble() * 0.15f;
        float ctrlJitter = (float) (Math.max(0.0f, Math.min(3.5f, angleMag * 0.05f)) * Math.max(0.0, controlJitterScale.getRawState()));
        c1Yaw = startYaw + yawDelta * c1t + (float) (rng.nextGaussian() * ctrlJitter);
        c2Yaw = startYaw + yawDelta * c2t + (float) (rng.nextGaussian() * ctrlJitter);
        c1Pitch = startPitch + pitchDelta * c1t + (float) (rng.nextGaussian() * ctrlJitter * 0.6f);
        c2Pitch = startPitch + pitchDelta * c2t + (float) (rng.nextGaussian() * ctrlJitter * 0.6f);

        float minPitch = Math.min(startPitch, endPitch);
        float maxPitch = Math.max(startPitch, endPitch);
        c1Pitch = MathHelper.clamp(c1Pitch, minPitch, maxPitch);
        c2Pitch = MathHelper.clamp(c2Pitch, minPitch, maxPitch);

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

    private boolean isPlanActive() {
        return planTicksElapsed < planTicksTotal;
    }

    private double computeAngleGap(ClientPlayerEntity player, LivingEntity target) {
        Vec3d eye = player.getEyePos();
        Vec3d to = target.getBoundingBox().getCenter().subtract(eye);
        if (to.lengthSquared() == 0) return 0.0;
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d dir = to.normalize();
        double dot = Math.max(-1, Math.min(1, look.dotProduct(dir)));
        return Math.toDegrees(Math.acos(dot));
    }

    private boolean planTargetChangedSignificantly(float[] targetAngles) {
        float dyaw = Math.abs(MathHelper.wrapDegrees(endYaw - targetAngles[0]));
        float dpitch = Math.abs(endPitch - targetAngles[1]);
        double thresh = Math.max(1.0, engageAimAngle.getRawState());
        return dyaw > thresh || dpitch > thresh;
    }

    private void clearPlan() {
        activeTarget = null;
        activeTargetId = null;
        planTicksTotal = 0;
        planTicksElapsed = 0;
    }

    private static float cubicBezier(float p0, float p1, float p2, float p3, float t) {
        float u = 1f - t;
        return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3;
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

    private Vec3d computeAimPoint(ClientPlayerEntity player, LivingEntity target, boolean attackNow, int dynamicTicks) {
        if (prediction.getRawState()) {
            Vec3d vel = target.getVelocity();
            int ticks = Math.max(0, dynamicTicks);
            double px = target.getX() + vel.x * ticks;
            double pz = target.getZ() + vel.z * ticks;

            double baseY = target.getY();
            double height = target.getHeight();
            double py;
            if (target.isOnGround() || Math.abs(vel.y) < 0.05 || ticks == 0) {
                py = baseY + height * 0.5;
            } else {
                double g = 0.08;
                double vy = vel.y;
                double t = ticks;
                double predictedY = baseY + (vy * t) - 0.5 * g * t * t;
                double minY = baseY + height * 0.25;
                double maxY = baseY + height * 0.85;
                py = MathHelper.clamp(predictedY, minY, maxY);
            }
            return new Vec3d(px, py, pz);
        } else {
            return AimingL.getAimPointInsideHitbox(player, target, attackNow, 0.1, 0.6, 2.5);
        }
    }

    private Vec3d computeAimPoint(ClientPlayerEntity player, LivingEntity target) {
        return computeAimPoint(player, target, false, (int) Math.round(predictionTicks.getRawState()));
    }

    private boolean isMurderMysteryActive() {
        return murderMysteryMode.getRawState();
    }

    private boolean shouldFilterTeammates() {
        return !isMurderMysteryActive() && ignoreTeammates.getRawState();
    }

    private AbstractClientPlayerEntity detectDetective(ClientPlayerEntity self) {
        if (!detectiveAim.getRawState()) return null;
        List<AbstractClientPlayerEntity> players = self.clientWorld.getPlayers();
        Stream<AbstractClientPlayerEntity> stream = players.stream()
                .filter(p -> p != self)
                .filter(p -> p.isAlive() && !p.isRemoved() && !p.isSpectator())
                .filter(p -> self.squaredDistanceTo(p) <= Math.pow(aimRange.getRawState(), 2));
        if (shouldFilterTeammates()) {
            stream = stream.filter(p -> !ServerL.playerOnSameTeam(self, p));
        }
        return stream
                .filter(
                        p -> MurderMysteryAgent.getRole(p) == MurderMysteryAgent.PersistentPlayer.Role.DETECTIVE)
                .min(Comparator.comparingDouble(p -> scoreTarget(self, self.getYaw(), p)))
                .orElse(null);
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
                .filter(p -> self.squaredDistanceTo(p) <= range2)
                .filter(p -> !filterTeams || !ServerL.playerOnSameTeam(self, p))
                .filter(botPredicate)
                .min(Comparator.comparingDouble(p -> INSTANCE.scoreTarget(self, curYaw, p)))
                .orElse(null);
    }

    public static final java.util.function.Predicate<AbstractClientPlayerEntity> botPredicate = p ->
            !(p.isSleeping() || p.isSpectator() || p.isInvisible() || p.isDead() || p.age < 20 || !WorldL.isRealPlayer(p));

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
