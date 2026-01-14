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
    private final ToggleWidget enableGradient = new ToggleWidget("Enable Gradient", "Enables UI-wide gradient coloring.").withDefaultState(false);
    private final ToggleWidget enableRainbow = new ToggleWidget("Rainbow Mode", "Enables rainbow cycling gradient.").withDefaultState(false);
    
    // Gradient colors
    private final ColorDropdownWidget<ToggleWidget, Boolean> gradientColor1 = ColorDropdownWidget.buildToggle("Color 1", "First gradient color.").withDefaultColor(0xFFFE5F00).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> gradientColor2 = ColorDropdownWidget.buildToggle("Color 2", "Second gradient color.").withDefaultColor(0xFFC44700).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> gradientColor3 = ColorDropdownWidget.buildToggle("Color 3", "Third gradient color (optional).").withDefaultColor(0xFF3AC4FC).withDefaultState(false);
    
    // Gradient controls
    private final SliderWidget gradientSaturation = new SliderWidget("Global Saturation", "Adjusts saturation for all gradient colors.").withBounds(0, 100, 100);
    private final SliderWidget gradientLightness = new SliderWidget("Global Lightness", "Adjusts lightness for all gradient colors.").withBounds(0, 50, 100);
    private final SliderWidget gradientSpeed = new SliderWidget("Rainbow Speed", "Controls the speed of rainbow cycling.").withBounds(0.1, 1.0, 5.0);
    private final SliderWidget gradientPosition = new SliderWidget("Gradient Position", "Controls the position of the gradient (0-100).").withBounds(0, 50, 100);

    private GUI(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.widget.withDefaultState(true);
        
        this.setChildren(
                enableGradient,
                enableRainbow,
                gradientColor1,
                gradientColor2,
                gradientColor3,
                gradientSaturation,
                gradientLightness,
                gradientSpeed,
                gradientPosition
        );
        
        // Register config keys
        enableGradient.registerConfigKey(id + ".gradient.enabled");
        enableRainbow.registerConfigKey(id + ".gradient.rainbow");
        gradientColor1.registerConfigKey(id + ".gradient.color1");
        gradientColor2.registerConfigKey(id + ".gradient.color2");
        gradientColor3.registerConfigKey(id + ".gradient.color3");
        gradientSaturation.registerConfigKey(id + ".gradient.saturation");
        gradientLightness.registerConfigKey(id + ".gradient.lightness");
        gradientSpeed.registerConfigKey(id + ".gradient.speed");
        gradientPosition.registerConfigKey(id + ".gradient.position");
    }

    /**
     * {@return whether gradient mode is enabled}
     */
    public boolean isGradientEnabled() {
        return enableGradient.getRawState();
    }

    /**
     * {@return whether rainbow mode is enabled}
     */
    public boolean isRainbowEnabled() {
        return enableRainbow.getRawState();
    }

    /**
     * Gets a color from the gradient based on the provided position.
     *
     * @param position The position along the gradient (0-1)
     * @return The interpolated color in ARGB format
     */
    public int getGradientColor(double position) {
        if (!isGradientEnabled()) {
            return CigaretteScreen.PRIMARY_COLOR;
        }

        if (isRainbowEnabled()) {
            return getRainbowColor(position);
        }

        return getMultiColorGradient(position);
    }

    /**
     * Gets a rainbow color based on position and time.
     *
     * @param position The position along the gradient (0-1)
     * @return The rainbow color in ARGB format
     */
    private int getRainbowColor(double position) {
        long currentTimeMs = System.currentTimeMillis();
        double speed = gradientSpeed.getRawState();
        double hue = (currentTimeMs / (1000.0 / speed)) % 360;
        
        double saturation = gradientSaturation.getRawState();
        double lightness = gradientLightness.getRawState();
        
        // Add position offset to hue for variety
        hue = (hue + position * 120) % 360;
        
        return dev.cigarette.gui.widget.ColorWheelWidget.hslToRgb(hue, saturation, lightness);
    }

    /**
     * Gets a multi-color gradient color.
     *
     * @param position The position along the gradient (0-1)
     * @return The interpolated color in ARGB format
     */
    private int getMultiColorGradient(double position) {
        position = Math.max(0, Math.min(1, position));
        
        boolean color3Enabled = gradientColor3.getToggleState();
        
        int color1 = gradientColor1.getStateARGB();
        int color2 = gradientColor2.getStateARGB();
        int color3 = color3Enabled ? gradientColor3.getStateARGB() : color2;
        
        // Apply global saturation and lightness
        double sat = gradientSaturation.getRawState();
        double light = gradientLightness.getRawState();
        
        if (color3Enabled && position > 0.5) {
            // Gradient from color2 to color3
            position = (position - 0.5) * 2;
            return lerpColor(color2, color3, position, sat, light);
        } else {
            // Gradient from color1 to color2
            position = color3Enabled ? position * 2 : position;
            return lerpColor(color1, color2, position, sat, light);
        }
    }

    /**
     * Linearly interpolates between two colors with optional saturation/lightness adjustment.
     *
     * @param color1 The first color (ARGB)
     * @param color2 The second color (ARGB)
     * @param position The interpolation position (0-1)
     * @param saturation The saturation adjustment (0-100)
     * @param lightness The lightness adjustment (0-100)
     * @return The interpolated color
     */
    private int lerpColor(int color1, int color2, double position, double saturation, double lightness) {
        position = Math.max(0, Math.min(1, position));
        
        // Extract components
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        // Interpolate RGB components
        int a = (int) (a1 + (a2 - a1) * position);
        int r = (int) (r1 + (r2 - r1) * position);
        int g = (int) (g1 + (g2 - g1) * position);
        int b = (int) (b1 + (b2 - b1) * position);
        
        int result = (a << 24) | (r << 16) | (g << 8) | b;
        
        // Apply global saturation and lightness if provided
        if (saturation != 100 || lightness != 50) {
            double hue = dev.cigarette.gui.widget.ColorWheelWidget.rgbToHue(result);
            result = dev.cigarette.gui.widget.ColorWheelWidget.hslToRgb(hue, saturation, lightness);
            // Restore alpha
            result = (a << 24) | (result & 0xFFFFFF);
        }
        
        return result;
    }

    /**
     * Gets the current gradient position (0-1) for positioning the gradient effect.
     *
     * @return The gradient position
     */
    public double getGradientPosition() {
        return gradientPosition.getRawState() / 100.0;
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
