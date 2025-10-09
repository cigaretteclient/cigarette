package dev.cigarette.module.murdermystery;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.PlayerAimbot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AutoBow – draws, holds, aims and fires the bow once alignment is acceptable.
 * Fixes implemented:
 *  - Removed premature slot switching while still aiming within viable tolerance.
 *  - Added hold grace (lostHoldTicks) with decay to avoid jitter-based cancellations.
 *  - Added configurable holdGraceTicks slider.
 *  - Respect switchSlotOnFail toggle before switching slots.
 *  - Proper tolerance scaling after long inactivity ( >60 / >120 ticks since last fire ).
 *  - Simplified and de-duplicated initialization logic.
 */
public class AutoBow extends TickModule<ToggleWidget, Boolean> {
    public static final AutoBow INSTANCE = new AutoBow("murdermystery.autobow", "AutoBow", "Automatically aims and fires a bow at the murderer.");

    private final SliderWidget shootDelay = new SliderWidget("Shoot Delay", "Maximum delay (ticks) to fully draw before shooting").withBounds(20, 45, 60).withAccuracy(1);
    private final SliderWidget targetRange = new SliderWidget("Max Range", "Maximum range to shoot a target.").withBounds(3, 5, 15);
    private final ToggleWidget genericMode = new ToggleWidget("Generic Mode", "Use PlayerAimbot target without Murder Mystery prioritization").withDefaultState(false);
    private final ToggleWidget prediction = new ToggleWidget("Prediction", "Use PlayerAimbot prediction settings while active").withDefaultState(false);
    private final SliderWidget predictionTicks = new SliderWidget("Prediction Ticks", "Ticks ahead to predict").withBounds(0, 5, 20).withAccuracy(1);
    private final SliderWidget holdAngleTolerance = new SliderWidget("Hold Angle", "Angle (deg) allowed while continuing to hold draw").withBounds(5, 25, 60).withAccuracy(1);
    private final SliderWidget holdGraceTicks = new SliderWidget("Hold Grace", "Ticks outside hold tolerance before cancelling").withBounds(1, 3, 12).withAccuracy(0);
    private final ToggleWidget switchSlotOnFail = new ToggleWidget("Slot Switch Fail", "Switch slot after failed shot alignment").withDefaultState(true);

    private boolean paOldEnableState;
    private boolean paOldPredictionState;
    private boolean paOldMMState;
    private double paOldPredictionTicks;
    private Boolean paMMOldState = false;

    private boolean isMurderer = false;

    private int drawTicks = 0;
    private int requiredDrawTicks = 20;
    private boolean drawing = false;
    private int ticksSinceLastFire = 0;
    private int lostHoldTicks = 0;

    private AutoBow(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(shootDelay, targetRange, genericMode, prediction, predictionTicks, holdAngleTolerance, holdGraceTicks, switchSlotOnFail);
        shootDelay.registerConfigKey(id + ".shootDelay");
        targetRange.registerConfigKey(id + ".targetRange");
        genericMode.registerConfigKey(id + ".genericMode");
        predictionTicks.registerConfigKey(id + ".predictionTicks");
        holdAngleTolerance.registerConfigKey(id + ".holdAngleTolerance");
        holdGraceTicks.registerConfigKey(id + ".holdGraceTicks");
        switchSlotOnFail.registerConfigKey(id + ".switchSlotOnFail");
    }

    @Override
    public void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (client.currentScreen != null) return;

        ticksSinceLastFire++;

        boolean shouldMM = !genericMode.getRawState();
        if (PlayerAimbot.INSTANCE.murderMysteryMode.getRawState() != shouldMM) {
            PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(shouldMM);
            releaseIfDrawing(player);
        }

        ensureActiveTarget(player);
        LivingEntity target = PlayerAimbot.INSTANCE.activeTarget;
        if (target != null) {
            double maxRangeSq = square(targetRange.getRawState());
            if (!target.isAlive() || target.isRemoved() || player.squaredDistanceTo(target) > maxRangeSq) {
                PlayerAimbot.INSTANCE.activeTarget = null;
                target = null;
            }
        }
        if (target == null) {
            releaseIfDrawing(player);
            return;
        }

        if (!genericMode.getRawState()) {
            LivingEntity fTarget = target;
            Optional<MurderMysteryAgent.PersistentPlayer> tPlayer = MurderMysteryAgent.getVisiblePlayers().stream().filter(p -> p.playerEntity == fTarget).findFirst();
            isMurderer = tPlayer.filter(pp -> pp.role == MurderMysteryAgent.PersistentPlayer.Role.MURDERER).isPresent();
        }

        selectBowIfNeeded(player);
        if (!holdingBow(player)) {
            releaseIfDrawing(player);
            return;
        }

        HitResult hr = client.crosshairTarget;
        if (hr != null && hr.getType() == HitResult.Type.BLOCK) {
            releaseIfDrawing(player);
            return;
        }

        if (!drawing) {
            if (!canStartDrawing(player, target)) return;
            startDrawing(player);
            return;
        }

        double baseTol = PlayerAimbot.INSTANCE.aimToleranceDeg.getRawState();
        double holdTol = holdAngleTolerance.getRawState();
        if (ticksSinceLastFire > 120) {
            baseTol *= 1.6;
            holdTol *= 1.25;
        } else if (ticksSinceLastFire > 60) {
            baseTol *= 1.4;
            holdTol *= 1.15;
        }

        double angleActual = computeAngle(player, target);
        boolean predEnabled = prediction.getRawState();
        double predictedAngle = Double.NaN;
        if (predEnabled) {
            int pticks = Math.max(0, predictionTicks.getRawState().intValue());
            Vec3d predicted = target.getPos().add(target.getVelocity().multiply(pticks))
                .add(0, target.getStandingEyeHeight() * 0.5, 0);
            predictedAngle = computeAngleToPoint(player, predicted);
        }

        boolean withinHold = (!Double.isNaN(angleActual) && angleActual <= holdTol) || (predEnabled && !Double.isNaN(predictedAngle) && predictedAngle <= holdTol);
        int graceLimit = Math.max(1, holdGraceTicks.getRawState().intValue());
        if (!withinHold) {
            lostHoldTicks++;
            if (lostHoldTicks >= graceLimit) {
                failRelease(player);
                return;
            }
        } else {
            if (lostHoldTicks > 0) lostHoldTicks = Math.max(0, lostHoldTicks - 2);
        }

        drawTicks++;
        int minHoldTicks = 5;
        if (drawTicks >= requiredDrawTicks && drawTicks >= minHoldTicks) {
            boolean crosshairOn = client.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() == target;
            boolean actualOk = !Double.isNaN(angleActual) && angleActual <= baseTol;
            boolean predictedOk = predEnabled && !Double.isNaN(predictedAngle) && predictedAngle <= baseTol;
            if (crosshairOn || actualOk || predictedOk) {
                fire(player);
            }
        }
    }

    private boolean canStartDrawing(ClientPlayerEntity player, LivingEntity target) {
        if (target == null) return false;
        double maxRangeSq = square(targetRange.getRawState());
        if (player.squaredDistanceTo(target) > maxRangeSq) return false;
        double baseTol = PlayerAimbot.INSTANCE.aimToleranceDeg.getRawState();
        double holdTol = holdAngleTolerance.getRawState();
        double angleActual = computeAngle(player, target);
        if (!Double.isNaN(angleActual) && angleActual <= baseTol) return true;
        boolean predEnabled = prediction.getRawState();
        if (predEnabled) {
            int pticks = Math.max(0, predictionTicks.getRawState().intValue());
            Vec3d predicted = target.getPos().add(target.getVelocity().multiply(pticks))
                .add(0, target.getStandingEyeHeight() * 0.5, 0);
            double pAng = computeAngleToPoint(player, predicted);
            if (!Double.isNaN(pAng) && pAng <= baseTol) return true;
            return !Double.isNaN(angleActual) && angleActual <= holdTol;
        }
        return !Double.isNaN(angleActual) && angleActual <= holdTol;
    }

    private void ensureActiveTarget(ClientPlayerEntity player) {
        LivingEntity current = PlayerAimbot.INSTANCE.activeTarget;
        if (current == null || !current.isAlive() || current.isRemoved()) {
            PlayerAimbot.INSTANCE.activeTarget = null;
            if (drawing) failRelease(player);
            try {
                PlayerAimbot.INSTANCE.activeTarget = PlayerAimbot.getBestTargetFor(player);
            } catch (Exception ignored) {
                PlayerAimbot.INSTANCE.activeTarget = null;
            }
        }
    }

    private void failRelease(ClientPlayerEntity player) {
        if (switchSlotOnFail.getRawState()) {
            releaseAndMaybeSwitch(player);
        } else {
            releaseIfDrawing(player);
        }
    }

    private void releaseAndMaybeSwitch(ClientPlayerEntity player) {
        releaseIfDrawing(player);
        switchHotbarSlot(player);
    }

    private void selectBowIfNeeded(ClientPlayerEntity player) {
        if (holdingBow(player)) return;
        DefaultedList<ItemStack> inv = player.getInventory().getMainStacks();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.get(i).isOf(Items.BOW)) {
                player.getInventory().setSelectedSlot(i);
                break;
            }
        }
    }

    private boolean holdingBow(ClientPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.BOW) || player.getOffHandStack().isOf(Items.BOW);
    }

    private double computeAngle(ClientPlayerEntity player, LivingEntity target) {
        if (target == null) return Double.NaN;
        Vec3d eye = player.getEyePos();
        Vec3d to = target.getPos().add(0, target.getStandingEyeHeight() * 0.5, 0).subtract(eye);
        if (to.lengthSquared() == 0) return Double.NaN;
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d dir = to.normalize();
        double dot = Math.max(-1, Math.min(1, look.dotProduct(dir)));
        return Math.toDegrees(Math.acos(dot));
    }

    private double computeAngleToPoint(ClientPlayerEntity player, Vec3d point) {
        Vec3d eye = player.getEyePos();
        Vec3d to = point.subtract(eye);
        if (to.lengthSquared() == 0) return 0.0;
        Vec3d look = player.getRotationVec(1.0F).normalize();
        Vec3d dir = to.normalize();
        double dot = Math.max(-1, Math.min(1, look.dotProduct(dir)));
        return Math.toDegrees(Math.acos(dot));
    }

    private void startDrawing(ClientPlayerEntity player) {
        if (!holdingBow(player)) return;
        if (player.isUsingItem()) return;
        drawing = true;
        drawTicks = 0;
        lostHoldTicks = 0;
        int max = shootDelay.getRawState().intValue();
        if (max < 20) max = 20;
        requiredDrawTicks = 20 + (max > 20 ? ThreadLocalRandom.current().nextInt(Math.max(1, max - 19)) : 0);
        if (MinecraftClient.getInstance().interactionManager != null) {
            KeyBinding binding = KeyBinding.byId("key.use");
            if (binding != null) {
                binding.setPressed(true);
                KeyBindingAccessor accessor = (KeyBindingAccessor) binding;
                accessor.setTimesPressed(accessor.getTimesPressed() + 1);
            }
        }
    }

    private void fire(ClientPlayerEntity player) {
        if (!drawing) return;
        if (player.isUsingItem()) {
            KeyBinding binding = KeyBinding.byId("key.use");
            if (binding != null) {
                binding.setPressed(false);
                KeyBindingAccessor accessor = (KeyBindingAccessor) binding;
                accessor.setTimesPressed(Math.max(0, accessor.getTimesPressed() - 1));
            }
        }
        drawing = false;
        drawTicks = 0;
        lostHoldTicks = 0;
        ticksSinceLastFire = 0;
    }

    private void releaseIfDrawing(ClientPlayerEntity player) {
        if (!drawing) return;
        fire(player);
    }

    private void switchHotbarSlot(ClientPlayerEntity player) {
        if (player == null) return;
        int current = player.getInventory().getSelectedSlot();
        for (int i = 1; i < 9; i++) {
            int next = (current + i) % 9;
            ItemStack stack = player.getInventory().getStack(next);
            if (!stack.isEmpty() && !stack.isOf(Items.BOW)) {
                player.getInventory().setSelectedSlot(next);
                return;
            }
        }
        player.getInventory().setSelectedSlot((current + 1) % 9);
    }

    private double square(double v) { return v * v; }

    @Override
    protected void whenEnabled() {
        this.paOldEnableState = PlayerAimbot.INSTANCE.getRawState();
        this.paMMOldState = PlayerAimbot.INSTANCE.murderMysteryMode.getRawState();
        paOldPredictionState = PlayerAimbot.INSTANCE.prediction.getRawState();
        paOldPredictionTicks = PlayerAimbot.INSTANCE.predictionTicks.getRawState();
        paOldMMState = PlayerAimbot.INSTANCE.murderMysteryMode.getRawState();

        PlayerAimbot.INSTANCE.widget.setRawState(true);
        AutoClicker.INSTANCE.widget.setRawState(false);
        PlayerAimbot.INSTANCE.predictionTicks.setRawState(predictionTicks.getRawState());
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(!genericMode.getRawState());
        PlayerAimbot.INSTANCE.prediction.setRawState(prediction.getRawState());

        isMurderer = false;
        drawTicks = 0;
        requiredDrawTicks = 20;
        drawing = false;
        lostHoldTicks = 0;
        ticksSinceLastFire = 0;
    }

    @Override
    protected void whenDisabled() {
        PlayerAimbot.INSTANCE.widget.setRawState(this.paOldEnableState);
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(this.paMMOldState);
        PlayerAimbot.INSTANCE.prediction.setRawState(paOldPredictionState);
        PlayerAimbot.INSTANCE.predictionTicks.setRawState(paOldPredictionTicks);
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(paOldMMState);
        AutoClicker.INSTANCE.widget.setRawState(false);

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.interactionManager != null && mc.player != null) {
            mc.interactionManager.stopUsingItem(mc.player);
        }
        drawing = false;
        drawTicks = 0;
        requiredDrawTicks = 20;
        lostHoldTicks = 0;
    }
}
