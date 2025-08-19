package io.github.waqfs.module.ui;

import org.jetbrains.annotations.NotNull;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.hud.modules.ModuleListDisplay;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;

public class ModuleList extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Module List";
    protected static final String MODULE_TOOLTIP = "Displays a list of modules.";
    protected static final String MODULE_ID = "render.module_list";

    private ModuleListDisplay display;

    public ModuleList() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren();
        this.widget.withDefaultState(true);
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        if (display == null) {
            display = new ModuleListDisplay();
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
