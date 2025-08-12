package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Raycast;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.UUID;

public class ZombiesAgent extends BaseAgent {
    private static final HashSet<ZombieTarget> zombies = new HashSet<>();

    public ZombiesAgent(@Nullable ToggleWidget devToggle) {
        super(devToggle);
    }

    private boolean isNoClipBlock(BlockState state) {
        return state.isIn(BlockTags.SLABS);
    }

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
            if (!zombie.canShoot) continue;
            if (closest == null || zombie.distance < closest.distance) {
                closest = zombie;
            }
        }
        return closest;
    }

    @Override
    protected void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        zombies.removeIf(ZombieTarget::isDead);
        for (Entity zombie : world.getEntities()) {
            if (ZombieType.from(zombie) == ZombieType.UNKNOWN) continue;

            ZombieTarget target = ZombieTarget.create(zombie);
            target.distance = player.distanceTo(zombie);

//            Headshot Detection
            Vec3d start = player.getEyePos();
            Vec3d end = zombie.getEyePos();
            Raycast.FirstBlock result = Raycast.firstBlockCollision(start, end, this::isNoClipBlock);
            if ((result.hit() && result.whitelisted()) || result.missed()) {
                target.canShoot = true;
                target.canHeadshot = true;
            } else {
                target.canShoot = false;
                target.canHeadshot = false;
            }
        }
    }

    @Override
    protected void onInvalidTick(MinecraftClient client) {
        zombies.clear();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }

    public static class ZombieTarget {
        public final Entity entity;
        public final ZombieType type;
        public final UUID uuid;
        private float distance = 0;
        private boolean canShoot = false;
        private boolean canHeadshot = false;

        private ZombieTarget(Entity entity) {
            this.entity = entity;
            this.type = ZombieType.from(entity);
            this.uuid = entity.getUuid();
        }

        public boolean isDead() {
            return !this.entity.isAlive();
        }

        private static ZombieTarget create(Entity entity) {
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

        public boolean canShoot() {
            return this.canShoot;
        }

        public boolean canHeadshot() {
            return this.canHeadshot;
        }

        public Direction getDirectionVector(ClientPlayerEntity player) {
            return Direction.getFacing(entity.getEyePos().subtract(player.getEyePos()));
        }
    }

    public enum ZombieType {
        UNKNOWN(0), ZOMBIE(1), BLAZE(2), WOLF(3), SKELETON(4), CREEPER(5), MAGMACUBE(6), SLIME(7), WITCH(8);

        private final int id;

        ZombieType(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static ZombieType from(Entity entity) {
            if (entity instanceof ZombieEntity) return ZOMBIE;
            if (entity instanceof BlazeEntity) return BLAZE;
            if (entity instanceof WolfEntity) return WOLF;
            if (entity instanceof SkeletonEntity) return SKELETON;
            if (entity instanceof CreeperEntity) return CREEPER;
            if (entity instanceof MagmaCubeEntity) return MAGMACUBE;
            if (entity instanceof WitchEntity) return WITCH;
            return UNKNOWN;
        }
    }
}
