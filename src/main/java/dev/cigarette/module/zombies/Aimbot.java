package dev.cigarette.module.zombies;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.WeaponSelector;
import dev.cigarette.mixin.ClientWorldAccessor;
import dev.cigarette.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class Aimbot extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Aimbot";
    protected static final String MODULE_TOOLTIP = "Automatically aims at zombies.";
    protected static final String MODULE_ID = "zombies.aimbot";

    private final ToggleWidget silentAim = new ToggleWidget(Text.literal("Silent Aim"), Text.literal("Doesn't snap your camera client-side.")).withDefaultState(true);
    private final ToggleWidget autoShoot = new ToggleWidget(Text.literal("Auto Shoot"), Text.literal("Automatically shoots zombies")).withDefaultState(true);
    private final ToggleWidget autoWeaponSwitch = new ToggleWidget(Text.literal("Auto Weapon Switch"), Text.literal("Automatically switch weapons")).withDefaultState(true);
    private final ToggleWidget predictiveAim = new ToggleWidget(Text.literal("Predictive Aim"), Text.literal("Predict zombie movement for better accuracy")).withDefaultState(true);
    private final SliderWidget predictionTicks = new SliderWidget(Text.literal("Prediction Ticks"), Text.literal("How many ticks ahead to predict zombie movement")).withBounds(1, 10, 20).withAccuracy(0);

    private KeyBinding rightClickKey = null;

    private final Map<UUID, Vec3d> lastPositions = new HashMap<>();
    private final Map<UUID, Vec3d> velocities = new HashMap<>();

    public Aimbot() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(silentAim, autoShoot, autoWeaponSwitch, predictiveAim, predictionTicks);
        silentAim.registerConfigKey("zombies.aimbot.silentAim");
        autoShoot.registerConfigKey("zombies.aimbot.autoShoot");
        autoWeaponSwitch.registerConfigKey("zombies.aimbot.autoWeaponSwitch");
        predictiveAim.registerConfigKey("zombies.aimbot.predictiveAim");
        predictionTicks.registerConfigKey("zombies.aimbot.predictionTicks");
    }

    @Nullable
    private ZombiesAgent.ZombieTarget getBestTarget(ClientPlayerEntity player) {
        HashSet<ZombiesAgent.ZombieTarget> zombies = ZombiesAgent.getZombies();
        if (zombies.isEmpty()) {
            return null;
        }

        ZombiesAgent.ZombieTarget bestTarget = null;
        double minTimeToPlayer = Double.MAX_VALUE;

        Vec3d playerPos = player.getPos();

        for (ZombiesAgent.ZombieTarget zombie : zombies) {
            Vec3d zombiePos = zombie.entity.getPos();
            double distance = playerPos.distanceTo(zombiePos);

            Vec3d velocity = velocities.getOrDefault(zombie.uuid, Vec3d.ZERO);
            Vec3d playerToZombie = zombiePos.subtract(playerPos).normalize();
            double speedTowardsPlayer = -velocity.dotProduct(playerToZombie) * 20;

            if (speedTowardsPlayer <= 0.1) {
                // Use distance as a fallback metric for stationary or retreating targets
                if (bestTarget == null || distance < playerPos.distanceTo(bestTarget.entity.getPos())) {
                    bestTarget = zombie;
                }
                continue;
            }

            double timeToPlayer = distance / speedTowardsPlayer;

            if (timeToPlayer < minTimeToPlayer) {
                minTimeToPlayer = timeToPlayer;
                bestTarget = zombie;
            }
        }

        return bestTarget;
    }

    private Vec3d calculatePredictedPosition(ZombiesAgent.ZombieTarget zombie, ClientPlayerEntity player) {
        if (!predictiveAim.getRawState()) {
            return zombie.getEndVec();
        }

        Vec3d currentPos = zombie.entity.getPos();
        Vec3d playerPos = player.getEyePos();
        Vec3d currentVelocity = velocities.getOrDefault(zombie.uuid, Vec3d.ZERO);
        Vec3d pathDirection = getPathfindingDirection(zombie, currentPos, playerPos, currentVelocity);

        if (pathDirection.lengthSquared() < 1.0E-7D) {
            return zombie.getEndVec();
        }

        double distance = playerPos.distanceTo(currentPos);
        double projectileSpeed = getProjectileSpeed();
        double timeToTarget = (projectileSpeed > 0) ? distance / projectileSpeed : 0;

        int maxPredictionTicks = predictionTicks.getRawState().intValue();
        double maxPredictionTime = maxPredictionTicks / 20.0;
        timeToTarget = Math.min(timeToTarget, maxPredictionTime);

        Vec3d predictedBodyPos = currentPos.add(pathDirection.multiply(timeToTarget));

        return predictedBodyPos.add(0, zombie.entity.getEyeHeight(zombie.entity.getPose()), 0);
    }

    private Vec3d getPathfindingDirection(ZombiesAgent.ZombieTarget zombie, Vec3d zombiePos, Vec3d playerPos, Vec3d currentVelocity) {
        Vec3d directPath = playerPos.subtract(zombiePos).normalize();

        if (currentVelocity.lengthSquared() > 1.0E-7D) {
            Vec3d normalizedVelocity = currentVelocity.normalize();
            double directness = normalizedVelocity.dotProduct(directPath);
            directness = Math.max(0, Math.min(1, directness));

            return directPath.multiply(1.0 - directness).add(normalizedVelocity.multiply(directness)).normalize().multiply(currentVelocity.length());
        }

        return directPath.multiply(estimateZombieSpeed(zombie) / 20.0);
    }

    private double estimateZombieSpeed(ZombiesAgent.ZombieTarget zombie) {
        return switch (zombie.type) {
            case ZOMBIE, SKELETON, CREEPER, WITCH -> 5.0; // ~0.25 B/t * 20 t/s
            case BLAZE -> 8.0;
            case WOLF -> 7.0;
            case MAGMACUBE, SLIME -> 4.0;
            case ENDERMITE, SILVERFISH -> 6.0;
            default -> 5.0;
        };
    }

    private double getProjectileSpeed() {
        return 20.0;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        updateAllZombieVelocities();

        if (rightClickKey.isPressed() || autoShoot.getRawState()) {
            if (ZombiesAgent.getZombies().isEmpty()) return;

            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                BlockState lookingAt = world.getBlockState(blockResult.getBlockPos());
                if (lookingAt.isIn(BlockTags.BUTTONS) || lookingAt.isOf(Blocks.CHEST)) return;
            }

            ZombiesAgent.ZombieTarget bestTarget = getBestTarget(player);
            if (bestTarget == null) return;

            if (autoWeaponSwitch.getRawState()) {
                WeaponSelector.switchToBestWeapon(player, bestTarget);
            }

            if (!ZombiesAgent.isGun(player.getMainHandStack())) return;

            Vec3d predictedPos = calculatePredictedPosition(bestTarget, player);
            Vec3d vector = predictedPos.subtract(player.getEyePos()).normalize();

            WeaponSelector.addCooldown(player.getInventory().getSelectedSlot());

            float aimYaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
            float aimPitch = (float) Math.toDegrees(Math.asin(-vector.y));

            if (!silentAim.getRawState()) {
                PlayerEntityL.setRotationVector(player, vector);
            }

            ClientWorldAccessor clientWorldAccessor = (ClientWorldAccessor) world;

//            player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(aimYaw, aimPitch, player.isOnGround(), player.horizontalCollision));

            try (PendingUpdateManager pendingUpdateManager = clientWorldAccessor.getPendingUpdateManager().incrementSequence()) {
                int seq = pendingUpdateManager.getSequence();
                player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, aimYaw, aimPitch));
            }
        }

        cleanupTrackingData();
    }

    private void updateAllZombieVelocities() {
        HashSet<ZombiesAgent.ZombieTarget> zombies = ZombiesAgent.getZombies();
        if (zombies.isEmpty()) {
            velocities.clear();
            return;
        }

        for (ZombiesAgent.ZombieTarget zombie : zombies) {
            UUID zombieId = zombie.uuid;
            Vec3d currentPos = zombie.entity.getPos();
            Vec3d lastPos = lastPositions.get(zombieId);

            if (lastPos != null) {
                velocities.put(zombieId, currentPos.subtract(lastPos));
            }
            lastPositions.put(zombieId, currentPos);
        }
    }

    private void cleanupTrackingData() {
        Set<UUID> currentZombies = new HashSet<>();
        for (ZombiesAgent.ZombieTarget zombie : ZombiesAgent.getZombies()) {
            currentZombies.add(zombie.uuid);
        }

        lastPositions.keySet().retainAll(currentZombies);
        velocities.keySet().retainAll(currentZombies);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        lastPositions.clear();
        velocities.clear();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
