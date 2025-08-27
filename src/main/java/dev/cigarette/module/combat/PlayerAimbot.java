package dev.cigarette.module.combat;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.InputOverride;
import dev.cigarette.lib.ServerL;
import dev.cigarette.lib.WeaponSelector;
import dev.cigarette.lib.WorldL;
import dev.cigarette.mixin.ClientWorldAccessor;
import dev.cigarette.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class PlayerAimbot extends TickModule<ToggleWidget, Boolean> {
    public static final PlayerAimbot INSTANCE = new PlayerAimbot("combat.playerAimbot", "PlayerAimbot", "Automatically aims at players.");

//    private final ToggleWidget silentAim = new ToggleWidget("Silent Aim", "Doesn't snap your camera client-side.").withDefaultState(true);
    private final ToggleWidget autoAttack = new ToggleWidget("Auto Attack", "Automatically hits players").withDefaultState(true);
    private final ToggleWidget autoWeaponSwitch = new ToggleWidget("Auto Weapon Switch", "Automatically switch weapons").withDefaultState(true);
    public final ToggleWidget predictiveAim = new ToggleWidget("Predictive Aim", "Predict player movement for better accuracy").withDefaultState(true);
    public final SliderWidget predictionTicks = new SliderWidget("Prediction Ticks", "How many ticks ahead to predict player movement").withBounds(1, 10, 20).withAccuracy(0);
    public final SliderWidget smoothAim = new SliderWidget("Smooth Aim", "How quickly to snap to target in ticks").withBounds(1, 5, 20).withAccuracy(0);
    public final ToggleWidget wTap = new ToggleWidget("W-Tap", "Automatically sprint before attacking").withDefaultState(false);
    public final SliderWidget.TwoHandedSlider jitterAmount = new SliderWidget.TwoHandedSlider("Jitter", "Random jitter range (min/max) degrees").withBounds(0, 0, 4).withAccuracy(2);
    public final SliderWidget jitterSpeed = new SliderWidget("Jitter Speed", "How fast jitter target changes (ticks)").withBounds(5, 10, 40).withAccuracy(0);
    private final ToggleWidget blockHit = new ToggleWidget("Block-Hit", "Briefly block just before attacking (if shield available)").withDefaultState(false);

    private KeyBinding rightClickKey = null;

    private boolean hasLastAim = false;
    private float lastAimYaw = 0f;
    private float lastAimPitch = 0f;
    private float currentJitterYaw = 0f;
    private float currentJitterPitch = 0f;
    private float targetJitterYaw = 0f;
    private float targetJitterPitch = 0f;
    private int jitterTickCounter = 0;

    private PlayerAimbot(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(autoAttack, autoWeaponSwitch, predictiveAim, predictionTicks, smoothAim, jitterAmount, jitterSpeed, wTap, blockHit);
        autoAttack.registerConfigKey(id + ".autoAttack");
        autoWeaponSwitch.registerConfigKey(id + ".autoWeaponSwitch");
        predictiveAim.registerConfigKey(id + ".predictiveAim");
        predictionTicks.registerConfigKey(id + ".predictionTicks");
        smoothAim.registerConfigKey(id + ".smoothAim");
        wTap.registerConfigKey(id + ".wTap");
        jitterAmount.registerConfigKey(id + ".jitter");
        jitterSpeed.registerConfigKey(id + ".jitterSpeed");
        blockHit.registerConfigKey(id + ".blockHit");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        if (rightClickKey.isPressed() || autoAttack.getRawState()) {
            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                BlockState lookingAt = world.getBlockState(blockResult.getBlockPos());
                if (lookingAt.isIn(BlockTags.BUTTONS) || lookingAt.isOf(Blocks.CHEST)) return;
            }

            AbstractClientPlayerEntity bestTarget = getBestTargetFor(player);

            if (bestTarget == null) {
                hasLastAim = false;
                return;
            }

            if (autoWeaponSwitch.getRawState()) {
                double dist = player.distanceTo(bestTarget);
                WeaponSelector.switchToBestPvPWeapon(player, dist);
            }

            Vec3d predictedPos;
            if (predictiveAim.getRawState()) {
                Vec3d currentPos = bestTarget.getPos();
                Vec3d instantVelocity = currentPos.subtract(bestTarget.lastX, bestTarget.lastY, bestTarget.lastZ);
                int ticks = predictionTicks.getRawState().intValue();
                double xVelocity = instantVelocity.x * ticks;
                double yVelocity = instantVelocity.y > LivingEntity.GRAVITY ? 0 : instantVelocity.y * (instantVelocity.y > 0 ? 1 : ticks);
                double zVelocity = instantVelocity.z * ticks;
                Vec3d projected = currentPos.add(xVelocity, yVelocity, zVelocity);
                predictedPos = projected.add(0, bestTarget.getEyeHeight(bestTarget.getPose()), 0);
            } else {
                predictedPos = bestTarget.getEyePos();
            }

            Vec3d vector = predictedPos.subtract(player.getEyePos()).normalize();

            float baseYaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
            float basePitch = (float) Math.toDegrees(Math.asin(-vector.y));

            double minJitter = jitterAmount.getMinValue();
            double maxJitter = jitterAmount.getMaxValue();
            double jitterRange = maxJitter - minJitter;
            int jitterSpeedTicks = (int) jitterSpeed.getRawState().doubleValue();
            if (jitterRange > 0 && jitterSpeedTicks > 0) {
                if (jitterTickCounter++ >= jitterSpeedTicks) {
                    jitterTickCounter = 0;
                    targetJitterYaw = (float) randomSigned(minJitter, maxJitter);
                    targetJitterPitch = (float) randomSigned(minJitter, maxJitter);
                }
                float lerpFactor = 1f / Math.max(1, jitterSpeedTicks);
                currentJitterYaw += (targetJitterYaw - currentJitterYaw) * lerpFactor;
                currentJitterPitch += (targetJitterPitch - currentJitterPitch) * lerpFactor;
            } else {
                currentJitterYaw = currentJitterPitch = targetJitterYaw = targetJitterPitch = 0f;
            }

            float targetYaw = baseYaw + currentJitterYaw;
            float targetPitch = basePitch + currentJitterPitch;

            int smoothTicks = smoothAim.getRawState().intValue();
            float sendYaw;
            float sendPitch;
            if (!hasLastAim) {
                lastAimYaw = targetYaw;
                lastAimPitch = targetPitch;
                hasLastAim = true;
            }
            if (smoothTicks <= 1) {
                sendYaw = targetYaw;
                sendPitch = targetPitch;
            } else {
                float yawDiff = wrapDegrees(targetYaw - lastAimYaw);
                float pitchDiff = targetPitch - lastAimPitch;
                float stepYaw = yawDiff / smoothTicks;
                float stepPitch = pitchDiff / smoothTicks;
                sendYaw = lastAimYaw + stepYaw;
                sendPitch = lastAimPitch + stepPitch;
            }
            lastAimYaw = sendYaw;
            lastAimPitch = sendPitch;

            boolean isRanged = isRangedStack(player.getMainHandStack());
            if (isRanged) {
                ClientWorldAccessor clientWorldAccessor = (ClientWorldAccessor) world;
                try (PendingUpdateManager pendingUpdateManager = clientWorldAccessor.getPendingUpdateManager().incrementSequence()) {
                    int seq = pendingUpdateManager.getSequence();
                    player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, sendYaw, sendPitch));
                }
            } else {
                if (wTap.getRawState()) {
                    doSprint(false);
                    doSprint(true);
                }

                player.setYaw(sendYaw);
                player.setPitch(sendPitch);

                if (blockHit.getRawState() && player.getOffHandStack() != null && player.getOffHandStack().isOf(Items.SHIELD)) {
                    ClientWorldAccessor clientWorldAccessor = (ClientWorldAccessor) world;
                    try (PendingUpdateManager pendingUpdateManager = clientWorldAccessor.getPendingUpdateManager().incrementSequence()) {
                        int seq = pendingUpdateManager.getSequence();
                        player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.OFF_HAND, seq, sendYaw, sendPitch));
                    }
                }

                player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(bestTarget, player.isSneaking()));
                player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        } else {
            hasLastAim = false;
        }
    }

    private static boolean isRangedStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        return (item instanceof BowItem)
            || (item instanceof CrossbowItem)
            || (item instanceof TridentItem)
            || stack.isOf(Items.SNOWBALL)
            || stack.isOf(Items.EGG)
            || stack.isOf(Items.ENDER_PEARL);
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
            if (player.isTeammate(p) || ServerL.playerOnSameTeam(player, p)) return false;
            if (p.isDead() || p.getHealth() <= 0) return false;
            if (p.isInvulnerable()) return false;
            if (p.age < 20) return false;
            if (p.distanceTo(player) > 6) return false;
            return player.canSee(p);
        }).findFirst().orElse(null);
    }

    private static float wrapDegrees(float degrees) {
        return MathHelper.wrapDegrees(degrees);
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

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame != GameDetector.ParentGame.ZOMBIES;
    }
}
