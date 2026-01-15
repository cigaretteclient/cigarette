package dev.cigarette.gui;

import dev.cigarette.config.FileSystem;

/**
 * Global color scheme with gradient definitions for consistent GUI theming.
 * Now user-configurable via config and GUI.
 */
public final class ColorScheme {
    private ColorScheme() {}
    
    // Primary gradient
    private static int GRADIENT_PRIMARY_START = 0xFFFE5F00;
    private static int GRADIENT_PRIMARY_END = 0xFFC44700;

    // Secondary gradient
    private static int GRADIENT_SECONDARY_START = 0xFFC44700;
    private static int GRADIENT_SECONDARY_END = 0xFF7A2E00;

    // Background gradient
    private static int GRADIENT_BG_START = 0xFF1A1A1A;
    private static int GRADIENT_BG_END = 0xFF000000;

    // Accent gradient
    private static int GRADIENT_ACCENT_START = 0xFFFFB84D;
    private static int GRADIENT_ACCENT_END = 0xFFFF8C00;

    // Success gradient
    private static int GRADIENT_SUCCESS_START = 0xFF52B86E;
    private static int GRADIENT_SUCCESS_END = 0xFF2E8B4A;

    // Warning gradient
    private static int GRADIENT_WARNING_START = 0xFFFFA500;
    private static int GRADIENT_WARNING_END = 0xFFFF6B35;

    // Error gradient
    private static int GRADIENT_ERROR_START = 0xFFFF6B6B;
    private static int GRADIENT_ERROR_END = 0xFFCC0000;

    // Info gradient
    private static int GRADIENT_INFO_START = 0xFFFE5F00;
    private static int GRADIENT_INFO_END = 0xFFC44700;

    // Dark background gradient
    private static int GRADIENT_DARK_START = 0xFF141414;
    private static int GRADIENT_DARK_END = 0xFF0A0A0A;

    // Light accent gradient
    private static int GRADIENT_LIGHT_START = 0xFFFFD700;
    private static int GRADIENT_LIGHT_END = 0xFFFFA500;

    // Wave animation tuning
    private static float WAVE_WAVELENGTH = 120f;
    private static float WAVE_SPEED = 0.65f;
    private static float WAVE_AMPLITUDE = 0.12f;

    static {
        // Register config keys for all gradients and wave settings
        registerConfig();
    }

    private static void registerConfig() {
        FileSystem.registerUpdate("colors.primary.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_PRIMARY_START = i;
        });
        FileSystem.registerUpdate("colors.primary.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_PRIMARY_END = i;
        });
        FileSystem.registerUpdate("colors.secondary.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_SECONDARY_START = i;
        });
        FileSystem.registerUpdate("colors.secondary.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_SECONDARY_END = i;
        });
        FileSystem.registerUpdate("colors.bg.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_BG_START = i;
        });
        FileSystem.registerUpdate("colors.bg.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_BG_END = i;
        });
        FileSystem.registerUpdate("colors.accent.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_ACCENT_START = i;
        });
        FileSystem.registerUpdate("colors.accent.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_ACCENT_END = i;
        });
        FileSystem.registerUpdate("colors.success.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_SUCCESS_START = i;
        });
        FileSystem.registerUpdate("colors.success.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_SUCCESS_END = i;
        });
        FileSystem.registerUpdate("colors.warning.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_WARNING_START = i;
        });
        FileSystem.registerUpdate("colors.warning.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_WARNING_END = i;
        });
        FileSystem.registerUpdate("colors.error.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_ERROR_START = i;
        });
        FileSystem.registerUpdate("colors.error.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_ERROR_END = i;
        });
        FileSystem.registerUpdate("colors.info.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_INFO_START = i;
        });
        FileSystem.registerUpdate("colors.info.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_INFO_END = i;
        });
        FileSystem.registerUpdate("colors.dark.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_DARK_START = i;
        });
        FileSystem.registerUpdate("colors.dark.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_DARK_END = i;
        });
        FileSystem.registerUpdate("colors.light.start", (state) -> {
            if (state instanceof Integer i) GRADIENT_LIGHT_START = i;
        });
        FileSystem.registerUpdate("colors.light.end", (state) -> {
            if (state instanceof Integer i) GRADIENT_LIGHT_END = i;
        });
        FileSystem.registerUpdate("colors.wave.wavelength", (state) -> {
            if (state instanceof Float f) WAVE_WAVELENGTH = f;
        });
        FileSystem.registerUpdate("colors.wave.speed", (state) -> {
            if (state instanceof Float f) WAVE_SPEED = f;
        });
        FileSystem.registerUpdate("colors.wave.amplitude", (state) -> {
            if (state instanceof Float f) WAVE_AMPLITUDE = f;
        });
    }

    // Getters for gradients
    public static int getGradientPrimaryStart() { return GRADIENT_PRIMARY_START; }
    public static int getGradientPrimaryEnd() { return GRADIENT_PRIMARY_END; }
    public static int getGradientSecondaryStart() { return GRADIENT_SECONDARY_START; }
    public static int getGradientSecondaryEnd() { return GRADIENT_SECONDARY_END; }
    public static int getGradientBgStart() { return GRADIENT_BG_START; }
    public static int getGradientBgEnd() { return GRADIENT_BG_END; }
    public static int getGradientAccentStart() { return GRADIENT_ACCENT_START; }
    public static int getGradientAccentEnd() { return GRADIENT_ACCENT_END; }
    public static int getGradientSuccessStart() { return GRADIENT_SUCCESS_START; }
    public static int getGradientSuccessEnd() { return GRADIENT_SUCCESS_END; }
    public static int getGradientWarningStart() { return GRADIENT_WARNING_START; }
    public static int getGradientWarningEnd() { return GRADIENT_WARNING_END; }
    public static int getGradientErrorStart() { return GRADIENT_ERROR_START; }
    public static int getGradientErrorEnd() { return GRADIENT_ERROR_END; }
    public static int getGradientInfoStart() { return GRADIENT_INFO_START; }
    public static int getGradientInfoEnd() { return GRADIENT_INFO_END; }
    public static int getGradientDarkStart() { return GRADIENT_DARK_START; }
    public static int getGradientDarkEnd() { return GRADIENT_DARK_END; }
    public static int getGradientLightStart() { return GRADIENT_LIGHT_START; }
    public static int getGradientLightEnd() { return GRADIENT_LIGHT_END; }
    public static float getWaveWavelength() { return WAVE_WAVELENGTH; }
    public static float getWaveSpeed() { return WAVE_SPEED; }
    public static float getWaveAmplitude() { return WAVE_AMPLITUDE; }

    // Setters for gradients (will save to config)
    public static void setGradientPrimaryStart(int value) { GRADIENT_PRIMARY_START = value; FileSystem.updateState("colors.primary.start", value); }
    public static void setGradientPrimaryEnd(int value) { GRADIENT_PRIMARY_END = value; FileSystem.updateState("colors.primary.end", value); }
    public static void setGradientSecondaryStart(int value) { GRADIENT_SECONDARY_START = value; FileSystem.updateState("colors.secondary.start", value); }
    public static void setGradientSecondaryEnd(int value) { GRADIENT_SECONDARY_END = value; FileSystem.updateState("colors.secondary.end", value); }
    public static void setGradientBgStart(int value) { GRADIENT_BG_START = value; FileSystem.updateState("colors.bg.start", value); }
    public static void setGradientBgEnd(int value) { GRADIENT_BG_END = value; FileSystem.updateState("colors.bg.end", value); }
    public static void setGradientAccentStart(int value) { GRADIENT_ACCENT_START = value; FileSystem.updateState("colors.accent.start", value); }
    public static void setGradientAccentEnd(int value) { GRADIENT_ACCENT_END = value; FileSystem.updateState("colors.accent.end", value); }
    public static void setGradientSuccessStart(int value) { GRADIENT_SUCCESS_START = value; FileSystem.updateState("colors.success.start", value); }
    public static void setGradientSuccessEnd(int value) { GRADIENT_SUCCESS_END = value; FileSystem.updateState("colors.success.end", value); }
    public static void setGradientWarningStart(int value) { GRADIENT_WARNING_START = value; FileSystem.updateState("colors.warning.start", value); }
    public static void setGradientWarningEnd(int value) { GRADIENT_WARNING_END = value; FileSystem.updateState("colors.warning.end", value); }
    public static void setGradientErrorStart(int value) { GRADIENT_ERROR_START = value; FileSystem.updateState("colors.error.start", value); }
    public static void setGradientErrorEnd(int value) { GRADIENT_ERROR_END = value; FileSystem.updateState("colors.error.end", value); }
    public static void setGradientInfoStart(int value) { GRADIENT_INFO_START = value; FileSystem.updateState("colors.info.start", value); }
    public static void setGradientInfoEnd(int value) { GRADIENT_INFO_END = value; FileSystem.updateState("colors.info.end", value); }
    public static void setGradientDarkStart(int value) { GRADIENT_DARK_START = value; FileSystem.updateState("colors.dark.start", value); }
    public static void setGradientDarkEnd(int value) { GRADIENT_DARK_END = value; FileSystem.updateState("colors.dark.end", value); }
    public static void setGradientLightStart(int value) { GRADIENT_LIGHT_START = value; FileSystem.updateState("colors.light.start", value); }
    public static void setGradientLightEnd(int value) { GRADIENT_LIGHT_END = value; FileSystem.updateState("colors.light.end", value); }
    public static void setWaveWavelength(float value) { WAVE_WAVELENGTH = value; FileSystem.updateState("colors.wave.wavelength", value); }
    public static void setWaveSpeed(float value) { WAVE_SPEED = value; FileSystem.updateState("colors.wave.speed", value); }
    public static void setWaveAmplitude(float value) { WAVE_AMPLITUDE = value; FileSystem.updateState("colors.wave.amplitude", value); }

    public static int[] getGradientForType(String type) {
        if (type == null) {
            return new int[]{getGradientSecondaryStart(), getGradientSecondaryEnd()};
        }
        return switch (type) {
            case "success" -> new int[]{getGradientSuccessStart(), getGradientSuccessEnd()};
            case "warning" -> new int[]{getGradientWarningStart(), getGradientWarningEnd()};
            case "error" -> new int[]{getGradientErrorStart(), getGradientErrorEnd()};
            case "info" -> new int[]{getGradientInfoStart(), getGradientInfoEnd()};
            default -> new int[]{getGradientSecondaryStart(), getGradientSecondaryEnd()};
        };
    }

    public static int[] getCategoryHeaderGradient() {
        return new int[]{getGradientPrimaryStart(), getGradientPrimaryEnd()};
    }

    public static int[] getBackgroundGradient() {
        return new int[]{getGradientBgStart(), getGradientBgEnd()};
    }
}
