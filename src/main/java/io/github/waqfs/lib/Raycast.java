package io.github.waqfs.lib;

import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class Raycast {
    private static final double ARROW_GRAVITY = -0.05;
    private static final double ITEM_GRAVITY = -0.03;
    private static final double DEFAULT_DRAG = 0.99;
    private static final double FLUID_ARROW_DRAG = 0.6;
    private static final double FLUID_ITEM_DRAG = 0.8;
    private static final int MIN_PROJECTION_TICKS = 1;

    public static HitResult first(BlockHitResult blockResult, @Nullable EntityHitResult entityResult, Vec3d source) {
        return entityResult != null && entityResult.getPos().squaredDistanceTo(source) < blockResult.getPos().squaredDistanceTo(source) ? entityResult : blockResult;
    }

    public static HitResult raycast(Vec3d start, Vec3d end, Entity entity) {
        BlockHitResult blockResult = raycastBlock(start, end, entity);
        EntityHitResult entityResult = raycastEntity(start, end, entity);
        return first(blockResult, entityResult, start);
    }

    public static HitResult raycast(Vec3d start, Vec3d end, ShapeContext shape) {
        BlockHitResult blockResult = raycastBlock(start, end, shape);
        EntityHitResult entityResult = raycastEntity(start, end, null);
        return first(blockResult, entityResult, start);
    }

    public static EntityHitResult raycastEntity(Vec3d start, Vec3d end, @Nullable Entity excludedEntity) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        assert cameraEntity != null;

        double distance = start.distanceTo(end);
        return ProjectileUtil.raycast(excludedEntity != null ? excludedEntity : cameraEntity, start, end, new Box(start, end).expand(3), EntityPredicates.CAN_HIT, distance);
    }

    public static BlockHitResult raycastBlock(Vec3d start, Vec3d end, ShapeContext shape) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, shape);
        return world.raycast(ctx);
    }

    public static BlockHitResult raycastBlock(Vec3d start, Vec3d end, Entity entity) {
        return raycastBlock(start, end, ShapeContext.of(entity));
    }

    public static @Nullable EntityHitResult withEntity(@Nullable Entity entity, Box entityRegion, Box region) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        for (Entity target : world.getOtherEntities(entity, entityRegion, EntityPredicates.CAN_HIT)) {
            Box targetRegion = target.getBoundingBox();
            if (targetRegion.intersects(region)) return new EntityHitResult(target);
        }
        return null;
    }

    public static @Nullable EntityHitResult withEntity(Entity entity, Vec3d position, double padding) {
        Box box = new Box(position.subtract(3), position.add(3));
        Box region = entity.getBoundingBox();
        region = region.offset(region.getCenter().negate()).offset(position).expand(padding);
        return withEntity(entity, box, region);
    }

    public static SteppedTrajectory trajectory(ProjectileEntity entity, int ticks) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert ticks > 0;
        assert world != null;

        boolean isArrow = entity instanceof PersistentProjectileEntity;
        SteppedTrajectory trajectory = new SteppedTrajectory(ticks);

        Vec3d previousPosition = entity.getPos();
        Vec3d previousVelocity = entity.getVelocity();
        trajectory.steps[0] = previousPosition;

        boolean collided = false;
        for (int tick = 0; tick < ticks; tick++) {
            Vec3d position = previousPosition.add(previousVelocity);

            EntityHitResult entityResult = withEntity(entity, position, 1 / 12d);
            if (entityResult != null) {
                trajectory.collision = entityResult;
                trajectory.collisionPos = position;
                trajectory.collisionStep = tick;
                collided = true;
            } else {
                BlockHitResult blockResult = raycastBlock(previousPosition, position, ShapeContext.absent());
                if (blockResult.getType() == HitResult.Type.BLOCK) {
                    position = blockResult.getPos();
                    trajectory.collision = blockResult;
                    trajectory.collisionPos = blockResult.getPos();
                    trajectory.collisionStep = tick;
                    collided = true;
                }
            }

            trajectory.steps[tick] = position;
            previousPosition = position;
            if (collided) break;

            boolean inFluid = world.getBlockState(new BlockPos((int) position.x, (int) position.y, (int) position.z)).isOf(Blocks.WATER);
            previousVelocity = isArrow ? previousVelocity.multiply(inFluid ? FLUID_ARROW_DRAG : DEFAULT_DRAG).add(0, ARROW_GRAVITY, 0) : previousVelocity.add(0, ITEM_GRAVITY, 0).multiply(inFluid ? FLUID_ITEM_DRAG : DEFAULT_DRAG);
        }

        return trajectory;
    }

    public static @Nullable SteppedTrajectory trajectory(PlayerEntity player, ItemStack item, int ticks) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert ticks > MIN_PROJECTION_TICKS;
        assert world != null;

        boolean isArrow = item.isOf(Items.BOW);
        SteppedTrajectory trajectory = new SteppedTrajectory(ticks - MIN_PROJECTION_TICKS);

        Vec3d previousPosition = player.getEyePos();
        float pullProgress = isArrow ? BowItem.getPullProgress(player.getItemUseTime()) : 1f;
        if (isArrow && pullProgress < 0.1f) return null;
        Vec3d previousVelocity = isArrow ? computeProjectileVelocity(player, pullProgress * 3f) : computeProjectileVelocity(player, 1.5f);

        boolean collided = false;
        for (int tick = 0; tick < ticks; tick++) {
            Vec3d position = previousPosition.add(previousVelocity);

            EntityHitResult entityResult = withEntity(player, position, 1 / 12d);
            if (entityResult != null) {
                trajectory.collision = entityResult;
                trajectory.collisionPos = position;
                trajectory.collisionStep = tick - MIN_PROJECTION_TICKS;
                collided = true;
            } else {
                BlockHitResult blockResult = raycastBlock(previousPosition, position, ShapeContext.absent());
                if (blockResult.getType() == HitResult.Type.BLOCK) {
                    position = blockResult.getPos();
                    trajectory.collision = blockResult;
                    trajectory.collisionPos = blockResult.getPos();
                    trajectory.collisionStep = tick - MIN_PROJECTION_TICKS;
                    collided = true;
                }
            }

            if (tick >= MIN_PROJECTION_TICKS) trajectory.steps[tick - MIN_PROJECTION_TICKS] = position;
            previousPosition = position;
            if (collided) {
                if (tick < MIN_PROJECTION_TICKS) return null;
                break;
            }

            boolean inFluid = world.getBlockState(new BlockPos((int) position.x, (int) position.y, (int) position.z)).isOf(Blocks.WATER);
            previousVelocity = isArrow ? previousVelocity.multiply(inFluid ? FLUID_ARROW_DRAG : DEFAULT_DRAG).add(0, ARROW_GRAVITY, 0) : previousVelocity.add(0, ITEM_GRAVITY, 0).multiply(inFluid ? FLUID_ITEM_DRAG : DEFAULT_DRAG);
        }

        return trajectory;
    }

    private static Vec3d computeProjectileVelocity(Entity shooter, float speed) {
        float f = -MathHelper.sin(shooter.getYaw() * (float) (Math.PI / 180.0)) * MathHelper.cos(shooter.getPitch() * (float) (Math.PI / 180.0));
        float g = -MathHelper.sin((shooter.getPitch() + 0.0f) * (float) (Math.PI / 180.0));
        float h = MathHelper.cos(shooter.getYaw() * (float) (Math.PI / 180.0)) * MathHelper.cos(shooter.getPitch() * (float) (Math.PI / 180.0));
        Vec3d movement = shooter.getMovement();
        return new Vec3d(f, g, h).normalize().multiply(speed).add(movement.x, shooter.isOnGround() ? 0.0 : movement.y, movement.z);
    }

    public static class SteppedTrajectory {
        public final Vec3d[] steps;
        public @Nullable HitResult collision = null;
        public @Nullable Vec3d collisionPos = null;
        public @Nullable Integer collisionStep = null;

        SteppedTrajectory(int ticks) {
            this.steps = new Vec3d[ticks];
        }
    }
}
