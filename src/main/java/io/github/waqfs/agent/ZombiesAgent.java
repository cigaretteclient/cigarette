package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.lib.Raycast;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
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

import java.util.HashSet;
import java.util.UUID;

public class ZombiesAgent extends BaseAgent {
    private static final HashSet<ZombieTarget> zombies = new HashSet<>();

    public ZombiesAgent(@Nullable ToggleWidget devToggle) {
        super(devToggle);
    }

    private boolean isNoClipBlock(BlockState state) {
        if (state.isIn(BlockTags.SLABS)) {
            @Nullable SlabType doubleSlab = state.getOrEmpty(Properties.SLAB_TYPE).orElse(null);
            return doubleSlab != SlabType.DOUBLE;
        }
        return state.isOf(Blocks.IRON_BARS) || state.isOf(Blocks.BARRIER) || state.isOf(Blocks.CHEST);
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
            if (!zombie.canShoot || zombie.isDead()) continue;
            if (closest == null || zombie.distance < closest.distance) {
                closest = zombie;
            }
        }
        return closest;
    }

    public static @Nullable ZombieTarget getClosestZombieTo(Entity entity, float maxAngle) {
        ZombieTarget closest = null;
        float closestAngle = Float.MAX_VALUE;
        for (ZombieTarget zombie : zombies) {
            if (!zombie.canShoot || zombie.isDead()) continue;
            float angle = zombie.angleTo(entity);
            if (angle > maxAngle || angle >= closestAngle) continue;
            closest = zombie;
            closestAngle = zombie.angleTo(entity);
        }
        return closest;
    }

    public static boolean isGun(ItemStack item) {
        if (item.isOf(Items.IRON_SWORD)) return false;
        return item.isIn(ItemTags.WEAPON_ENCHANTABLE) || item.isIn(ItemTags.HOES) || item.isIn(ItemTags.SHOVELS) || item.isIn(ItemTags.AXES) || item.isIn(ItemTags.PICKAXES);
    }

    @Override
    protected void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        zombies.removeIf(ZombieTarget::isDead);
        for (Entity zombie : world.getEntities()) {
            if (!(zombie instanceof LivingEntity livingEntity)) continue;
            if (ZombieType.from(zombie) == ZombieType.UNKNOWN) continue;

            ZombieTarget target = ZombieTarget.create(livingEntity);
            target.distance = player.distanceTo(zombie);

//            Headshot Detection
            Vec3d start = player.getPos().add(0, player.getEyeHeight(EntityPose.STANDING), 0);
            Vec3d zombieVelocity = zombie.getPos().subtract(zombie.lastX, zombie.lastY, zombie.lastZ);
            float factor = 6f * Math.min(target.distance / 30, 1);
            Vec3d end = zombie.getEyePos().add(zombieVelocity.multiply(factor, 0.2, factor));
            target.end = end;
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
            return this.entity.getHealth() <= 0.0f;
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

        public float angleTo(Entity entity) {
            if (this.end == null) return 0;
            float[] yawPitch = PlayerEntityL.getRotationVectorInDirection(this.getDirectionVector(entity));
            return PlayerEntityL.angleBetween(entity.getYaw(), entity.getPitch(), yawPitch[0], yawPitch[1]);
        }
    }

    public enum ZombieType {
        UNKNOWN(0), ZOMBIE(1), BLAZE(2), WOLF(3), SKELETON(4), CREEPER(5), MAGMACUBE(6), SLIME(7), WITCH(8), ENDERMITE(9), SILVERFISH(10);

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
            if (entity instanceof SlimeEntity) return SLIME;
            if (entity instanceof WitchEntity) return WITCH;
            if (entity instanceof EndermiteEntity) return ENDERMITE;
            if (entity instanceof SilverfishEntity) return SILVERFISH;
            return UNKNOWN;
        }
    }
}
