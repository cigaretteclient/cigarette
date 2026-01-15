package dev.cigarette.module.ui;

import dev.cigarette.gui.ColorScheme;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class Colors extends RenderModule<ToggleWidget, Boolean> {
    public static final Colors INSTANCE = new Colors("ui.colors", "Colors", "Customize GUI color scheme.");

    // Primary gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> primaryStart = ColorDropdownWidget.buildToggle("Primary Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientPrimaryStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> primaryEnd = ColorDropdownWidget.buildToggle("Primary End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientPrimaryEnd()).withDefaultState(true);

    // Secondary gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> secondaryStart = ColorDropdownWidget.buildToggle("Secondary Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSecondaryStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> secondaryEnd = ColorDropdownWidget.buildToggle("Secondary End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSecondaryEnd()).withDefaultState(true);

    // Background gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> bgStart = ColorDropdownWidget.buildToggle("Background Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientBgStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> bgEnd = ColorDropdownWidget.buildToggle("Background End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientBgEnd()).withDefaultState(true);

    // Accent gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> accentStart = ColorDropdownWidget.buildToggle("Accent Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientAccentStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> accentEnd = ColorDropdownWidget.buildToggle("Accent End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientAccentEnd()).withDefaultState(true);

    // Success gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> successStart = ColorDropdownWidget.buildToggle("Success Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSuccessStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> successEnd = ColorDropdownWidget.buildToggle("Success End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSuccessEnd()).withDefaultState(true);

    // Warning gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> warningStart = ColorDropdownWidget.buildToggle("Warning Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientWarningStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> warningEnd = ColorDropdownWidget.buildToggle("Warning End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientWarningEnd()).withDefaultState(true);

    // Error gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> errorStart = ColorDropdownWidget.buildToggle("Error Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientErrorStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> errorEnd = ColorDropdownWidget.buildToggle("Error End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientErrorEnd()).withDefaultState(true);

    // Info gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> infoStart = ColorDropdownWidget.buildToggle("Info Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientInfoStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> infoEnd = ColorDropdownWidget.buildToggle("Info End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientInfoEnd()).withDefaultState(true);

    // Dark gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> darkStart = ColorDropdownWidget.buildToggle("Dark Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientDarkStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> darkEnd = ColorDropdownWidget.buildToggle("Dark End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientDarkEnd()).withDefaultState(true);

    // Light gradient
    private final ColorDropdownWidget<ToggleWidget, Boolean> lightStart = ColorDropdownWidget.buildToggle("Light Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientLightStart()).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> lightEnd = ColorDropdownWidget.buildToggle("Light End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientLightEnd()).withDefaultState(true);

    // Wave settings
    private final SliderWidget waveWavelength = new SliderWidget("Wave Wavelength", "The wavelength of the gradient").withBounds(20f, ColorScheme.getWaveWavelength(), 100f).withAccuracy(1);

    private final SliderWidget waveSpeed = new SliderWidget("Wave Speed", "The speed of the color wave effect.").withBounds(0f, ColorScheme.getWaveSpeed(), 5f).withAccuracy(2);

    private final SliderWidget waveAmplitude = new SliderWidget("Wave Amplitude", "The amplitude of the color wave effect.").withBounds(0f, ColorScheme.getWaveAmplitude(), 1f).withAccuracy(2);

    private Colors(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.setChildren(primaryStart, primaryEnd, secondaryStart, secondaryEnd, bgStart, bgEnd,
                   accentStart, accentEnd, successStart, successEnd, warningStart, warningEnd,
                   errorStart, errorEnd, infoStart, infoEnd, darkStart, darkEnd, lightStart, lightEnd,
                   new TextWidget("Wave Settings").withUnderline(),
                   waveWavelength, waveSpeed, waveAmplitude);
        
        // Register config keys
        primaryStart.getColorSquare().registerConfigKey("colors.primary.start");
        primaryEnd.getColorSquare().registerConfigKey("colors.primary.end");
        secondaryStart.getColorSquare().registerConfigKey("colors.secondary.start");
        secondaryEnd.getColorSquare().registerConfigKey("colors.secondary.end");
        bgStart.getColorSquare().registerConfigKey("colors.bg.start");
        bgEnd.getColorSquare().registerConfigKey("colors.bg.end");
        accentStart.getColorSquare().registerConfigKey("colors.accent.start");
        accentEnd.getColorSquare().registerConfigKey("colors.accent.end");
        successStart.getColorSquare().registerConfigKey("colors.success.start");
        successEnd.getColorSquare().registerConfigKey("colors.success.end");
        warningStart.getColorSquare().registerConfigKey("colors.warning.start");
        warningEnd.getColorSquare().registerConfigKey("colors.warning.end");
        errorStart.getColorSquare().registerConfigKey("colors.error.start");
        errorEnd.getColorSquare().registerConfigKey("colors.error.end");
        infoStart.getColorSquare().registerConfigKey("colors.info.start");
        infoEnd.getColorSquare().registerConfigKey("colors.info.end");
        darkStart.getColorSquare().registerConfigKey("colors.dark.start");
        darkEnd.getColorSquare().registerConfigKey("colors.dark.end");
        lightStart.getColorSquare().registerConfigKey("colors.light.start");
        lightEnd.getColorSquare().registerConfigKey("colors.light.end");
        waveWavelength.registerConfigKey("colors.wave.wavelength");
        waveSpeed.registerConfigKey("colors.wave.speed");
        waveAmplitude.registerConfigKey("colors.wave.amplitude");
        
        // Set up callbacks to update ColorScheme when colors change
        setupCallbacks();
    }
    
    private void setupCallbacks() {
        primaryStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientPrimaryStart((Integer) state));
        primaryEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientPrimaryEnd((Integer) state));
        secondaryStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientSecondaryStart((Integer) state));
        secondaryEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientSecondaryEnd((Integer) state));
        bgStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientBgStart((Integer) state));
        bgEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientBgEnd((Integer) state));
        accentStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientAccentStart((Integer) state));
        accentEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientAccentEnd((Integer) state));
        successStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientSuccessStart((Integer) state));
        successEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientSuccessEnd((Integer) state));
        warningStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientWarningStart((Integer) state));
        warningEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientWarningEnd((Integer) state));
        errorStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientErrorStart((Integer) state));
        errorEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientErrorEnd((Integer) state));
        infoStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientInfoStart((Integer) state));
        infoEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientInfoEnd((Integer) state));
        darkStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientDarkStart((Integer) state));
        darkEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientDarkEnd((Integer) state));
        lightStart.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientLightStart((Integer) state));
        lightEnd.getColorSquare().setStateCallback((state) -> ColorScheme.setGradientLightEnd((Integer) state));
        
        // Wave settings callbacks
        waveWavelength.setStateCallback((value) -> ColorScheme.setWaveWavelength(value.floatValue()));
        waveSpeed.setStateCallback((value) -> ColorScheme.setWaveSpeed(value.floatValue()));
        waveAmplitude.setStateCallback((value) -> ColorScheme.setWaveAmplitude(value.floatValue()));
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