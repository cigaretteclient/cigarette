package dev.cigarette.module.ui;

import org.jetbrains.annotations.NotNull;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.modules.ModuleListDisplay;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public class ModuleList extends RenderModule<ToggleWidget, Boolean> {
    public static final ModuleList INSTANCE = new ModuleList("ui.module_list", "Module List", "Displays a list of modules.");
    
    private final ToggleWidget enableFlip = new ToggleWidget(Text.literal("Flip"), Text.literal("Flip to top left.")).withDefaultState(false);


    private ModuleListDisplay display;

    public ModuleList(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(
            enableFlip
        );
        this.widget.withDefaultState(true);
        this.enableFlip.registerConfigKey(id + ".flip");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
        if (display == null) {
            display = new ModuleListDisplay();
            Cigarette.registerHudElement(display);
        }
        display.alignment = enableFlip.getRawState() ? ModuleListDisplay.Alignment.TOPLEFT : ModuleListDisplay.Alignment.TOPRIGHT;
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
