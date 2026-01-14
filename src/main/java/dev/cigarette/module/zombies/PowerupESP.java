package dev.cigarette.module.zombies;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.GameDetector;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Renderer;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.OptionalDouble;

public class PowerupESP extends RenderModule<ToggleWidget, Boolean> {
    public static final PowerupESP INSTANCE = new PowerupESP("zombies.powerupesp", "ItemESP", "Places ESP boxes around dropped powerups and the lucky chest.");

//    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp", 1536, Renderer.BLOCK_ESP_PHASE, RenderLayers.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false));
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp",
        1536,
        Renderer.BLOCK_ESP_PHASE,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .target(RenderPhase.MAIN_TARGET)
            .lineWidth(RenderPhase.FULL_LINE_WIDTH)
            .texture(RenderPhase.NO_TEXTURE)
            .build(false)
    );
    private PowerupESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        HashSet<ZombiesAgent.Powerup> powerups = ZombiesAgent.getPowerups();
        if (powerups.size() == 0) return;

        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (ZombiesAgent.Powerup powerup : powerups) {
            Renderer.drawCube(buffer, matrix, 0x3F000000 + powerup.type.getColor(), powerup.position, 2f);
        }

        BuiltBuffer built = buffer.endNullable();
        if (built != null) RENDER_LAYER.draw(built);
        matrixStack.pop();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
