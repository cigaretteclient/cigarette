package io.github.waqfs.module.bedwars;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;

public class ArrowESP extends RenderModule {
    protected static final String MODULE_NAME = "ArrowESP";
    protected static final String MODULE_TOOLTIP = "Displays the trajectory of all arrows.";
    protected static final String MODULE_ID = "bedwars.arrowesp";
    private static final int MAX_TICKS = 200;
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));
    private final HashSet<Raycast.SteppedTrajectory> arrows = new HashSet<>();
    private final Glow.Context glowContext = new Glow.Context();

    public ArrowESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Raycast.SteppedTrajectory arrow : arrows) {
            assert arrow.collision != null;
            assert arrow.collisionPos != null;
            assert arrow.collisionStep != null;

            Vec3d start = arrow.steps[0];
            int color = arrow.collision instanceof EntityHitResult ? 0xFFFF0000 : 0xFF0000FF;
            for (int tick = 1; tick < MAX_TICKS; tick++) {
                if (tick > arrow.collisionStep) break;
                Vec3d end = arrow.steps[tick];
                Renderer.drawFakeLine(buffer, matrix, color, start.toVector3f(), end.toVector3f(), 0.1f);
                start = end;
            }
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);

        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.arrows.clear();
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof ArrowEntity entityArrow)) continue;
            if (!entityArrow.isPushedByFluids()) continue;

            Raycast.SteppedTrajectory trajectory = Raycast.arrowTrajectory(entityArrow, MAX_TICKS);
            if (trajectory.collisionPos == null) continue;

            arrows.add(trajectory);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.arrows.clear();
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
