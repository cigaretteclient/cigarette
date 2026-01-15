package dev.cigarette.module.ui;

import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class GUI extends RenderModule<ToggleWidget, Boolean> {
    public static final GUI INSTANCE = new GUI("ui.gui", "GUI", "ClickGUI settings.");

    /**
     * Whether to use the alternate GUI layout (sidebar + right panel).
     * False = classic ClickGUI, True = alternate layout.
     */
    private final ToggleWidget alternateLayoutToggle = new ToggleWidget("Alternate Layout", "Use sidebar + right panel layout instead of classic ClickGUI.")
        .withDefaultState(false);

    private GUI(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.setChildren(alternateLayoutToggle);
        alternateLayoutToggle.registerConfigKey("gui.alternate.layout");
    }

    /**
     * Returns whether the alternate GUI layout is enabled.
     */
    public boolean isAlternateLayoutEnabled() {
        return alternateLayoutToggle.getRawState();
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
