package io.github.waqfs.lib;

import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class Raycast {
    private static final double GRAVITY = -0.05;
    private static final double DEFAULT_DRAG = 0.99;
    private static final double FLUID_DRAG = 0.6;

    public static HitResult first(BlockHitResult blockResult, @Nullable EntityHitResult entityResult, Vec3d source) {
        return entityResult != null && entityResult.getPos().squaredDistanceTo(source) < blockResult.getPos().squaredDistanceTo(source) ? entityResult : blockResult;
    }

    public static HitResult raycast(Vec3d start, Vec3d end, Entity entity) {
        BlockHitResult blockResult = raycastBlock(start, end, entity);
        EntityHitResult entityResult = raycastEntity(start, end);
        return first(blockResult, entityResult, start);
    }

    public static HitResult raycast(Vec3d start, Vec3d end, ShapeContext shape) {
        BlockHitResult blockResult = raycastBlock(start, end, shape);
        EntityHitResult entityResult = raycastEntity(start, end);
        return first(blockResult, entityResult, start);
    }

    public static EntityHitResult raycastEntity(Vec3d start, Vec3d end) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        double distance = start.distanceTo(end);
        return ProjectileUtil.raycast(player, start, end, new Box(start, end).expand(3), EntityPredicates.CAN_HIT, distance);
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

    public static @Nullable EntityHitResult withEntity(Entity entity, Box entityRegion, Box region) {
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

    public static SteppedTrajectory arrowTrajectory(ArrowEntity entity, int ticks) {
        ClientWorld world = MinecraftClient.getInstance().world;
        assert ticks > 0;
        assert world != null;

        SteppedTrajectory trajectory = new SteppedTrajectory(ticks);

        Vec3d previousPosition = entity.getPos();
        Vec3d previousVelocity = entity.getVelocity();
        trajectory.steps[0] = previousPosition;

        boolean collided = false;
        for (int tick = 0; tick < ticks; tick++) {
            Vec3d position = previousPosition.add(previousVelocity);

            EntityHitResult entityResult = withEntity(entity, position, 1 / 16d);
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
            previousVelocity = previousVelocity.multiply(inFluid ? FLUID_DRAG : DEFAULT_DRAG).add(0, GRAVITY, 0);
        }

        return trajectory;
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
