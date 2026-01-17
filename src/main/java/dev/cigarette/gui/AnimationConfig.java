package dev.cigarette.gui;

import dev.cigarette.config.FileSystem;

public final class AnimationConfig {
    private AnimationConfig() {}

    // Animation enable/disable toggles
    private static boolean GUI_ANIMATIONS_ENABLED = true;
    private static boolean HUD_ANIMATIONS_ENABLED = true;
    private static boolean WIDGET_ANIMATIONS_ENABLED = true;
    
    // Animation durations (in seconds)
    private static double GUI_OPEN_DURATION = 0.2;
    private static double GUI_CLOSE_DURATION_FACTOR = 0.8;
    private static double GUI_STAGGER_DURATION = 0.03;
    private static double HUD_TRANSITION_DURATION = 0.3;
    private static double WIDGET_TRANSITION_DURATION = 0.25;
    
    // Animation distances/intensities
    private static int GUI_SLIDE_DISTANCE = 24;
    private static double GUI_SCALE_FACTOR = 0.0; // 0 = scale from 0, 1 = no scaling
    private static double GUI_FADE_ALPHA = 0.0; // 0 = fully transparent start, 1 = opaque start
    
    // Easing function selection
    private static EasingFunction GUI_EASING = EasingFunction.EASE_OUT_CUBIC;
    private static EasingFunction HUD_EASING = EasingFunction.EASE_OUT_EXPO;
    private static EasingFunction WIDGET_EASING = EasingFunction.EASE_OUT_CUBIC;
    
    // Border configuration
    private static boolean BORDER_ENABLED = true;
    private static int BORDER_COLOR = 0xFFFE5F00; // Orange
    private static int BORDER_WIDTH = 1;
    
    // Color configuration
    private static int PRIMARY_COLOR = 0xFFFE5F00; // Orange
    private static int SECONDARY_COLOR = 0xFFC44700; // Dark orange
    private static int PRIMARY_TEXT_COLOR = 0xFFFFFFFF; // White
    private static int SECONDARY_TEXT_COLOR = 0xFFAAAAAA; // Gray
    private static int BACKGROUND_COLOR = 0xFF1A1A1A; // Dark gray
    private static int DARK_BACKGROUND_COLOR = 0xFF0D0D0D; // Darker gray
    private static int HOVER_COLOR = 0xFF2A2A2A; // Light dark gray
    private static int SELECTION_COLOR = 0xFFFE5F00; // Orange (same as primary)
    private static int CHECKBOX_BACKGROUND_COLOR = 0xFF1A1A1A; // Dark gray
    private static int CHECKBOX_BORDER_COLOR = 0xFFFFFFFF; // White
    private static int CHECKBOX_CHECK_COLOR = 0xFF00FF00; // Green

    static {
        registerConfig();
    }

    private static void registerConfig() {
        // Enable/disable toggles
        FileSystem.registerUpdate("animations.gui.enabled", (state) -> {
            if (state instanceof Boolean b) GUI_ANIMATIONS_ENABLED = b;
        });
        FileSystem.registerUpdate("animations.hud.enabled", (state) -> {
            if (state instanceof Boolean b) HUD_ANIMATIONS_ENABLED = b;
        });
        FileSystem.registerUpdate("animations.widgets.enabled", (state) -> {
            if (state instanceof Boolean b) WIDGET_ANIMATIONS_ENABLED = b;
        });
        
        // Duration settings
        FileSystem.registerUpdate("animations.gui.open.duration", (state) -> {
            if (state instanceof Double d) GUI_OPEN_DURATION = Math.max(0.01, d);
        });
        FileSystem.registerUpdate("animations.gui.close.factor", (state) -> {
            if (state instanceof Double d) GUI_CLOSE_DURATION_FACTOR = Math.max(0.01, d);
        });
        FileSystem.registerUpdate("animations.gui.stagger", (state) -> {
            if (state instanceof Double d) GUI_STAGGER_DURATION = Math.max(0.0, d);
        });
        FileSystem.registerUpdate("animations.hud.duration", (state) -> {
            if (state instanceof Double d) HUD_TRANSITION_DURATION = Math.max(0.01, d);
        });
        FileSystem.registerUpdate("animations.widgets.duration", (state) -> {
            if (state instanceof Double d) WIDGET_TRANSITION_DURATION = Math.max(0.01, d);
        });
        
        // Animation properties
        FileSystem.registerUpdate("animations.gui.slide.distance", (state) -> {
            if (state instanceof Integer i) GUI_SLIDE_DISTANCE = Math.max(0, i);
            else if (state instanceof Double d) GUI_SLIDE_DISTANCE = Math.max(0, d.intValue());
        });
        FileSystem.registerUpdate("animations.gui.scale.factor", (state) -> {
            if (state instanceof Double d) GUI_SCALE_FACTOR = Math.max(0.0, Math.min(1.0, d));
        });
        FileSystem.registerUpdate("animations.gui.fade.alpha", (state) -> {
            if (state instanceof Double d) GUI_FADE_ALPHA = Math.max(0.0, Math.min(1.0, d));
        });
        
        // Easing functions
        FileSystem.registerUpdate("animations.gui.easing", (state) -> {
            if (state instanceof String s) GUI_EASING = EasingFunction.fromString(s);
        });
        FileSystem.registerUpdate("animations.hud.easing", (state) -> {
            if (state instanceof String s) HUD_EASING = EasingFunction.fromString(s);
        });
        FileSystem.registerUpdate("animations.widgets.easing", (state) -> {
            if (state instanceof String s) WIDGET_EASING = EasingFunction.fromString(s);
        });
        
        // Border configuration
        FileSystem.registerUpdate("gui.border.enabled", (state) -> {
            if (state instanceof Boolean b) BORDER_ENABLED = b;
        });
        FileSystem.registerUpdate("gui.border.color", (state) -> {
            if (state instanceof Integer i) BORDER_COLOR = i;
            else if (state instanceof String s) {
                try {
                    BORDER_COLOR = (int) Long.parseLong(s.replace("0x", "").replace("0X", ""), 16);
                } catch (NumberFormatException e) {
                    BORDER_COLOR = 0xFFFE5F00;
                }
            }
        });
        FileSystem.registerUpdate("gui.border.width", (state) -> {
            if (state instanceof Integer i) BORDER_WIDTH = Math.max(1, i);
            else if (state instanceof Double d) BORDER_WIDTH = Math.max(1, d.intValue());
        });
        
        // Color configuration
        FileSystem.registerUpdate("gui.colors.primary", (state) -> {
            if (state instanceof Integer i) PRIMARY_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> PRIMARY_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.secondary", (state) -> {
            if (state instanceof Integer i) SECONDARY_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> SECONDARY_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.primary_text", (state) -> {
            if (state instanceof Integer i) PRIMARY_TEXT_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> PRIMARY_TEXT_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.secondary_text", (state) -> {
            if (state instanceof Integer i) SECONDARY_TEXT_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> SECONDARY_TEXT_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.background", (state) -> {
            if (state instanceof Integer i) BACKGROUND_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> BACKGROUND_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.dark_background", (state) -> {
            if (state instanceof Integer i) DARK_BACKGROUND_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> DARK_BACKGROUND_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.hover", (state) -> {
            if (state instanceof Integer i) HOVER_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> HOVER_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.selection", (state) -> {
            if (state instanceof Integer i) SELECTION_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> SELECTION_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.checkbox_background", (state) -> {
            if (state instanceof Integer i) CHECKBOX_BACKGROUND_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> CHECKBOX_BACKGROUND_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.checkbox_border", (state) -> {
            if (state instanceof Integer i) CHECKBOX_BORDER_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> CHECKBOX_BORDER_COLOR = v);
        });
        FileSystem.registerUpdate("gui.colors.checkbox_check", (state) -> {
            if (state instanceof Integer i) CHECKBOX_CHECK_COLOR = i;
            else if (state instanceof String s) parseHexColor(s, v -> CHECKBOX_CHECK_COLOR = v);
        });
    }
    
    /**
     * Parses a hex color string and applies it via the provided setter.
     */
    private static void parseHexColor(String s, java.util.function.IntConsumer setter) {
        try {
            int value = (int) Long.parseLong(s.replace("0x", "").replace("0X", "").replace("#", ""), 16);
            setter.accept(value | 0xFF000000); // Ensure alpha is FF
        } catch (NumberFormatException e) {
            // Keep existing value on parse error
        }
    }

    // Getters for animation toggles
    public static boolean isGuiAnimationsEnabled() { return GUI_ANIMATIONS_ENABLED; }
    public static boolean isHudAnimationsEnabled() { return HUD_ANIMATIONS_ENABLED; }
    public static boolean isWidgetAnimationsEnabled() { return WIDGET_ANIMATIONS_ENABLED; }
    
    // Getters for durations
    public static double getGuiOpenDuration() { return GUI_OPEN_DURATION; }
    public static double getGuiCloseDurationFactor() { return GUI_CLOSE_DURATION_FACTOR; }
    public static double getGuiStaggerDuration() { return GUI_STAGGER_DURATION; }
    public static double getHudTransitionDuration() { return HUD_TRANSITION_DURATION; }
    public static double getWidgetTransitionDuration() { return WIDGET_TRANSITION_DURATION; }
    
    // Getters for animation properties
    public static int getGuiSlideDistance() { return GUI_SLIDE_DISTANCE; }
    public static double getGuiScaleFactor() { return GUI_SCALE_FACTOR; }
    public static double getGuiFadeAlpha() { return GUI_FADE_ALPHA; }
    
    // Getters for easing functions
    public static EasingFunction getGuiEasing() { return GUI_EASING; }
    public static EasingFunction getHudEasing() { return HUD_EASING; }
    public static EasingFunction getWidgetEasing() { return WIDGET_EASING; }
    
    // Getters for border configuration
    public static boolean isBorderEnabled() { return BORDER_ENABLED; }
    public static int getBorderColor() { return BORDER_COLOR; }
    public static int getBorderWidth() { return BORDER_WIDTH; }
    
    // Getters for color configuration
    public static int getPrimaryColor() { return PRIMARY_COLOR; }
    public static int getSecondaryColor() { return SECONDARY_COLOR; }
    public static int getPrimaryTextColor() { return PRIMARY_TEXT_COLOR; }
    public static int getSecondaryTextColor() { return SECONDARY_TEXT_COLOR; }
    public static int getBackgroundColor() { return BACKGROUND_COLOR; }
    public static int getDarkBackgroundColor() { return DARK_BACKGROUND_COLOR; }
    public static int getHoverColor() { return HOVER_COLOR; }
    public static int getSelectionColor() { return SELECTION_COLOR; }
    public static int getCheckboxBackgroundColor() { return CHECKBOX_BACKGROUND_COLOR; }
    public static int getCheckboxBorderColor() { return CHECKBOX_BORDER_COLOR; }
    public static int getCheckboxCheckColor() { return CHECKBOX_CHECK_COLOR; }
    
    // Setters (for GUI configuration)
    public static void setGuiAnimationsEnabled(boolean enabled) {
        GUI_ANIMATIONS_ENABLED = enabled;
        FileSystem.save("animations.gui.enabled", enabled);
    }
    
    public static void setHudAnimationsEnabled(boolean enabled) {
        HUD_ANIMATIONS_ENABLED = enabled;
        FileSystem.save("animations.hud.enabled", enabled);
    }
    
    public static void setWidgetAnimationsEnabled(boolean enabled) {
        WIDGET_ANIMATIONS_ENABLED = enabled;
        FileSystem.save("animations.widgets.enabled", enabled);
    }

    // Setters for animation durations
    public static void setGuiOpenDuration(double duration) {
        GUI_OPEN_DURATION = Math.max(0.01, duration);
        FileSystem.save("animations.gui.open.duration", GUI_OPEN_DURATION);
    }

    public static void setGuiCloseDurationFactor(double factor) {
        GUI_CLOSE_DURATION_FACTOR = Math.max(0.01, factor);
        FileSystem.save("animations.gui.close.factor", GUI_CLOSE_DURATION_FACTOR);
    }

    public static void setGuiStaggerDuration(double duration) {
        GUI_STAGGER_DURATION = Math.max(0, duration);
        FileSystem.save("animations.gui.stagger.duration", GUI_STAGGER_DURATION);
    }

    public static void setHudTransitionDuration(double duration) {
        HUD_TRANSITION_DURATION = Math.max(0.01, duration);
        FileSystem.save("animations.hud.transition.duration", HUD_TRANSITION_DURATION);
    }

    public static void setWidgetTransitionDuration(double duration) {
        WIDGET_TRANSITION_DURATION = Math.max(0.01, duration);
        FileSystem.save("animations.widgets.transition.duration", WIDGET_TRANSITION_DURATION);
    }

    // Setters for animation properties
    public static void setGuiSlideDistance(int distance) {
        GUI_SLIDE_DISTANCE = Math.max(0, distance);
        FileSystem.save("animations.gui.slide.distance", GUI_SLIDE_DISTANCE);
    }

    public static void setGuiScaleFactor(double factor) {
        GUI_SCALE_FACTOR = Math.max(0, Math.min(1, factor));
        FileSystem.save("animations.gui.scale.factor", GUI_SCALE_FACTOR);
    }

    public static void setGuiFadeAlpha(double alpha) {
        GUI_FADE_ALPHA = Math.max(0, Math.min(1, alpha));
        FileSystem.save("animations.gui.fade.alpha", GUI_FADE_ALPHA);
    }

    // Setters for border configuration
    public static void setBorderEnabled(boolean enabled) {
        BORDER_ENABLED = enabled;
        FileSystem.save("gui.border.enabled", enabled);
    }

    public static void setBorderColor(int color) {
        BORDER_COLOR = color;
        FileSystem.save("gui.border.color", color);
    }

    public static void setBorderWidth(int width) {
        BORDER_WIDTH = Math.max(1, width);
        FileSystem.save("gui.border.width", width);
    }

    // Setters for color configuration
    public static void setPrimaryColor(int color) {
        PRIMARY_COLOR = color;
        FileSystem.save("gui.colors.primary", color);
    }

    public static void setSecondaryColor(int color) {
        SECONDARY_COLOR = color;
        FileSystem.save("gui.colors.secondary", color);
    }

    public static void setPrimaryTextColor(int color) {
        PRIMARY_TEXT_COLOR = color;
        FileSystem.save("gui.colors.primary_text", color);
    }

    public static void setSecondaryTextColor(int color) {
        SECONDARY_TEXT_COLOR = color;
        FileSystem.save("gui.colors.secondary_text", color);
    }

    public static void setBackgroundColor(int color) {
        BACKGROUND_COLOR = color;
        FileSystem.save("gui.colors.background", color);
    }

    public static void setDarkBackgroundColor(int color) {
        DARK_BACKGROUND_COLOR = color;
        FileSystem.save("gui.colors.dark_background", color);
    }

    public static void setHoverColor(int color) {
        HOVER_COLOR = color;
        FileSystem.save("gui.colors.hover", color);
    }

    public static void setSelectionColor(int color) {
        SELECTION_COLOR = color;
        FileSystem.save("gui.colors.selection", color);
    }

    public static void setCheckboxBackgroundColor(int color) {
        CHECKBOX_BACKGROUND_COLOR = color;
        FileSystem.save("gui.colors.checkbox_background", color);
    }

    public static void setCheckboxBorderColor(int color) {
        CHECKBOX_BORDER_COLOR = color;
        FileSystem.save("gui.colors.checkbox_border", color);
    }

    public static void setCheckboxCheckColor(int color) {
        CHECKBOX_CHECK_COLOR = color;
        FileSystem.save("gui.colors.checkbox_check", color);
    }

    /**
     * Easing functions for smooth animations.
     */
    public enum EasingFunction {
        LINEAR,
        EASE_IN_CUBIC,
        EASE_OUT_CUBIC,
        EASE_IN_OUT_CUBIC,
        EASE_IN_EXPO,
        EASE_OUT_EXPO,
        EASE_IN_OUT_EXPO,
        EASE_OUT_ELASTIC,
        EASE_OUT_BACK;

        public double apply(double t) {
            t = Math.max(0.0, Math.min(1.0, t));
            return switch (this) {
                case LINEAR -> t;
                case EASE_IN_CUBIC -> t * t * t;
                case EASE_OUT_CUBIC -> 1 - Math.pow(1 - t, 3);
                case EASE_IN_OUT_CUBIC -> t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
                case EASE_IN_EXPO -> t <= 0.0 ? 0.0 : Math.pow(2.0, 10.0 * (t - 1.0));
                case EASE_OUT_EXPO -> t >= 1.0 ? 1.0 : 1.0 - Math.pow(2.0, -10.0 * t);
                case EASE_IN_OUT_EXPO -> {
                    if (t <= 0.0) yield 0.0;
                    if (t >= 1.0) yield 1.0;
                    if (t < 0.5) yield Math.pow(2.0, 20.0 * t - 10.0) / 2.0;
                    yield (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
                }
                case EASE_OUT_ELASTIC -> {
                    if (t >= 1.0) yield 1.0;
                    if (t <= 0.0) yield 0.0;
                    double p = 0.3;
                    double s = p / 4.0;
                    yield 1.0 - Math.pow(2.0, -10.0 * t) * Math.sin((t - s) * (2.0 * Math.PI) / p);
                }
                case EASE_OUT_BACK -> {
                    double s = 1.70158;
                    double t1 = t - 1.0;
                    yield 1.0 + t1 * t1 * ((s + 1.0) * t1 + s);
                }
            };
        }

        public static EasingFunction fromString(String name) {
            try {
                return valueOf(name.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return EASE_OUT_CUBIC;
            }
        }
    }
}
