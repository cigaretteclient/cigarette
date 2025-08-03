package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.lib.Raycast;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;

public class ProjectileESP extends RenderModule {
    protected static final String MODULE_NAME = "ProjectileESP";
    protected static final String MODULE_TOOLTIP = "Displays the trajectory of all projectiles.";
    protected static final String MODULE_ID = "bedwars.projectileesp";
    private static final int MAX_TICKS = 200;
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private final HashSet<Projectile> projectiles = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();

    public ProjectileESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Projectile projectile : projectiles) {
            Raycast.SteppedTrajectory trajectory = projectile.trajectory;
            assert trajectory.collision != null;
            assert trajectory.collisionPos != null;
            assert trajectory.collisionStep != null;

            Vec3d start = trajectory.steps[0];
            for (int tick = 1; tick < MAX_TICKS; tick++) {
                if (tick > trajectory.collisionStep) break;
                Vec3d end = trajectory.steps[tick];
                Renderer.drawFakeLine(buffer, matrix, projectile.color, start.toVector3f(), end.toVector3f(), 0.1f);
                start = end;
            }
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);

        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.projectiles.clear();
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArrowEntity) && !(entity instanceof EggEntity) && !(entity instanceof SnowballEntity) && !(entity instanceof EnderPearlEntity))
                continue;
            if (!entity.isPushedByFluids()) continue;

            Raycast.SteppedTrajectory trajectory = Raycast.trajectory((ProjectileEntity) entity, MAX_TICKS);
            if (trajectory.collisionPos == null) continue;

            int color = 0xFFFFFFFF;
            if (trajectory.collision instanceof EntityHitResult) color = 0xFFFF0000;
            else if (entity instanceof ArrowEntity) color = 0xFF0000FF;
            else if (entity instanceof EggEntity) color = 0xFFFFFF00;
            else if (entity instanceof EnderPearlEntity) color = 0xFF00FF00;

            Projectile projectile = new Projectile(entity, trajectory, color);
            projectiles.add(projectile);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.projectiles.clear();
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return true;
//        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    private record Projectile(Entity entity, Raycast.SteppedTrajectory trajectory, int color) {
    }
}
