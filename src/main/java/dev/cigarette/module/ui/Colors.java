package dev.cigarette.module.ui;

import dev.cigarette.gui.AnimationConfig;
import dev.cigarette.gui.ColorScheme;
import dev.cigarette.gui.TextGradientRenderer;
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

    // Guards to prevent re-entrant callbacks causing freezes when applying presets
    private boolean applyingPreset = false;
    private boolean updatingUIColors = false;

    // Primary gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> primaryStart = ColorDropdownWidget.buildToggle("Primary Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientPrimaryStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> primaryEnd = ColorDropdownWidget.buildToggle("Primary End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientPrimaryEnd()).withDefaultState(true);

    // Secondary gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> secondaryStart = ColorDropdownWidget.buildToggle("Secondary Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSecondaryStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> secondaryEnd = ColorDropdownWidget.buildToggle("Secondary End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSecondaryEnd()).withDefaultState(true);

    // Background gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> bgStart = ColorDropdownWidget.buildToggle("Background Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientBgStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> bgEnd = ColorDropdownWidget.buildToggle("Background End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientBgEnd()).withDefaultState(true);

    // Accent gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> accentStart = ColorDropdownWidget.buildToggle("Accent Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientAccentStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> accentEnd = ColorDropdownWidget.buildToggle("Accent End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientAccentEnd()).withDefaultState(true);

    // Success gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> successStart = ColorDropdownWidget.buildToggle("Success Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSuccessStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> successEnd = ColorDropdownWidget.buildToggle("Success End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientSuccessEnd()).withDefaultState(true);

    // Warning gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> warningStart = ColorDropdownWidget.buildToggle("Warning Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientWarningStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> warningEnd = ColorDropdownWidget.buildToggle("Warning End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientWarningEnd()).withDefaultState(true);

    // Error gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> errorStart = ColorDropdownWidget.buildToggle("Error Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientErrorStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> errorEnd = ColorDropdownWidget.buildToggle("Error End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientErrorEnd()).withDefaultState(true);

    // Info gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> infoStart = ColorDropdownWidget.buildToggle("Info Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientInfoStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> infoEnd = ColorDropdownWidget.buildToggle("Info End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientInfoEnd()).withDefaultState(true);

    // Dark gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> darkStart = ColorDropdownWidget.buildToggle("Dark Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientDarkStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> darkEnd = ColorDropdownWidget.buildToggle("Dark End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientDarkEnd()).withDefaultState(true);

    // Light gradient
    public final ColorDropdownWidget<ToggleWidget, Boolean> lightStart = ColorDropdownWidget.buildToggle("Light Start", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientLightStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> lightEnd = ColorDropdownWidget.buildToggle("Light End", null)
        .withAlpha(true).withDefaultColor(ColorScheme.getGradientLightEnd()).withDefaultState(true);

    // Wave settings
    public final SliderWidget waveWavelength = new SliderWidget("Wave Wavelength", "The wavelength of the gradient").withBounds(20f, ColorScheme.getWaveWavelength(), 100f).withAccuracy(1);

    public final SliderWidget waveSpeed = new SliderWidget("Wave Speed", "The speed of the color wave effect.").withBounds(0f, ColorScheme.getWaveSpeed(), 5f).withAccuracy(2);

    public final SliderWidget waveAmplitude = new SliderWidget("Wave Amplitude", "The amplitude of the color wave effect.").withBounds(0f, ColorScheme.getWaveAmplitude(), 1f).withAccuracy(2);

    // Text gradient settings
    public final ToggleWidget textGradientsEnabled = new ToggleWidget("Text Gradients", "Enable gradient text effects.").withDefaultState(TextGradientRenderer.isTextGradientsEnabled());
    public final ColorDropdownWidget<ToggleWidget, Boolean> textGradientStart = ColorDropdownWidget.buildToggle("Text Gradient Start", null)
        .withAlpha(true).withDefaultColor(TextGradientRenderer.getTextGradientStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> textGradientEnd = ColorDropdownWidget.buildToggle("Text Gradient End", null)
        .withAlpha(true).withDefaultColor(TextGradientRenderer.getTextGradientEnd()).withDefaultState(true);

    // Text wave settings
    public final ToggleWidget textWaveEnabled = new ToggleWidget("Text Wave", "Enable animated wave text effects.").withDefaultState(TextGradientRenderer.isTextWaveEnabled());
    public final ColorDropdownWidget<ToggleWidget, Boolean> textWaveStart = ColorDropdownWidget.buildToggle("Text Wave Start", "Orange by default")
        .withAlpha(true).withDefaultColor(TextGradientRenderer.getTextWaveStart()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> textWaveEnd = ColorDropdownWidget.buildToggle("Text Wave End", "Black by default")
        .withAlpha(true).withDefaultColor(TextGradientRenderer.getTextWaveEnd()).withDefaultState(true);

    // Color Presets
    public final ToggleWidget defaultPreset = new ToggleWidget("Default", "Apply default Cigarette color scheme");
    public final ToggleWidget nordPreset = new ToggleWidget("Nord", "Apply Nord color scheme");
    public final ToggleWidget gruvboxDarkPreset = new ToggleWidget("Gruvbox Dark", "Apply Gruvbox Dark color scheme");
    public final ToggleWidget gruvboxLightPreset = new ToggleWidget("Gruvbox Light", "Apply Gruvbox Light color scheme");
    public final ToggleWidget draculaPreset = new ToggleWidget("Dracula", "Apply Dracula color scheme");

    // GUI Color Scheme Settings
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiPrimaryColor = ColorDropdownWidget.buildToggle("GUI Primary", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getPrimaryColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiSecondaryColor = ColorDropdownWidget.buildToggle("GUI Secondary", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getSecondaryColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiPrimaryTextColor = ColorDropdownWidget.buildToggle("GUI Primary Text", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getPrimaryTextColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiSecondaryTextColor = ColorDropdownWidget.buildToggle("GUI Secondary Text", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getSecondaryTextColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiBackgroundColor = ColorDropdownWidget.buildToggle("GUI Background", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getBackgroundColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiDarkBackgroundColor = ColorDropdownWidget.buildToggle("GUI Dark Background", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getDarkBackgroundColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiHoverColor = ColorDropdownWidget.buildToggle("GUI Hover", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getHoverColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiSelectionColor = ColorDropdownWidget.buildToggle("GUI Selection", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getSelectionColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiCheckboxBg = ColorDropdownWidget.buildToggle("GUI Checkbox Background", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getCheckboxBackgroundColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiCheckboxBorder = ColorDropdownWidget.buildToggle("GUI Checkbox Border", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getCheckboxBorderColor()).withDefaultState(true);
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiCheckboxCheck = ColorDropdownWidget.buildToggle("GUI Checkbox Check", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getCheckboxCheckColor()).withDefaultState(true);

    // Border settings
    public final ToggleWidget guiBorderEnabled = (ToggleWidget) new ToggleWidget("GUI Border Enabled", "Enable the border around GUI containers.").withDefault(AnimationConfig.isBorderEnabled());
    public final ColorDropdownWidget<ToggleWidget, Boolean> guiBorderColor = ColorDropdownWidget.buildToggle("GUI Border Color", null)
        .withAlpha(true).withDefaultColor(AnimationConfig.getBorderColor()).withDefaultState(true);
    public final SliderWidget guiBorderWidth = new SliderWidget("GUI Border Width", "The width of the GUI border in pixels.").withBounds(1f, AnimationConfig.getBorderWidth(), 5f).withAccuracy(0);

    private Colors(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.setChildren(new TextWidget("Color Presets").withUnderline(),
                   defaultPreset, nordPreset, gruvboxDarkPreset, gruvboxLightPreset, draculaPreset,
                   primaryStart, primaryEnd, secondaryStart, secondaryEnd, bgStart, bgEnd,
                   accentStart, accentEnd, successStart, successEnd, warningStart, warningEnd,
                   errorStart, errorEnd, infoStart, infoEnd, darkStart, darkEnd, lightStart, lightEnd,
                   new TextWidget("Wave Settings").withUnderline(),
                   waveWavelength, waveSpeed, waveAmplitude,
                   new TextWidget("Text Gradients").withUnderline(),
                   textGradientsEnabled, textGradientStart, textGradientEnd,
                   new TextWidget("Text Wave").withUnderline(),
                   textWaveEnabled, textWaveStart, textWaveEnd,
                   new TextWidget("GUI Colors").withUnderline(),
                   guiPrimaryColor, guiSecondaryColor, guiPrimaryTextColor, guiSecondaryTextColor,
                   guiBackgroundColor, guiDarkBackgroundColor, guiHoverColor, guiSelectionColor,
                   guiCheckboxBg, guiCheckboxBorder, guiCheckboxCheck,
                   new TextWidget("GUI Border").withUnderline(),
                   guiBorderEnabled, guiBorderColor, guiBorderWidth);
        
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
        textGradientsEnabled.registerConfigKey("text.gradient.enabled");
        textGradientStart.getColorSquare().registerConfigKey("text.gradient.start");
        textGradientEnd.getColorSquare().registerConfigKey("text.gradient.end");
        textWaveEnabled.registerConfigKey("text.wave.enabled");
        textWaveStart.getColorSquare().registerConfigKey("text.wave.start");
        textWaveEnd.getColorSquare().registerConfigKey("text.wave.end");
        
        // GUI color settings config keys
        guiPrimaryColor.getColorSquare().registerConfigKey("gui.colors.primary");
        guiSecondaryColor.getColorSquare().registerConfigKey("gui.colors.secondary");
        guiPrimaryTextColor.getColorSquare().registerConfigKey("gui.colors.primary_text");
        guiSecondaryTextColor.getColorSquare().registerConfigKey("gui.colors.secondary_text");
        guiBackgroundColor.getColorSquare().registerConfigKey("gui.colors.background");
        guiDarkBackgroundColor.getColorSquare().registerConfigKey("gui.colors.dark_background");
        guiHoverColor.getColorSquare().registerConfigKey("gui.colors.hover");
        guiSelectionColor.getColorSquare().registerConfigKey("gui.colors.selection");
        guiCheckboxBg.getColorSquare().registerConfigKey("gui.colors.checkbox_background");
        guiCheckboxBorder.getColorSquare().registerConfigKey("gui.colors.checkbox_border");
        guiCheckboxCheck.getColorSquare().registerConfigKey("gui.colors.checkbox_check");
        
        // Border settings config keys
        guiBorderEnabled.registerConfigKey("gui.border.enabled");
        guiBorderColor.getColorSquare().registerConfigKey("gui.border.color");
        guiBorderWidth.registerConfigKey("gui.border.width");
        
        // Set up callbacks to update configurations when settings change
        setupCallbacks();
        
        // Set up color presets
        setupColorPresets();
    }
    
    private void setupColorPresets() {
        // Default preset (current colors)
        defaultPreset.setStateCallback((state) -> {
            if ((Boolean) state && !applyingPreset) {
                applyingPreset = true;
                try {
                    setActivePreset(defaultPreset);
                    applyDefaultPreset();
                } finally {
                    applyingPreset = false;
                }
            }
        });
        
        // Nord preset
        nordPreset.setStateCallback((state) -> {
            if ((Boolean) state && !applyingPreset) {
                applyingPreset = true;
                try {
                    setActivePreset(nordPreset);
                    applyNordPreset();
                } finally {
                    applyingPreset = false;
                }
            }
        });
        
        // Gruvbox Dark preset
        gruvboxDarkPreset.setStateCallback((state) -> {
            if ((Boolean) state && !applyingPreset) {
                applyingPreset = true;
                try {
                    setActivePreset(gruvboxDarkPreset);
                    applyGruvboxDarkPreset();
                } finally {
                    applyingPreset = false;
                }
            }
        });
        
        // Gruvbox Light preset
        gruvboxLightPreset.setStateCallback((state) -> {
            if ((Boolean) state && !applyingPreset) {
                applyingPreset = true;
                try {
                    setActivePreset(gruvboxLightPreset);
                    applyGruvboxLightPreset();
                } finally {
                    applyingPreset = false;
                }
            }
        });
        
        // Dracula preset
        draculaPreset.setStateCallback((state) -> {
            if ((Boolean) state && !applyingPreset) {
                applyingPreset = true;
                try {
                    setActivePreset(draculaPreset);
                    applyDraculaPreset();
                } finally {
                    applyingPreset = false;
                }
            }
        });
    }
    
    private void setupCallbacks() {
        primaryStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientPrimaryStart((Integer) state); });
        primaryEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientPrimaryEnd((Integer) state); });
        secondaryStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientSecondaryStart((Integer) state); });
        secondaryEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientSecondaryEnd((Integer) state); });
        bgStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientBgStart((Integer) state); });
        bgEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientBgEnd((Integer) state); });
        accentStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientAccentStart((Integer) state); });
        accentEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientAccentEnd((Integer) state); });
        successStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientSuccessStart((Integer) state); });
        successEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientSuccessEnd((Integer) state); });
        warningStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientWarningStart((Integer) state); });
        warningEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientWarningEnd((Integer) state); });
        errorStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientErrorStart((Integer) state); });
        errorEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientErrorEnd((Integer) state); });
        infoStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientInfoStart((Integer) state); });
        infoEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientInfoEnd((Integer) state); });
        darkStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientDarkStart((Integer) state); });
        darkEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientDarkEnd((Integer) state); });
        lightStart.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientLightStart((Integer) state); });
        lightEnd.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; ColorScheme.setGradientLightEnd((Integer) state); });
        
        // Wave settings callbacks
        waveWavelength.setStateCallback((value) -> ColorScheme.setWaveWavelength(value.floatValue()));
        waveSpeed.setStateCallback((value) -> ColorScheme.setWaveSpeed(value.floatValue()));
        waveAmplitude.setStateCallback((value) -> ColorScheme.setWaveAmplitude(value.floatValue()));
        
        // Text gradient settings callbacks
        textGradientsEnabled.setStateCallback((state) -> TextGradientRenderer.setTextGradientsEnabled((Boolean) state));
        textGradientStart.getColorSquare().setStateCallback((state) -> TextGradientRenderer.setTextGradientStart((Integer) state));
        textGradientEnd.getColorSquare().setStateCallback((state) -> TextGradientRenderer.setTextGradientEnd((Integer) state));
        
        // Text wave settings callbacks
        textWaveEnabled.setStateCallback((state) -> TextGradientRenderer.setTextWaveEnabled((Boolean) state));
        textWaveStart.getColorSquare().setStateCallback((state) -> TextGradientRenderer.setTextWaveStart((Integer) state));
        textWaveEnd.getColorSquare().setStateCallback((state) -> TextGradientRenderer.setTextWaveEnd((Integer) state));
        
        // GUI color settings callbacks
        guiPrimaryColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setPrimaryColor((Integer) state); });
        guiSecondaryColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setSecondaryColor((Integer) state); });
        guiPrimaryTextColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setPrimaryTextColor((Integer) state); });
        guiSecondaryTextColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setSecondaryTextColor((Integer) state); });
        guiBackgroundColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setBackgroundColor((Integer) state); });
        guiDarkBackgroundColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setDarkBackgroundColor((Integer) state); });
        guiHoverColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setHoverColor((Integer) state); });
        guiSelectionColor.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setSelectionColor((Integer) state); });
        guiCheckboxBg.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setCheckboxBackgroundColor((Integer) state); });
        guiCheckboxBorder.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setCheckboxBorderColor((Integer) state); });
        guiCheckboxCheck.getColorSquare().setStateCallback((state) -> { if (updatingUIColors) return; AnimationConfig.setCheckboxCheckColor((Integer) state); });
        
        // Border settings callbacks
        guiBorderEnabled.setStateCallback((state) -> AnimationConfig.setBorderEnabled((Boolean) state));
        guiBorderColor.getColorSquare().setStateCallback((state) -> AnimationConfig.setBorderColor((Integer) state));
        guiBorderWidth.setStateCallback((value) -> AnimationConfig.setBorderWidth(value.intValue()));
    }
    
    private void applyDefaultPreset() {
        // Default Cigarette colors
        ColorScheme.setGradientPrimaryStart(0xFFFE5F00);
        ColorScheme.setGradientPrimaryEnd(0xFFC44700);
        ColorScheme.setGradientSecondaryStart(0xFFC44700);
        ColorScheme.setGradientSecondaryEnd(0xFF7A2E00);
        ColorScheme.setGradientBgStart(0xFF1A1A1A);
        ColorScheme.setGradientBgEnd(0xFF000000);
        ColorScheme.setGradientAccentStart(0xFFFFB84D);
        ColorScheme.setGradientAccentEnd(0xFFFF8C00);
        ColorScheme.setGradientSuccessStart(0xFF52B86E);
        ColorScheme.setGradientSuccessEnd(0xFF2E8B4A);
        ColorScheme.setGradientWarningStart(0xFFFFA500);
        ColorScheme.setGradientWarningEnd(0xFFFF6B35);
        ColorScheme.setGradientErrorStart(0xFFFF6B6B);
        ColorScheme.setGradientErrorEnd(0xFFCC0000);
        ColorScheme.setGradientInfoStart(0xFFFE5F00);
        ColorScheme.setGradientInfoEnd(0xFFC44700);
        ColorScheme.setGradientDarkStart(0xFF141414);
        ColorScheme.setGradientDarkEnd(0xFF0A0A0A);
        ColorScheme.setGradientLightStart(0xFFFFD700);
        ColorScheme.setGradientLightEnd(0xFFFFA500);
        
        // GUI colors
        AnimationConfig.setPrimaryColor(0xFFFE5F00);
        AnimationConfig.setSecondaryColor(0xFFC44700);
        AnimationConfig.setPrimaryTextColor(0xFFFFFFFF);
        AnimationConfig.setSecondaryTextColor(0xFFAAAAAA);
        AnimationConfig.setBackgroundColor(0xFF1A1A1A);
        AnimationConfig.setDarkBackgroundColor(0xFF0A0A0A);
        AnimationConfig.setHoverColor(0xFF333333);
        AnimationConfig.setSelectionColor(0xFFFE5F00);
        AnimationConfig.setCheckboxBackgroundColor(0xFF1A1A1A);
        AnimationConfig.setCheckboxBorderColor(0xFFFE5F00);
        AnimationConfig.setCheckboxCheckColor(0xFFFE5F00);
        // Borders
        AnimationConfig.setBorderColor(0xFFFE5F00);
        
        // Update UI widgets
        updateUIFromColorScheme();
    }
    
    private void applyNordPreset() {
        // Nord color scheme
        ColorScheme.setGradientPrimaryStart(0xFF88C0D0);
        ColorScheme.setGradientPrimaryEnd(0xFF81A1C1);
        ColorScheme.setGradientSecondaryStart(0xFF5E81AC);
        ColorScheme.setGradientSecondaryEnd(0xFF434C5E);
        ColorScheme.setGradientBgStart(0xFF2E3440);
        ColorScheme.setGradientBgEnd(0xFF3B4252);
        ColorScheme.setGradientAccentStart(0xFF88C0D0);
        ColorScheme.setGradientAccentEnd(0xFF81A1C1);
        ColorScheme.setGradientSuccessStart(0xFFA3BE8C);
        ColorScheme.setGradientSuccessEnd(0xFF8FBCBB);
        ColorScheme.setGradientWarningStart(0xFFEBCB8B);
        ColorScheme.setGradientWarningEnd(0xFFD08770);
        ColorScheme.setGradientErrorStart(0xFFBF616A);
        ColorScheme.setGradientErrorEnd(0xFF8FBCBB);
        ColorScheme.setGradientInfoStart(0xFF88C0D0);
        ColorScheme.setGradientInfoEnd(0xFF81A1C1);
        ColorScheme.setGradientDarkStart(0xFF2E3440);
        ColorScheme.setGradientDarkEnd(0xFF3B4252);
        ColorScheme.setGradientLightStart(0xFFECEFF4);
        ColorScheme.setGradientLightEnd(0xFFD8DEE9);
        
        // GUI colors
        AnimationConfig.setPrimaryColor(0xFF88C0D0);
        AnimationConfig.setSecondaryColor(0xFF5E81AC);
        AnimationConfig.setPrimaryTextColor(0xFFECEFF4);
        AnimationConfig.setSecondaryTextColor(0xFFD8DEE9);
        AnimationConfig.setBackgroundColor(0xFF2E3440);
        AnimationConfig.setDarkBackgroundColor(0xFF3B4252);
        AnimationConfig.setHoverColor(0xFF434C5E);
        AnimationConfig.setSelectionColor(0xFF88C0D0);
        AnimationConfig.setCheckboxBackgroundColor(0xFF2E3440);
        AnimationConfig.setCheckboxBorderColor(0xFF88C0D0);
        AnimationConfig.setCheckboxCheckColor(0xFF88C0D0);
        // Borders
        AnimationConfig.setBorderColor(0xFF88C0D0);
        
        updateUIFromColorScheme();
    }
    
    private void applyGruvboxDarkPreset() {
        // Gruvbox Dark color scheme
        ColorScheme.setGradientPrimaryStart(0xFFFB4934);
        ColorScheme.setGradientPrimaryEnd(0xFFCC241D);
        ColorScheme.setGradientSecondaryStart(0xFFB8BB26);
        ColorScheme.setGradientSecondaryEnd(0xFF98971A);
        ColorScheme.setGradientBgStart(0xFF282828);
        ColorScheme.setGradientBgEnd(0xFF1D2021);
        ColorScheme.setGradientAccentStart(0xFFFABD2F);
        ColorScheme.setGradientAccentEnd(0xFFD79921);
        ColorScheme.setGradientSuccessStart(0xFFB8BB26);
        ColorScheme.setGradientSuccessEnd(0xFF98971A);
        ColorScheme.setGradientWarningStart(0xFFFABD2F);
        ColorScheme.setGradientWarningEnd(0xFFD79921);
        ColorScheme.setGradientErrorStart(0xFFFB4934);
        ColorScheme.setGradientErrorEnd(0xFFCC241D);
        ColorScheme.setGradientInfoStart(0xFF83A598);
        ColorScheme.setGradientInfoEnd(0xFF689D6A);
        ColorScheme.setGradientDarkStart(0xFF282828);
        ColorScheme.setGradientDarkEnd(0xFF1D2021);
        ColorScheme.setGradientLightStart(0xFFEBDBB2);
        ColorScheme.setGradientLightEnd(0xFFD5C4A1);
        
        // GUI colors
        AnimationConfig.setPrimaryColor(0xFFFB4934);
        AnimationConfig.setSecondaryColor(0xFFB8BB26);
        AnimationConfig.setPrimaryTextColor(0xFFEBDBB2);
        AnimationConfig.setSecondaryTextColor(0xFFD5C4A1);
        AnimationConfig.setBackgroundColor(0xFF282828);
        AnimationConfig.setDarkBackgroundColor(0xFF1D2021);
        AnimationConfig.setHoverColor(0xFF3C3836);
        AnimationConfig.setSelectionColor(0xFFFB4934);
        AnimationConfig.setCheckboxBackgroundColor(0xFF282828);
        AnimationConfig.setCheckboxBorderColor(0xFFFB4934);
        AnimationConfig.setCheckboxCheckColor(0xFFFB4934);
        // Borders
        AnimationConfig.setBorderColor(0xFFFB4934);
        
        updateUIFromColorScheme();
    }
    
    private void applyGruvboxLightPreset() {
        // Gruvbox Light color scheme
        ColorScheme.setGradientPrimaryStart(0xFFCC241D);
        ColorScheme.setGradientPrimaryEnd(0xFF9D0006);
        ColorScheme.setGradientSecondaryStart(0xFF98971A);
        ColorScheme.setGradientSecondaryEnd(0xFF79740E);
        ColorScheme.setGradientBgStart(0xFFF9F5D7);
        ColorScheme.setGradientBgEnd(0xFFEBDBB2);
        ColorScheme.setGradientAccentStart(0xFFD79921);
        ColorScheme.setGradientAccentEnd(0xFFB57614);
        ColorScheme.setGradientSuccessStart(0xFF98971A);
        ColorScheme.setGradientSuccessEnd(0xFF79740E);
        ColorScheme.setGradientWarningStart(0xFFD79921);
        ColorScheme.setGradientWarningEnd(0xFFB57614);
        ColorScheme.setGradientErrorStart(0xFFCC241D);
        ColorScheme.setGradientErrorEnd(0xFF9D0006);
        ColorScheme.setGradientInfoStart(0xFF689D6A);
        ColorScheme.setGradientInfoEnd(0xFF427B58);
        ColorScheme.setGradientDarkStart(0xFFEBDBB2);
        ColorScheme.setGradientDarkEnd(0xFFD5C4A1);
        ColorScheme.setGradientLightStart(0xFFF9F5D7);
        ColorScheme.setGradientLightEnd(0xFFEBDBB2);
        
        // GUI colors
        AnimationConfig.setPrimaryColor(0xFFCC241D);
        AnimationConfig.setSecondaryColor(0xFF98971A);
        AnimationConfig.setPrimaryTextColor(0xFF3C3836);
        AnimationConfig.setSecondaryTextColor(0xFF504945);
        AnimationConfig.setBackgroundColor(0xFFF9F5D7);
        AnimationConfig.setDarkBackgroundColor(0xFFEBDBB2);
        AnimationConfig.setHoverColor(0xFFD5C4A1);
        AnimationConfig.setSelectionColor(0xFFCC241D);
        AnimationConfig.setCheckboxBackgroundColor(0xFFF9F5D7);
        AnimationConfig.setCheckboxBorderColor(0xFFCC241D);
        AnimationConfig.setCheckboxCheckColor(0xFFCC241D);
        // Borders
        AnimationConfig.setBorderColor(0xFFCC241D);
        
        updateUIFromColorScheme();
    }
    
    private void applyDraculaPreset() {
        // Dracula color scheme
        ColorScheme.setGradientPrimaryStart(0xFFBD93F9);
        ColorScheme.setGradientPrimaryEnd(0xFF6272A4);
        ColorScheme.setGradientSecondaryStart(0xFF50FA7B);
        ColorScheme.setGradientSecondaryEnd(0xFF6272A4);
        ColorScheme.setGradientBgStart(0xFF282A36);
        ColorScheme.setGradientBgEnd(0xFF21222C);
        ColorScheme.setGradientAccentStart(0xFFFFB86C);
        ColorScheme.setGradientAccentEnd(0xFFFF79C6);
        ColorScheme.setGradientSuccessStart(0xFF50FA7B);
        ColorScheme.setGradientSuccessEnd(0xFF6272A4);
        ColorScheme.setGradientWarningStart(0xFFF1FA8C);
        ColorScheme.setGradientWarningEnd(0xFFFFB86C);
        ColorScheme.setGradientErrorStart(0xFFFF5555);
        ColorScheme.setGradientErrorEnd(0xFFFF79C6);
        ColorScheme.setGradientInfoStart(0xFFBD93F9);
        ColorScheme.setGradientInfoEnd(0xFF6272A4);
        ColorScheme.setGradientDarkStart(0xFF282A36);
        ColorScheme.setGradientDarkEnd(0xFF21222C);
        ColorScheme.setGradientLightStart(0xFFF8F8F2);
        ColorScheme.setGradientLightEnd(0xFFF1FA8C);
        
        // GUI colors
        AnimationConfig.setPrimaryColor(0xFFBD93F9);
        AnimationConfig.setSecondaryColor(0xFF50FA7B);
        AnimationConfig.setPrimaryTextColor(0xFFF8F8F2);
        AnimationConfig.setSecondaryTextColor(0xFFF1FA8C);
        AnimationConfig.setBackgroundColor(0xFF282A36);
        AnimationConfig.setDarkBackgroundColor(0xFF21222C);
        AnimationConfig.setHoverColor(0xFF44475A);
        AnimationConfig.setSelectionColor(0xFFBD93F9);
        AnimationConfig.setCheckboxBackgroundColor(0xFF282A36);
        AnimationConfig.setCheckboxBorderColor(0xFFBD93F9);
        AnimationConfig.setCheckboxCheckColor(0xFFBD93F9);
        AnimationConfig.setBorderColor(0xFFBD93F9);
        
        updateUIFromColorScheme();
    }
    
    private void setActivePreset(ToggleWidget activePreset) {
        // Set all presets to false except the active one
        defaultPreset.setRawState(defaultPreset == activePreset);
        nordPreset.setRawState(nordPreset == activePreset);
        gruvboxDarkPreset.setRawState(gruvboxDarkPreset == activePreset);
        gruvboxLightPreset.setRawState(gruvboxLightPreset == activePreset);
        draculaPreset.setRawState(draculaPreset == activePreset);
    }
    
    private void updateUIFromColorScheme() {
        // Update all the UI widgets to reflect the new color scheme
        updatingUIColors = true;
        primaryStart.getColorSquare().setRawState(ColorScheme.getGradientPrimaryStart());
        primaryEnd.getColorSquare().setRawState(ColorScheme.getGradientPrimaryEnd());
        secondaryStart.getColorSquare().setRawState(ColorScheme.getGradientSecondaryStart());
        secondaryEnd.getColorSquare().setRawState(ColorScheme.getGradientSecondaryEnd());
        bgStart.getColorSquare().setRawState(ColorScheme.getGradientBgStart());
        bgEnd.getColorSquare().setRawState(ColorScheme.getGradientBgEnd());
        accentStart.getColorSquare().setRawState(ColorScheme.getGradientAccentStart());
        accentEnd.getColorSquare().setRawState(ColorScheme.getGradientAccentEnd());
        successStart.getColorSquare().setRawState(ColorScheme.getGradientSuccessStart());
        successEnd.getColorSquare().setRawState(ColorScheme.getGradientSuccessEnd());
        warningStart.getColorSquare().setRawState(ColorScheme.getGradientWarningStart());
        warningEnd.getColorSquare().setRawState(ColorScheme.getGradientWarningEnd());
        errorStart.getColorSquare().setRawState(ColorScheme.getGradientErrorStart());
        errorEnd.getColorSquare().setRawState(ColorScheme.getGradientErrorEnd());
        infoStart.getColorSquare().setRawState(ColorScheme.getGradientInfoStart());
        infoEnd.getColorSquare().setRawState(ColorScheme.getGradientInfoEnd());
        darkStart.getColorSquare().setRawState(ColorScheme.getGradientDarkStart());
        darkEnd.getColorSquare().setRawState(ColorScheme.getGradientDarkEnd());
        lightStart.getColorSquare().setRawState(ColorScheme.getGradientLightStart());
        lightEnd.getColorSquare().setRawState(ColorScheme.getGradientLightEnd());
        
        // Update GUI color widgets
        guiPrimaryColor.getColorSquare().setRawState(AnimationConfig.getPrimaryColor());
        guiSecondaryColor.getColorSquare().setRawState(AnimationConfig.getSecondaryColor());
        guiPrimaryTextColor.getColorSquare().setRawState(AnimationConfig.getPrimaryTextColor());
        guiSecondaryTextColor.getColorSquare().setRawState(AnimationConfig.getSecondaryTextColor());
        guiBackgroundColor.getColorSquare().setRawState(AnimationConfig.getBackgroundColor());
        guiDarkBackgroundColor.getColorSquare().setRawState(AnimationConfig.getDarkBackgroundColor());
        guiHoverColor.getColorSquare().setRawState(AnimationConfig.getHoverColor());
        guiSelectionColor.getColorSquare().setRawState(AnimationConfig.getSelectionColor());
        guiCheckboxBg.getColorSquare().setRawState(AnimationConfig.getCheckboxBackgroundColor());
        guiCheckboxBorder.getColorSquare().setRawState(AnimationConfig.getCheckboxBorderColor());
        guiCheckboxCheck.getColorSquare().setRawState(AnimationConfig.getCheckboxCheckColor());
        updatingUIColors = false;
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