package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.HashSet;

public class FireballESP extends RenderModule {
    protected static final String MODULE_NAME = "FireballESP";
    protected static final String MODULE_TOOLTIP = "Displays the trajectory and blast radius of all fireballs.";
    protected static final String MODULE_ID = "bedwars.fireballesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private final HashSet<Fireball> fireballs = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();

    public FireballESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Fireball fireball : fireballs) {
            if (fireball.collisionNearPlayer) {
                Renderer.drawCube(buffer, matrix, 0x4FFF0000, fireball.collisionPathEnd, 5f);
            }
            Renderer.drawFakeLine(buffer, matrix, 0xFFFF0000, fireball.collisionPathStart.toVector3f(), fireball.collisionPathEnd.toVector3f(), 0.1f);
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);
        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.fireballs.clear();
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof FireballEntity entityfb)) continue;

            Vec3d velocity = entityfb.getVelocity();
            Vec3d start = entityfb.getPos();
            Vec3d end = start.add(velocity.multiply(1000));

            RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entityfb);
            BlockHitResult result = world.raycast(ctx);

            Fireball fireball = null;
            switch (result.getType()) {
                case BLOCK, ENTITY -> {
                    Vec3d collisionEnd = result.getPos();
                    double timeToCollision = start.distanceTo(collisionEnd) / velocity.length();
                    boolean nearPlayer = collisionEnd.distanceTo(player.getPos()) < 100;
                    fireball = new Fireball(entityfb, timeToCollision, start, collisionEnd, nearPlayer);
                }
                case MISS -> {
                    fireball = new Fireball(entityfb, -1, start, end, false);
                }
            }
            fireballs.add(fireball);
            glowContext.addGlow(entityfb.getUuid(), 0xFF0000);
        }
        ItemStack heldItem = player.getInventory().getSelectedStack();
        if (heldItem.isOf(Items.FIRE_CHARGE)) {
            Entity camera = client.getCameraEntity();
            if (camera == null) return;
            Vec3d start = player.getEyePos();
            Vec3d end = start.add(camera.getRotationVector().multiply(1000));

            RaycastContext ctx = new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, ShapeContext.absent());
            BlockHitResult result = world.raycast(ctx);
            switch (result.getType()) {
                case BLOCK, ENTITY -> {
                    Vec3d collisionEnd = result.getPos();
                    fireballs.add(new Fireball(null, -1, collisionEnd, collisionEnd, true));
                }
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.fireballs.clear();
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private static class Fireball {
        public final @Nullable FireballEntity entity;
        public final double time;
        public final Vec3d collisionPathStart;
        public final Vec3d collisionPathEnd;
        public final boolean collisionNearPlayer;

        public Fireball(@Nullable FireballEntity entity, double time, Vec3d start, Vec3d end, boolean nearPlayer) {
            this.entity = entity;
            this.time = time;
            this.collisionPathStart = start;
            this.collisionPathEnd = end;
            this.collisionNearPlayer = nearPlayer;
        }
    }
}
