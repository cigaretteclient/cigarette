package io.github.waqfs.module.zombies;

import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Renderer;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.OptionalDouble;

public class PowerupESP extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "ItemESP";
    protected static final String MODULE_TOOLTIP = "Places ESP boxes around dropped powerups and the lucky chest.";
    protected static final String MODULE_ID = "zombies.powerupesp";
    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockesp", 1536, Renderer.BLOCK_ESP_PHASE, RenderLayer.MultiPhaseParameters.builder().lineWidth(new RenderPhase.LineWidth(OptionalDouble.of(1))).build(false));

    public PowerupESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
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
