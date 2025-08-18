package io.github.waqfs.module.render;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.hud.wmark.WatermarkDisplay;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class Watermark extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Watermark";
    protected static final String MODULE_TOOLTIP = "Displays a watermark.";
    protected static final String MODULE_ID = "render.watermark";
    
    private final ToggleWidget enableText = new ToggleWidget(Text.literal("Text"), Text.literal("Display text.")).withDefaultState(true);
    private final ToggleWidget simplistic = new ToggleWidget(Text.literal("Simplistic"), Text.literal("A very simple watermark.")).withDefaultState(false);
    
    private WatermarkDisplay display;

    public Watermark() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(
                enableText,
                simplistic
        );
        enableText.registerConfigKey("render.watermark.text");
        simplistic.registerConfigKey("render.watermark.simplistic");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        if (display == null) {
            display = new WatermarkDisplay();
            Cigarette.registerHudElement(display);
        }
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
        WatermarkDisplay.TEXT_ENABLED = enableText.getRawState();
        WatermarkDisplay.SIMPLE_DISPLAY = simplistic.getRawState();
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
