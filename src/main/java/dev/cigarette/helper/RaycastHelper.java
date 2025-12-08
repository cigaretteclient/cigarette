package dev.cigarette.helper;

import net.minecraft.block.BlockState;
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
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Helper class for raycasting and projectile trajectory calculations.
 */
public class RaycastHelper {
    private static final double ARROW_GRAVITY = -0.05;
    private static final double ITEM_GRAVITY = -0.03;
    private static final double DEFAULT_DRAG = 0.99;
    private static final double FLUID_ARROW_DRAG = 0.6;
    private static final double FLUID_ITEM_DRAG = 0.8;
    private static final int MIN_PROJECTION_TICKS = 1;

    /**
     * {@return the hit result that would have occurred first between a block hit and an entity hit}
     *
     * @param blockResult  The block hit result.
     * @param entityResult The entity hit result.
     * @param source       The source position from which the raycast was performed.
     */
    public static HitResult first(BlockHitResult blockResult, @Nullable EntityHitResult entityResult, Vec3d source) {
        return entityResult != null && entityResult.getPos().squaredDistanceTo(source) < blockResult.getPos().squaredDistanceTo(source) ? entityResult : blockResult;
    }

    /**
     * Performs a raycast between two points, checking for both block and entity collisions, returning the first to occur.
     *
     * @param start  The start position.
     * @param end    The end position.
     * @param entity The entity whose shape context will be used for block collisions and also ignored for entity collisions.
     * @return The hit result (either block or entity) that occurs first.
     */
    public static HitResult raycast(Vec3d start, Vec3d end, Entity entity) {
        BlockHitResult blockResult = raycastBlock(start, end, entity);
        EntityHitResult entityResult = raycastEntity(start, end, entity);
        return first(blockResult, entityResult, start);
    }

    /**
     * Performs a raycast between two points, checking for both block and entity collisions, returning the first to occur.
     *
     * @param start The start position.
     * @param end   The end position.
     * @param shape The shape context.
     * @return The hit result (either block or entity) that occurs first.
     */
    public static HitResult raycast(Vec3d start, Vec3d end, ShapeContext shape) {
        BlockHitResult blockResult = raycastBlock(start, end, shape);
        EntityHitResult entityResult = raycastEntity(start, end, null);
        return first(blockResult, entityResult, start);
    }

    /**
     * Performs a simple entity raycast between two points, excluding {@code excludedEntity} from the results.
     *
     * @param start          The start position.
     * @param end            The end position.
     * @param excludedEntity An excluded entity, or null to include all entities.
     * @return The entity hit result.
     */
    public static EntityHitResult raycastEntity(Vec3d start, Vec3d end, @Nullable Entity excludedEntity) {
        Entity cameraEntity = MinecraftClient.getInstance().cameraEntity;
        assert cameraEntity != null;

        double distance = start.distanceTo(end);
        return ProjectileUtil.raycast(excludedEntity != null ? excludedEntity : cameraEntity, start, end, new Box(start, end).expand(3), EntityPredicates.CAN_HIT, distance);
    }

    /**
     * Performs a simple block raycast between two points.
     *
     * @param start The start position.
     * @param end   The end position.
     * @param shape The shape context.
     * @return The block hit result.
     */
    public static BlockHitResult raycastBlock(Vec3d start, Vec3d end, ShapeContext shape) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, shape);
        return world.raycast(ctx);
    }

    /**
     * Performs a simple block raycast between two points, using the entity's shape context for colliding with blocks.
     *
     * @param start  The start position.
     * @param end    The end position.
     * @param entity The entity whose shape context will be used.
     * @return The block hit result.
     */
    public static BlockHitResult raycastBlock(Vec3d start, Vec3d end, Entity entity) {
        return raycastBlock(start, end, ShapeContext.of(entity));
    }

    /**
     * Raycasts between two points, returning the first block hit and whether it is contained in the {@code whitelist} predicate.
     *
     * @param start     The start position.
     * @param end       The end position.
     * @param whitelist A predicate to test whether a block is whitelisted.
     * @return A {@link FirstBlock} record containing information about the first block hit.
     */
    public static FirstBlock firstBlockCollision(Vec3d start, Vec3d end, Predicate<BlockState> whitelist) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        BlockHitResult res = BlockView.raycast(start, end, new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, ShapeContext.absent()), (innerContext, pos) -> {
            BlockState blockState = world.getBlockState(pos);
            Vec3d innerStart = innerContext.getStart();
            Vec3d innerEnd = innerContext.getEnd();
            VoxelShape voxelShape = innerContext.getBlockShape(blockState, world, pos);

            if (whitelist.test(blockState)) {
                Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
                return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
            }

            BlockHitResult res1 = voxelShape.raycast(innerStart, innerEnd, pos);
            if (res1 != null) {
                BlockHitResult res2 = blockState.getRaycastShape(world, pos).raycast(start, end, pos);
                if (res2 != null && res2.getPos().subtract(start).lengthSquared() < res1.getPos().subtract(start).lengthSquared()) {
                    return res1.withSide(res2.getSide());
                }
            }
            return res1;
        }, (innerContext) -> {
            Vec3d vec3d = innerContext.getStart().subtract(innerContext.getEnd());
            return BlockHitResult.createMissed(innerContext.getEnd(), Direction.getFacing(vec3d.x, vec3d.y, vec3d.z), BlockPos.ofFloored(innerContext.getEnd()));
        });

        boolean missed = res.getType() == HitResult.Type.MISS;
        boolean hit = !missed;
        boolean whitelisted = hit && whitelist.test(world.getBlockState(res.getBlockPos()));
        return new FirstBlock(hit, whitelisted, missed);
    }

    /**
     * Performs an entity collision check within a specified region.
     *
     * @param entity       The entity to exclude from the check, or null to include all entities, usually the source entity.
     * @param entityRegion The region to search for entities.
     * @param region       The region to check for intersections, usually the bounding box of the source entity.
     * @return The entity hit result, or null if no entity intersects.
     */
    public static @Nullable EntityHitResult withEntity(@Nullable Entity entity, Box entityRegion, Box region) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert world != null;

        for (Entity target : world.getOtherEntities(entity, entityRegion, EntityPredicates.CAN_HIT)) {
            Box targetRegion = target.getBoundingBox();
            if (targetRegion.intersects(region)) return new EntityHitResult(target);
        }
        return null;
    }

    /**
     * Performs an entity collision check for {@code entity} at a specific position.
     *
     * @param entity   The entity to pull the bounding box from.
     * @param position The position to check for collisions.
     * @param padding  Padding to apply to the entity's bounding box to expand the search region.
     * @return The entity hit result, or null if no entity intersects.
     */
    public static @Nullable EntityHitResult withEntity(Entity entity, Vec3d position, double padding) {
        Box box = new Box(position.subtract(3), position.add(3));
        Box region = entity.getBoundingBox();
        region = region.offset(region.getCenter().negate()).offset(position).expand(padding);
        return withEntity(entity, box, region);
    }

    /**
     * Computes the {@link SteppedTrajectory} of a projectile entity over a specified number of {@code ticks}.
     *
     * @param entity The projectile entity.
     * @param ticks  The number of ticks to project.
     * @return The computed {@link SteppedTrajectory}.
     */
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

    /**
     * Computes the {@link SteppedTrajectory} of a projectile {@code item} that would be launched by the {@code player} over a specified number of {@code ticks}.
     *
     * @param player The player that would be launching the projectile.
     * @param item   The projectile item.
     * @param ticks  The number of ticks to project.
     * @return The computed {@link SteppedTrajectory}, or null if there is immediate collision or if the projectile is a bow that is not being pulled.
     */
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

    /**
     * Computes the velocity vector for a projectile launched by the {@code shooter} at the specified {@code speed}.
     *
     * @param shooter The entity launching the projectile.
     * @param speed   The speed of the projectile.
     * @return The velocity vector.
     */
    private static Vec3d computeProjectileVelocity(Entity shooter, float speed) {
        float f = -MathHelper.sin(shooter.getYaw() * (float) (Math.PI / 180.0)) * MathHelper.cos(shooter.getPitch() * (float) (Math.PI / 180.0));
        float g = -MathHelper.sin((shooter.getPitch() + 0.0f) * (float) (Math.PI / 180.0));
        float h = MathHelper.cos(shooter.getYaw() * (float) (Math.PI / 180.0)) * MathHelper.cos(shooter.getPitch() * (float) (Math.PI / 180.0));
        Vec3d movement = shooter.getMovement();
        return new Vec3d(f, g, h).normalize().multiply(speed).add(movement.x, shooter.isOnGround() ? 0.0 : movement.y, movement.z);
    }

    /**
     * A sequence of projectile positions over time, along with when (if any) collision occurs.
     */
    public static class SteppedTrajectory {
        public final Vec3d[] steps;
        public @Nullable HitResult collision = null;
        public @Nullable Vec3d collisionPos = null;
        public @Nullable Integer collisionStep = null;

        SteppedTrajectory(int ticks) {
            this.steps = new Vec3d[ticks];
        }
    }

    public record FirstBlock(boolean hit, boolean whitelisted, boolean missed) {
    }
}
