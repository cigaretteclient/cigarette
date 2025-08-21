package dev.cigarette.agent;

import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Raycast;
import dev.cigarette.lib.TextL;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ZombiesAgent extends BaseAgent {
    private static final HashSet<ZombieTarget> zombies = new HashSet<>();
    private static final HashSet<Powerup> powerups = new HashSet<>();

    public ZombiesAgent(@Nullable ToggleWidget devToggle) {
        super(devToggle);
    }

    private boolean isNoClipBlock(BlockState state) {
        if (state.isIn(BlockTags.SLABS)) {
            @Nullable SlabType doubleSlab = state.getOrEmpty(Properties.SLAB_TYPE).orElse(null);
            return doubleSlab != SlabType.DOUBLE;
        }
        return state.isOf(Blocks.IRON_BARS) || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.CHEST) || state.isIn(BlockTags.SIGNS);
    }

//    ==============================
//    Syfe Aimbot

    private static final Map<UUID, Vec3d> lastPositions = new HashMap<>();
    private static final Map<UUID, Vec3d> velocities = new HashMap<>();

    @Nullable
    public static ZombiesAgent.ZombieTarget getBestTarget(ClientPlayerEntity player) {
        HashSet<ZombiesAgent.ZombieTarget> zombies = ZombiesAgent.getZombies();
        if (zombies.isEmpty()) {
            return null;
        }

        ZombiesAgent.ZombieTarget bestTarget = null;
        double minTimeToPlayer = Double.MAX_VALUE;

        Vec3d playerPos = player.getPos();

        for (ZombiesAgent.ZombieTarget zombie : zombies) {
            if (!zombie.canShoot) continue;

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
        if (!Cigarette.CONFIG.ZOMBIES_AIMBOT.predictiveAim.getRawState()) {
            return zombie.entity.getPos().subtract(player.getPos().add(0, player.getEyeHeight(EntityPose.STANDING), 0));
        }

        Vec3d currentPos = zombie.entity.getPos();
        Vec3d playerPos = player.getEyePos();
        Vec3d currentVelocity = velocities.getOrDefault(zombie.uuid, Vec3d.ZERO);
        Vec3d pathDirection = getPathfindingDirection(zombie, currentPos, playerPos, currentVelocity);

        if (pathDirection.lengthSquared() < 1.0E-7D) {
            return zombie.getEndVec();
        }

        double distance = playerPos.distanceTo(currentPos);
        double projectileSpeed = 20.0;
        double timeToTarget = (projectileSpeed > 0) ? distance / projectileSpeed : 0;

        int maxPredictionTicks = Cigarette.CONFIG.ZOMBIES_AIMBOT.predictionTicks.getRawState().intValue();
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


    private void onDisabledTick() {
        lastPositions.clear();
        velocities.clear();
    }

//    ==============================

    public static HashSet<ZombieTarget> getZombies() {
        HashSet<ZombieTarget> alive = new HashSet<>();
        for (ZombieTarget zombie : zombies) {
            if (zombie.isDead()) continue;
            alive.add(zombie);
        }
        return alive;
    }

    public static @Nullable ZombieTarget getClosestZombie() {
        ZombieTarget closest = null;
        for (ZombieTarget zombie : zombies) {
            if (!zombie.canShoot || zombie.isDead()) continue;
            if (closest == null || zombie.distance < closest.distance) {
                closest = zombie;
            }
        }
        return closest;
    }

    public static HashSet<Powerup> getPowerups() {
        HashSet<Powerup> alive = new HashSet<>();
        for (Powerup powerup : powerups) {
            if (powerup.isDead()) continue;
            alive.add(powerup);
        }
        return alive;
    }

    public static boolean isGun(ItemStack item) {
        if (item.isOf(Items.IRON_SWORD)) return false;
        return item.isIn(ItemTags.WEAPON_ENCHANTABLE) || item.isIn(ItemTags.HOES) || item.isIn(ItemTags.SHOVELS) || item.isIn(ItemTags.AXES) || item.isIn(ItemTags.PICKAXES) || item.isOf(Items.SHEARS) || item.isOf(Items.FLINT_AND_STEEL);
    }

    @Override
    protected void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        zombies.removeIf(ZombieTarget::isDead);
        powerups.removeIf(Powerup::isDead);
        powerups.forEach(Powerup::tick);

        updateAllZombieVelocities();
        for (Entity zombie : world.getEntities()) {
            if (!(zombie instanceof LivingEntity livingEntity)) continue;
            if (ZombieType.from(zombie) == ZombieType.UNKNOWN) {
                if (zombie instanceof ArmorStandEntity armorStandEntity) {
                    if (armorStandEntity.getCustomName() == null) continue;
                    String name = TextL.toColorCodedString(armorStandEntity.getCustomName());

                    Powerup.Type type;
                    switch (name) {
                        case "§r§9§lMAX AMMO" -> type = Powerup.Type.MAX_AMMO;
                        case "§r§6§lDOUBLE GOLD" -> type = Powerup.Type.DOUBLE_GOLD;
                        case "§r§c§lINSTA KILL" -> type = Powerup.Type.INSTANT_KILL;
                        case "§r§5Lucky Chest" -> type = Powerup.Type.LUCKY_CHEST;
                        default -> {
                            continue;
                        }
                    }
                    Powerup.create(armorStandEntity, armorStandEntity.getPos(), type);
                }
                continue;
            }

            ZombieTarget target = ZombieTarget.create(livingEntity);
            target.distance = player.distanceTo(zombie);

            Vec3d start = player.getPos().add(0, player.getEyeHeight(EntityPose.STANDING), 0);

            Vec3d predictedPos = calculatePredictedPosition(target, player);
            target.end = predictedPos;

            Raycast.FirstBlock result = Raycast.firstBlockCollision(start, predictedPos, this::isNoClipBlock);
            if ((result.hit() && result.whitelisted()) || result.missed()) {
                target.canShoot = true;
                target.canHeadshot = true;
            } else {
                target.canShoot = false;
                target.canHeadshot = false;
            }

            System.out.println("zombie canShoot=" + target.canShoot);
        }
        cleanupTrackingData();
    }

    @Override
    protected void onInvalidTick(MinecraftClient client) {
        zombies.clear();
        powerups.clear();
        onDisabledTick(); // Syfe Aimbot
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }

    public static class ZombieTarget {
        public final LivingEntity entity;
        public final ZombieType type;
        public final UUID uuid;
        private @Nullable Vec3d end = null;
        private float distance = 0;
        private boolean canShoot = false;
        private boolean canHeadshot = false;

        private ZombieTarget(LivingEntity entity) {
            this.entity = entity;
            this.type = ZombieType.from(entity);
            this.uuid = entity.getUuid();
        }

        public boolean isDead() {
            return this.entity.isRemoved() || this.entity.isDead() || this.entity.getHealth() <= 0.0f;
        }

        private static ZombieTarget create(LivingEntity entity) {
            for (ZombieTarget target : zombies) {
                if (target.entity == entity) return target;
            }
            ZombieTarget target = new ZombieTarget(entity);
            zombies.add(target);
            return target;
        }

        public float getDistance() {
            return this.distance;
        }

        public Vec3d getEndVec() {
            return this.end != null ? this.end : this.entity.getEyePos();
        }

        public boolean canShoot() {
            return this.canShoot;
        }

        public boolean canHeadshot() {
            return this.canHeadshot;
        }

        public Vec3d getDirectionVector(Entity player) {
            return this.getEndVec().subtract(player.getPos().add(0, player.getEyeHeight(EntityPose.STANDING), 0));
        }
    }

    public enum ZombieType {
        UNKNOWN("Unknown", 0xFFFFFFFF), ZOMBIE("Zombie", 0xFF2C936C), BLAZE("Blaze", 0xFFFCA50F), WOLF("Wolf", 0xFF3FE6FC), SKELETON("Skeleton", 0xFFE0E0E0), CREEPER("Creeper", 0xFF155B0D), MAGMACUBE("Magma Cube", 0xFFFC4619), SLIME("Slime", 0xFF155B0D), WITCH("Witch", 0xFFA625F7), ENDERMITE("Endermite", 0xFFA625F7), SILVERFISH("Silverfish", 0xFF3F3F3F);

        private final String name;
        private final int color;

        ZombieType(String name, int color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return this.name;
        }

        public int getColor() {
            return this.color;
        }

        public static ZombieType from(Entity entity) {
            if (entity instanceof ZombieEntity) return ZOMBIE;
            if (entity instanceof BlazeEntity) return BLAZE;
            if (entity instanceof WolfEntity) return WOLF;
            if (entity instanceof SkeletonEntity) return SKELETON;
            if (entity instanceof CreeperEntity) return CREEPER;
            if (entity instanceof MagmaCubeEntity) return MAGMACUBE;
            if (entity instanceof SlimeEntity) return SLIME;
            if (entity instanceof WitchEntity) return WITCH;
            if (entity instanceof EndermiteEntity) return ENDERMITE;
            if (entity instanceof SilverfishEntity) return SILVERFISH;
            return UNKNOWN;
        }
    }

    public static class Powerup {
        public final ArmorStandEntity armorStand;
        public final Vec3d position;
        public final Powerup.Type type;
        private int remainingTicks;

        private Powerup(ArmorStandEntity entity, Vec3d position, Powerup.Type type) {
            this.armorStand = entity;
            this.position = position;
            this.type = type;
            this.remainingTicks = type == Type.LUCKY_CHEST ? Integer.MAX_VALUE : 600;
        }

        private static Powerup create(ArmorStandEntity entity, Vec3d position, Powerup.Type type) {
            for (Powerup powerup : powerups) {
                if (powerup.armorStand == entity) return powerup;
            }
            Powerup powerup = new Powerup(entity, position, type);
            powerups.add(powerup);
            return powerup;
        }


        private boolean isDead() {
            return !this.armorStand.isAlive();
        }

        private void tick() {
            this.remainingTicks -= 1;
        }

        public int getRemainingTicks() {
            return this.remainingTicks;
        }

        public enum Type {
            INSTANT_KILL(0xFF0000), MAX_AMMO(0x0000FF), DOUBLE_GOLD(0xFFF800), LUCKY_CHEST(0xFC50C0);

            private final int color;

            Type(int rgb) {
                this.color = rgb;
            }

            public int getColor() {
                return this.color;
            }
        }
    }
}
