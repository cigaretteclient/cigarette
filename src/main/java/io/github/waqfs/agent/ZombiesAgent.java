package io.github.waqfs.agent;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Raycast;
import io.github.waqfs.lib.TextL;
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

import java.util.HashSet;
import java.util.UUID;

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
        return item.isIn(ItemTags.WEAPON_ENCHANTABLE) || item.isIn(ItemTags.HOES) || item.isIn(ItemTags.SHOVELS) || item.isIn(ItemTags.AXES) || item.isIn(ItemTags.PICKAXES) || item.isOf(Items.SHEARS);
    }

    @Override
    protected void onValidTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        zombies.removeIf(ZombieTarget::isDead);
        powerups.removeIf(Powerup::isDead);
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

    public static class Powerup {
        public final ArmorStandEntity armorStand;
        public final Vec3d position;
        public final Powerup.Type type;

        private Powerup(ArmorStandEntity entity, Vec3d position, Powerup.Type type) {
            this.armorStand = entity;
            this.position = position;
            this.type = type;
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
