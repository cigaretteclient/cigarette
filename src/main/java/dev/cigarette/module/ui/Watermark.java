package dev.cigarette.module.ui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.AnimationConfig;
import dev.cigarette.gui.hud.wmark.WatermarkDisplay;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Color;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class Watermark extends RenderModule<ToggleWidget, Boolean> {
    public static final Watermark INSTANCE = new Watermark("ui.watermark", "Watermark", "Displays a watermark.");
    
    private final ToggleWidget enableText = new ToggleWidget("Text", "Display text.").withDefaultState(true);
    private final ToggleWidget simplistic = new ToggleWidget("Simplistic", "A very simple watermark.").withDefaultState(false);

    private final ColorDropdownWidget simpleBgColor = ColorDropdownWidget.buildToggle("Background Color", "The background color of the simplistic watermark.").withDefaultColor(Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f)).withDefaultState(true);
    private final ToggleWidget gradientBg = new ToggleWidget("Gradient Background", "Use gradient background instead of solid color.").withDefaultState(false);
    private final ColorDropdownWidget gradientStartColor = ColorDropdownWidget.buildToggle("Gradient Start", "The starting color of the gradient background.").withDefaultColor(Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f)).withDefaultState(true);
    private final ColorDropdownWidget gradientEndColor = ColorDropdownWidget.buildToggle("Gradient End", "The ending color of the gradient background.").withDefaultColor(Color.colorTransparentize(CigaretteScreen.SECONDARY_COLOR, 0.4f)).withDefaultState(true);

    private WatermarkDisplay display;

    private Watermark(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(
                enableText,
                simplistic,
                simpleBgColor,
                gradientBg,
                gradientStartColor,
                gradientEndColor
        );
        enableText.registerConfigKey(id + ".text");
        simplistic.registerConfigKey(id + ".simplistic");
        gradientBg.registerConfigKey(id + ".gradientBg");
        gradientStartColor.registerConfigKey(id + ".gradientStart");
        gradientEndColor.registerConfigKey(id + ".gradientEnd");
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
        WatermarkDisplay.BG_COLOR = simpleBgColor.getToggleState() ? simpleBgColor.getStateARGB() : Color.colorTransparentize(AnimationConfig.getPrimaryColor(), 0.4f);
        WatermarkDisplay.GRADIENT_BG = gradientBg.getRawState();
        WatermarkDisplay.GRADIENT_START_COLOR = gradientStartColor.getToggleState() ? gradientStartColor.getStateARGB() : Color.colorTransparentize(AnimationConfig.getPrimaryColor(), 0.4f);
        WatermarkDisplay.GRADIENT_END_COLOR = gradientEndColor.getToggleState() ? gradientEndColor.getStateARGB() : Color.colorTransparentize(AnimationConfig.getSecondaryColor(), 0.4f);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
