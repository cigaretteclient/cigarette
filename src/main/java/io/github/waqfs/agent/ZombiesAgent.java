package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Raycast;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
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
        return state.isOf(Blocks.IRON_BARS);
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

    public static boolean isGun(ItemStack item) {
        if (item.isOf(Items.IRON_SWORD)) return false;
        return item.isIn(ItemTags.WEAPON_ENCHANTABLE) || item.isIn(ItemTags.HOES) || item.isIn(ItemTags.SHOVELS) || item.isIn(ItemTags.AXES) || item.isIn(ItemTags.PICKAXES);
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
            Vec3d end = zombie.getEyePos().add(zombie.getVelocity().multiply(5));
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
        public final Entity entity;
        public final ZombieType type;
        public final UUID uuid;
        private @Nullable Vec3d end = null;
        private float distance = 0;
        private boolean canShoot = false;
        private boolean canHeadshot = false;

        private ZombieTarget(Entity entity) {
            this.entity = entity;
            this.type = ZombieType.from(entity);
            this.uuid = entity.getUuid();
        }

        public boolean isDead() {
            return !this.entity.isAlive() || (this.entity.getEyeY() - this.entity.getY() < 0.1);
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

        public Vec3d getEndVec() {
            return this.end != null ? this.end : this.entity.getEyePos();
        }

        public boolean canShoot() {
            return this.canShoot;
        }

        public boolean canHeadshot() {
            return this.canHeadshot;
        }

        public Vec3d getDirectionVector(ClientPlayerEntity player) {
            return this.getEndVec().subtract(player.getEyePos());
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
            if (entity instanceof WitchEntity) return WITCH;
            if (entity instanceof EndermiteEntity) return ENDERMITE;
            if (entity instanceof SilverfishEntity) return SILVERFISH;
            return UNKNOWN;
        }
    }
}
