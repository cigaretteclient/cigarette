package dev.cigarette.module.ui;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.SliderWidget;
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

    // Gradient configuration
    private final ToggleWidget alternateLayout = new ToggleWidget("Alternate Layout", "Enables alternate layout.").withDefaultState(false);
    
    private GUI(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.widget.withDefaultState(true);
        
        this.setChildren(
                alternateLayout
        );
        
        // Register config keys
        alternateLayout.registerConfigKey(id + ".alternate.layout.enabled");
    }

    /**
     * {@return whether gradient mode is enabled}
     */
    public boolean isGradientEnabled() {
        return true;
    }

    public boolean isAlternateLayoutEnabled() {
        return alternateLayout.getRawState();
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
