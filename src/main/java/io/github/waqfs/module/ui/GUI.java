package io.github.waqfs.module.ui;

import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class GUI extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "GUI";
    protected static final String MODULE_TOOLTIP = "ClickGUI settings.";
    protected static final String MODULE_ID = "render.gui";

    public GUI() {
        super(ToggleWidget.ToggleWidgetDisabled::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(
        );
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
    }
}
