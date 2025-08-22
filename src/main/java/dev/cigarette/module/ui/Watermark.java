package dev.cigarette.module.ui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.hud.wmark.WatermarkDisplay;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Color;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class Watermark extends RenderModule<ToggleWidget, Boolean> {
    public static final Watermark INSTANCE = Cigarette.CONFIG.constructModule(new Watermark("ui.watermark", "Watermark", "Displays a watermark."), "UI");
    
    private final ToggleWidget enableText = new ToggleWidget(Text.literal("Text"), Text.literal("Display text.")).withDefaultState(true);
    private final ToggleWidget simplistic = new ToggleWidget(Text.literal("Simplistic"), Text.literal("A very simple watermark.")).withDefaultState(false);

    private final ColorDropdownWidget simpleBgColor = ColorDropdownWidget.buildToggle(Text.of("Background Color"), Text.literal("The background color of the simplistic watermark.")).withDefaultColor(Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f)).withDefaultState(true);

    private WatermarkDisplay display;

    public Watermark(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(
                enableText,
                simplistic,
                simpleBgColor
        );
        enableText.registerConfigKey(id + ".text");
        simplistic.registerConfigKey(id + ".simplistic");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {}

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
        if (display == null) {
            display = new WatermarkDisplay();
            Cigarette.registerHudElement(display);
        }
        WatermarkDisplay.TEXT_ENABLED = enableText.getRawState();
        WatermarkDisplay.SIMPLE_DISPLAY = simplistic.getRawState();
        WatermarkDisplay.BG_COLOR = simpleBgColor.getToggleState() ? simpleBgColor.getStateARGB() : Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
