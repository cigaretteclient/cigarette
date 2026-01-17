package dev.cigarette.gui;

import dev.cigarette.config.FileSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Utility for rendering text with gradient colors.
 * Supports horizontal gradients that interpolate across the text width.
 */
public final class TextGradientRenderer {
    private TextGradientRenderer() {}
    
    // Default text gradient colors
    private static int TEXT_GRADIENT_START = 0xFFFE5F00;
    private static int TEXT_GRADIENT_END = 0xFFC44700;
    private static boolean TEXT_GRADIENTS_ENABLED = false;
    
    // Wave gradient colors (default orange and black)
    private static int TEXT_WAVE_START = 0xFFFFA500; // Orange
    private static int TEXT_WAVE_END = 0xFF000000;   // Black
    private static boolean TEXT_WAVE_ENABLED = false;
    
    static {
        registerConfig();
    }
    
    private static void registerConfig() {
        FileSystem.registerUpdate("text.gradient.enabled", (state) -> {
            if (state instanceof Boolean b) TEXT_GRADIENTS_ENABLED = b;
        });
        FileSystem.registerUpdate("text.gradient.start", (state) -> {
            if (state instanceof Integer i) TEXT_GRADIENT_START = i;
        });
        FileSystem.registerUpdate("text.gradient.end", (state) -> {
            if (state instanceof Integer i) TEXT_GRADIENT_END = i;
        });
        FileSystem.registerUpdate("text.wave.enabled", (state) -> {
            if (state instanceof Boolean b) TEXT_WAVE_ENABLED = b;
        });
        FileSystem.registerUpdate("text.wave.start", (state) -> {
            if (state instanceof Integer i) TEXT_WAVE_START = i;
        });
        FileSystem.registerUpdate("text.wave.end", (state) -> {
            if (state instanceof Integer i) TEXT_WAVE_END = i;
        });
    }
    
    public static boolean isTextGradientsEnabled() {
        return TEXT_GRADIENTS_ENABLED;
    }
    
    
    public static void setTextGradientsEnabled(boolean enabled) {
        TEXT_GRADIENTS_ENABLED = enabled;
        FileSystem.save("text.gradient.enabled", enabled);
    }
    
    public static void setTextGradientStart(int color) {
        TEXT_GRADIENT_START = color;
        FileSystem.save("text.gradient.start", color);
    }
    
    public static void setTextGradientEnd(int color) {
        TEXT_GRADIENT_END = color;
        FileSystem.save("text.gradient.end", color);
    }
    
    public static void setTextWaveStart(int color) {
        TEXT_WAVE_START = color;
        FileSystem.save("text.wave.start", color);
    }
    
    public static void setTextWaveEnd(int color) {
        TEXT_WAVE_END = color;
        FileSystem.save("text.wave.end", color);
    }
    
    public static int getTextGradientStart() {
        return TEXT_GRADIENT_START;
    }
    
    public static int getTextGradientEnd() {
        return TEXT_GRADIENT_END;
    }
    
    public static boolean isTextWaveEnabled() {
        return TEXT_WAVE_ENABLED;
    }
    
    public static void setTextWaveEnabled(boolean enabled) {
        TEXT_WAVE_ENABLED = enabled;
        FileSystem.save("text.wave.enabled", enabled);
    }
    
    public static int getTextWaveStart() {
        return TEXT_WAVE_START;
    }
    
    public static int getTextWaveEnd() {
        return TEXT_WAVE_END;
    }
    
    /**
     * Renders text with a horizontal gradient effect.
     * If gradients are disabled, renders with the start color.
     * 
     * @param context The draw context
     * @param textRenderer The text renderer
     * @param text The text to render
     * @param x X position
     * @param y Y position
     * @param startColor Gradient start color (ARGB)
     * @param endColor Gradient end color (ARGB)
     * @param shadow Whether to render with shadow
     */
    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, String text, 
                                        int x, int y, int startColor, int endColor, boolean shadow) {
        if (!TEXT_GRADIENTS_ENABLED) {
            // Fall back to normal rendering
            if (shadow) {
                context.drawTextWithShadow(textRenderer, text, x, y, startColor);
            } else {
                context.drawText(textRenderer, text, x, y, startColor, false);
            }
            return;
        }
        
        // Render character by character with interpolated colors
        int currentX = x;
        int textWidth = textRenderer.getWidth(text);
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = textRenderer.getWidth(charStr);
            
            // Calculate interpolation factor based on position
            float t = textWidth > 0 ? (float) (currentX - x) / textWidth : 0;
            int color = interpolateColor(startColor, endColor, t);
            
            if (shadow) {
                context.drawTextWithShadow(textRenderer, charStr, currentX, y, color);
            } else {
                context.drawText(textRenderer, charStr, currentX, y, color, false);
            }
            
            currentX += charWidth;
        }
    }
    
    /**
     * Renders text with the default configured gradient.
     */
    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, String text, 
                                        int x, int y, boolean shadow) {
        drawGradientText(context, textRenderer, text, x, y, TEXT_GRADIENT_START, TEXT_GRADIENT_END, shadow);
    }
    
    /**
     * Renders Text object with gradient.
     */
    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, Text text, 
                                        int x, int y, int startColor, int endColor, boolean shadow) {
        drawGradientText(context, textRenderer, text.getString(), x, y, startColor, endColor, shadow);
    }
    
    /**
     * Renders Text object with default gradient.
     */
    public static void drawGradientText(DrawContext context, TextRenderer textRenderer, Text text, 
                                        int x, int y, boolean shadow) {
        drawGradientText(context, textRenderer, text.getString(), x, y, TEXT_GRADIENT_START, TEXT_GRADIENT_END, shadow);
    }
    
    /**
     * Renders centered text with gradient.
     */
    public static void drawCenteredGradientText(DrawContext context, TextRenderer textRenderer, String text, 
                                                int centerX, int y, int startColor, int endColor, boolean shadow) {
        int textWidth = textRenderer.getWidth(text);
        int x = centerX - textWidth / 2;
        drawGradientText(context, textRenderer, text, x, y, startColor, endColor, shadow);
    }
    
    /**
     * Renders text with an animated wave gradient effect.
     * Uses ColorScheme wave parameters for animation.
     * 
     * @param context The draw context
     * @param textRenderer The text renderer
     * @param text The text to render
     * @param x X position
     * @param y Y position
     * @param startColor Wave start color (ARGB)
     * @param endColor Wave end color (ARGB)
     * @param shadow Whether to render with shadow
     */
    public static void drawWaveText(DrawContext context, TextRenderer textRenderer, String text, 
                                    int x, int y, int startColor, int endColor, boolean shadow) {
        if (!TEXT_WAVE_ENABLED) {
            // Fall back to normal rendering
            if (shadow) {
                context.drawTextWithShadow(textRenderer, text, x, y, startColor);
            } else {
                context.drawText(textRenderer, text, x, y, startColor, false);
            }
            return;
        }
        
        // Get wave parameters from ColorScheme
        float wavelength = ColorScheme.getWaveWavelength();
        float speed = ColorScheme.getWaveSpeed();
        float amplitude = ColorScheme.getWaveAmplitude();
        
        // Render character by character with wave-based color interpolation
        int currentX = x;
        int textWidth = textRenderer.getWidth(text);
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String charStr = String.valueOf(c);
            int charWidth = textRenderer.getWidth(charStr);
            
            // Calculate wave-based interpolation factor
            // Position-based wave: sin(position / wavelength + time * speed)
            float time = GradientRenderer.getGlobalTimeSeconds();
            float positionFactor = (float) currentX / wavelength;
            float waveFactor = (float) Math.sin(positionFactor + time * speed * Math.PI * 2) * 0.5f + 0.5f;
            waveFactor = waveFactor * amplitude + (1.0f - amplitude) * 0.5f; // Mix with base
            
            int color = interpolateColor(startColor, endColor, waveFactor);
            
            if (shadow) {
                context.drawTextWithShadow(textRenderer, charStr, currentX, y, color);
            } else {
                context.drawText(textRenderer, charStr, currentX, y, color, false);
            }
            
            currentX += charWidth;
        }
    }
    
    /**
     * Renders text with the default configured wave gradient.
     */
    public static void drawWaveText(DrawContext context, TextRenderer textRenderer, String text, 
                                    int x, int y, boolean shadow) {
        drawWaveText(context, textRenderer, text, x, y, TEXT_WAVE_START, TEXT_WAVE_END, shadow);
    }
    
    /**
     * Renders Text object with wave gradient.
     */
    public static void drawWaveText(DrawContext context, TextRenderer textRenderer, Text text, 
                                    int x, int y, int startColor, int endColor, boolean shadow) {
        drawWaveText(context, textRenderer, text.getString(), x, y, startColor, endColor, shadow);
    }
    
    /**
     * Renders Text object with default wave gradient.
     */
    public static void drawWaveText(DrawContext context, TextRenderer textRenderer, Text text, 
                                    int x, int y, boolean shadow) {
        drawWaveText(context, textRenderer, text.getString(), x, y, TEXT_WAVE_START, TEXT_WAVE_END, shadow);
    }
    
    /**
     * Renders centered text with wave gradient.
     */
    public static void drawCenteredWaveText(DrawContext context, TextRenderer textRenderer, String text, 
                                            int centerX, int y, int startColor, int endColor, boolean shadow) {
        int textWidth = textRenderer.getWidth(text);
        int x = centerX - textWidth / 2;
        drawWaveText(context, textRenderer, text, x, y, startColor, endColor, shadow);
    }
    
    /**
     * Renders centered text with default wave gradient.
     */
    public static void drawCenteredWaveText(DrawContext context, TextRenderer textRenderer, String text, 
                                            int centerX, int y, boolean shadow) {
        drawCenteredWaveText(context, textRenderer, text, centerX, y, TEXT_WAVE_START, TEXT_WAVE_END, shadow);
    }
    
    /**
     * Interpolates between two ARGB colors.
     */
    private static int interpolateColor(int color1, int color2, float t) {
        t = Math.max(0, Math.min(1, t));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
