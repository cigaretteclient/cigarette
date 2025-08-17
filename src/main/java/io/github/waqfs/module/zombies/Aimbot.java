package io.github.waqfs.module.zombies;

import io.github.waqfs.Cigarette;
import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.gui.widget.SliderWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.lib.WeaponSelector;
import io.github.waqfs.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

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
    private final Map<UUID, Vec3d> pathfindingTargets = new HashMap<>();

    public Aimbot() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(silentAim, autoShoot, autoWeaponSwitch, predictiveAim, predictionTicks);
        silentAim.registerConfigKey("zombies.aimbot.silentAim");
        autoShoot.registerConfigKey("zombies.aimbot.autoShoot");
        autoWeaponSwitch.registerConfigKey("zombies.aimbot.autoWeaponSwitch");
        predictiveAim.registerConfigKey("zombies.aimbot.predictiveAim");
        predictionTicks.registerConfigKey("zombies.aimbot.predictionTicks");
    }

    private Vec3d calculatePredictedPosition(ZombiesAgent.ZombieTarget zombie, ClientPlayerEntity player) {
        if (!predictiveAim.getRawState()) {
            return zombie.getEndVec();
        }

        UUID zombieId = zombie.uuid;
        Vec3d currentPos = zombie.entity.getPos();
        Vec3d playerPos = player.getPos();

        // Update pathfinding target (zombies always pathfind toward player)
        pathfindingTargets.put(zombieId, playerPos);

        // Update position tracking
        Vec3d lastPos = lastPositions.get(zombieId);
        Vec3d currentVelocity = Vec3d.ZERO;

        if (lastPos != null) {
            currentVelocity = currentPos.subtract(lastPos);
            velocities.put(zombieId, currentVelocity);
        }
        lastPositions.put(zombieId, currentPos);

        // Calculate pathfinding-based prediction
        Vec3d pathDirection = getPathfindingDirection(zombie, currentPos, playerPos, currentVelocity);

        if (pathDirection.equals(Vec3d.ZERO)) {
            return zombie.getEndVec(); // Fallback to current headshot position
        }

        // Calculate prediction time based on distance and projectile speed
        double distance = playerPos.distanceTo(currentPos);
        double projectileSpeed = getProjectileSpeed();
        double timeToTarget = distance / projectileSpeed;

        // Limit prediction time based on slider setting
        int maxPredictionTicks = predictionTicks.getRawState().intValue();
        double maxPredictionTime = maxPredictionTicks / 20.0;
        timeToTarget = Math.min(timeToTarget, maxPredictionTime);

        double zombieSpeed = estimateZombieSpeed(zombie, currentVelocity);
        Vec3d predictedBodyPos = currentPos.add(pathDirection.normalize().multiply(zombieSpeed * timeToTarget * 20));

        Vec3d predictedHeadPos = predictedBodyPos.add(0, zombie.entity.getEyeHeight(zombie.entity.getPose()), 0);

        return predictedHeadPos;
    }

    private Vec3d getPathfindingDirection(ZombiesAgent.ZombieTarget zombie, Vec3d zombiePos, Vec3d playerPos, Vec3d currentVelocity) {
        Vec3d directPath = playerPos.subtract(zombiePos).normalize();

        if (currentVelocity.length() > 0.01) {
            Vec3d normalizedVelocity = currentVelocity.normalize();

            return directPath.multiply(0.7).add(normalizedVelocity.multiply(0.3));
        }

        return directPath;
    }

    private double estimateZombieSpeed(ZombiesAgent.ZombieTarget zombie, Vec3d currentVelocity) {
        if (currentVelocity.length() > 0.01) {
            return currentVelocity.length();
        }
        return switch (zombie.type) {
            case ZOMBIE -> 0.25; // Normal zombie speed
            case BLAZE -> 0.4;   // Blazes are faster
            case WOLF -> 0.35;   // Wolves are fast
            case SKELETON -> 0.25; // Similar to zombies
            case CREEPER -> 0.25; // Similar to zombies
            case MAGMACUBE, SLIME -> 0.2; // Slimes are slower
            case WITCH -> 0.25; // Similar to zombies
            case ENDERMITE, SILVERFISH -> 0.3; // Small but fast
            default -> 0.25; // Default speed
        };
    }

    private double getProjectileSpeed() {
        return 10.0;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }

        if (rightClickKey.isPressed() || autoShoot.getRawState()) {
            if (ZombiesAgent.getZombies().isEmpty()) return;

            // check if we're looking at a clickable
            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                BlockState lookingAt = world.getBlockState(blockResult.getBlockPos());
                if (lookingAt.isIn(BlockTags.BUTTONS) || lookingAt.isOf(Blocks.CHEST)) return;
            }

            ZombiesAgent.ZombieTarget closest = ZombiesAgent.getClosestZombie();
            if (closest == null) return;

            if (autoWeaponSwitch.getRawState()) {
                WeaponSelector.switchToBestWeapon(MinecraftClient.getInstance().player, closest);
            }

            if (ZombiesAgent.isGun(player.getMainHandStack())) {
                Vec3d predictedPos = calculatePredictedPosition(closest, player);
                Vec3d vector = predictedPos.subtract(player.getEyePos()).normalize();

                WeaponSelector.addCooldown(player.getInventory().getSelectedSlot());

                if (silentAim.getRawState()) {
                    float aimYaw = (float) Math.toDegrees(Math.atan2(-vector.x, vector.z));
                    float aimPitch = (float) Math.toDegrees(Math.asin(-vector.y));

                    float realYaw = player.getYaw();
                    float realPitch = player.getPitch();

                    player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(aimYaw, aimPitch, player.isOnGround(), player.horizontalCollision));

                    PlayerInteractItemC2SPacket shootPacket = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, (int) world.getTickOrder(), aimYaw, aimPitch);
                    player.networkHandler.sendPacket(shootPacket);

                    player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(realYaw, realPitch, player.isOnGround(), player.horizontalCollision));

                } else {
                    PlayerEntityL.setRotationVector(player, vector);

                    float yaw = player.getYaw();
                    float pitch = player.getPitch();
                    PlayerInteractItemC2SPacket shootPacket = new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, (int) world.getTickOrder(), yaw, pitch);
                    player.networkHandler.sendPacket(shootPacket);
                }
            }
        }

        // Clean up old tracking data for zombies that no longer exist
        cleanupTrackingData();
    }

    private void cleanupTrackingData() {
        // Get current zombie UUIDs
        HashSet<UUID> currentZombies = new HashSet<>();
        for (ZombiesAgent.ZombieTarget zombie : ZombiesAgent.getZombies()) {
            currentZombies.add(zombie.uuid);
        }

        // Remove tracking data for zombies that no longer exist
        lastPositions.keySet().retainAll(currentZombies);
        velocities.keySet().retainAll(currentZombies);
        pathfindingTargets.keySet().retainAll(currentZombies);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        // Clear tracking data when module is disabled
        lastPositions.clear();
        velocities.clear();
        pathfindingTargets.clear();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
