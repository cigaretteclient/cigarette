package io.github.waqfs.lib;

import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

public class Raycast {
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
}
