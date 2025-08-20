package dev.cigarette.module.ui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.bar.BarDisplay;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class TargetHUD extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "TargetHUD";
    protected static final String MODULE_TOOLTIP = "Displays the current target & kill logs.";
    protected static final String MODULE_ID = "render.targethud";

    private BarDisplay display;

    public TargetHUD() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.widget.withDefaultState(true);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        if (display == null) {
            display = new BarDisplay();
            Cigarette.registerHudElement(display);
        }
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
                                 @NotNull ClientPlayerEntity player) {
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
