package dev.cigarette.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Utility class for rendering gradients using DrawContext fill operations.
 * Provides GPU-efficient gradient rendering for GUI elements.
 */
public class GradientRenderer {

    private static final long WAVE_START_TIME = System.currentTimeMillis();

    /**
     * Shared animated time used to keep gradient waves in sync across widgets.
     */
    public static float getGlobalTimeSeconds() {
        return (System.currentTimeMillis() - WAVE_START_TIME) / 1000.0f;
    }

    /**
     * Renders a horizontal linear gradient from left to right.
     * 
     * @param context    The draw context
     * @param left       Left coordinate
     * @param top        Top coordinate
     * @param right      Right coordinate
     * @param bottom     Bottom coordinate
     * @param colorLeft  Color at left edge (ARGB)
     * @param colorRight Color at right edge (ARGB)
     */
    public static void renderHorizontalGradient(DrawContext context, int left, int top, int right, int bottom,
            int colorLeft, int colorRight) {
        int width = right - left;
        if (width <= 0)
            return;

        float invWidth = 1.0f / width;

        // Extract color components
        int leftA = (colorLeft >> 24) & 0xFF;
        int leftR = (colorLeft >> 16) & 0xFF;
        int leftG = (colorLeft >> 8) & 0xFF;
        int leftB = colorLeft & 0xFF;

        int rightA = (colorRight >> 24) & 0xFF;
        int rightR = (colorRight >> 16) & 0xFF;
        int rightG = (colorRight >> 8) & 0xFF;
        int rightB = colorRight & 0xFF;

        int diffA = rightA - leftA;
        int diffR = rightR - leftR;
        int diffG = rightG - leftG;
        int diffB = rightB - leftB;

        // Draw gradient by rendering vertical strips
        for (int x = left; x < right; x++) {
            float t = (x - left) * invWidth;

            int a = (int) (leftA + t * diffA);
            int r = (int) (leftR + t * diffR);
            int g = (int) (leftG + t * diffG);
            int b = (int) (leftB + t * diffB);

            int color = (a << 24) | (r << 16) | (g << 8) | b;
            context.fill(x, top, x + 1, bottom, color);
        }
    }

    /**
     * Renders a vertical linear gradient from top to bottom.
     * 
     * @param context     The draw context
     * @param left        Left coordinate
     * @param top         Top coordinate
     * @param right       Right coordinate
     * @param bottom      Bottom coordinate
     * @param colorTop    Color at top edge (ARGB)
     * @param colorBottom Color at bottom edge (ARGB)
     */
    public static void renderVerticalGradient(DrawContext context, int left, int top, int right, int bottom,
            int colorTop, int colorBottom) {
        int height = bottom - top;
        if (height <= 0)
            return;

        float invHeight = 1.0f / height;

        // Extract color components
        int topA = (colorTop >> 24) & 0xFF;
        int topR = (colorTop >> 16) & 0xFF;
        int topG = (colorTop >> 8) & 0xFF;
        int topB = colorTop & 0xFF;

        int bottomA = (colorBottom >> 24) & 0xFF;
        int bottomR = (colorBottom >> 16) & 0xFF;
        int bottomG = (colorBottom >> 8) & 0xFF;
        int bottomB = colorBottom & 0xFF;

        int diffA = bottomA - topA;
        int diffR = bottomR - topR;
        int diffG = bottomG - topG;
        int diffB = bottomB - topB;

        // Draw gradient by rendering horizontal strips
        for (int y = top; y < bottom; y++) {
            float t = (y - top) * invHeight;

            int a = (int) (topA + t * diffA);
            int r = (int) (topR + t * diffR);
            int g = (int) (topG + t * diffG);
            int b = (int) (topB + t * diffB);

            int color = (a << 24) | (r << 16) | (g << 8) | b;
            context.fill(left, y, right, y + 1, color);
        }
    }

    /**
     * Renders a linear gradient from startColor to endColor.
     * 
     * @param context    The draw context
     * @param x1         Start X coordinate
     * @param y1         Start Y coordinate
     * @param x2         End X coordinate
     * @param y2         End Y coordinate
     * @param startColor Start color (ARGB)
     * @param endColor   End color (ARGB)
     */
    public static void renderLinearGradient(DrawContext context, int x1, int y1, int x2, int y2, int startColor,
            int endColor) {
        // For simplicity, assume horizontal or vertical gradients
        if (x1 == x2) {
            // Vertical gradient
            renderVerticalGradient(context, x1, Math.min(y1, y2), x2, Math.max(y1, y2), y1 < y2 ? startColor : endColor,
                    y1 < y2 ? endColor : startColor);
        } else if (y1 == y2) {
            // Horizontal gradient
            renderHorizontalGradient(context, Math.min(x1, x2), y1, Math.max(x1, x2), y2,
                    x1 < x2 ? startColor : endColor, x1 < x2 ? endColor : startColor);
        } else {
            // Diagonal - approximate with horizontal gradient for now
            renderHorizontalGradient(context, Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2),
                    startColor, endColor);
        }
    }

    /**
     * Renders a radial gradient from center color to edge color.
     * 
     * @param context     The draw context
     * @param centerX     Center X coordinate
     * @param centerY     Center Y coordinate
     * @param radius      Gradient radius
     * @param centerColor Center color (ARGB)
     * @param edgeColor   Edge color (ARGB)
     */
    public static void renderRadialGradient(DrawContext context, int centerX, int centerY, int radius, int centerColor,
            int edgeColor) {
        // Simple radial gradient approximation using concentric circles
        for (int r = 0; r < radius; r++) {
            float t = (float) r / radius;

            // Extract color components
            int centerA = (centerColor >> 24) & 0xFF;
            int centerR = (centerColor >> 16) & 0xFF;
            int centerG = (centerColor >> 8) & 0xFF;
            int centerB = centerColor & 0xFF;

            int edgeA = (edgeColor >> 24) & 0xFF;
            int edgeR = (edgeColor >> 16) & 0xFF;
            int edgeG = (edgeColor >> 8) & 0xFF;
            int edgeB = edgeColor & 0xFF;

            int a = (int) (centerA + t * (edgeA - centerA));
            int red = (int) (centerR + t * (edgeR - centerR));
            int g = (int) (centerG + t * (edgeG - centerG));
            int b = (int) (centerB + t * (edgeB - centerB));

            int color = (a << 24) | (red << 16) | (g << 8) | b;

            // Draw circle outline at this radius
            int segments = Math.max(8, r * 2);
            double angleStep = 2 * Math.PI / segments;
            for (int i = 0; i < segments; i++) {
                double angle = i * angleStep;
                int x = centerX + (int) (r * Math.cos(angle));
                int y = centerY + (int) (r * Math.sin(angle));
                context.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    /**
     * Renders a horizontal gradient bar (useful for headers).
     * 
     * @param context    The draw context
     * @param x          Start X coordinate
     * @param y          Start Y coordinate
     * @param width      Bar width
     * @param height     Bar height
     * @param leftColor  Left color (ARGB)
     * @param rightColor Right color (ARGB)
     */
    public static void renderHorizontalGradientBar(DrawContext context, int x, int y, int width, int height,
            int leftColor, int rightColor) {
        renderHorizontalGradient(context, x, y, x + width, y + height, leftColor, rightColor);
    }

    /**
     * Renders a vertical gradient bar.
     * 
     * @param context     The draw context
     * @param x           Start X coordinate
     * @param y           Start Y coordinate
     * @param width       Bar width
     * @param height      Bar height
     * @param topColor    Top color (ARGB)P
     * @param bottomColor Bottom color (ARGB)
     */
    public static void renderVerticalGradientBar(DrawContext context, int x, int y, int width, int height, int topColor,
            int bottomColor) {
        renderVerticalGradient(context, x, y, x + width, y + height, topColor, bottomColor);
    }

    /**
     * Adds a subtle satin sheen overlay using a light-to-transparent vertical fade.
     */
    public static void renderSatinOverlay(DrawContext context, int left, int top, int right, int bottom) {
        int height = bottom - top;
        if (height <= 0) return;

        // Fade alpha from 30% at the top to 0% at the bottom for a gentle gloss.
        float invHeight = 1.0f / height;
        for (int y = 0; y < height; y++) {
            float t = y * invHeight;
            int alpha = (int) (60 * (1.0f - t));
            int color = (alpha << 24) | 0xFFFFFF;
            context.fill(left, top + y, right, top + y + 1, color);
        }
    }

    /**
     * Renders an animated vertical gradient that smoothly interpolates between colors over time.
     * This is much more efficient than the previous wave implementation.
     *
     * @param context       Draw context
     * @param left          Left coordinate
     * @param top           Top coordinate
     * @param right         Right coordinate
     * @param bottom        Bottom coordinate
     * @param colorTop      Base top color (ARGB)
     * @param colorBottom   Base bottom color (ARGB)
     * @param wavelengthPx  Unused (kept for API compatibility)
     * @param speedCps      Speed multiplier for animation
     * @param amplitude     Animation intensity (0-1)
     * @param phaseOffset   Phase offset for animation desync
     */
    public static void renderVerticalWaveGradient(DrawContext context, int left, int top, int right, int bottom,
            int colorTop, int colorBottom, float wavelengthPx, float speedCps, float amplitude, float phaseOffset) {
        // Calculate time-based interpolation factor
        float time = getGlobalTimeSeconds() * speedCps + phaseOffset;
        float animFactor = (float) Math.sin(time * Math.PI) * 0.5f + 0.5f; // Oscillates between 0 and 1
        animFactor = animFactor * Math.min(1.0f, amplitude);
        
        // Interpolate between the two colors based on animation
        int animatedTop = interpolateColor(colorTop, colorBottom, animFactor * 0.3f);
        int animatedBottom = interpolateColor(colorBottom, colorTop, animFactor * 0.3f);
        
        // Use efficient vertical gradient rendering
        renderVerticalGradient(context, left, top, right, bottom, animatedTop, animatedBottom);
    }
    
    /**
     * Renders an animated horizontal gradient that smoothly interpolates between colors over time.
     * This is much more efficient than per-pixel wave rendering.
     *
     * @param context       Draw context
     * @param left          Left coordinate
     * @param top           Top coordinate
     * @param right         Right coordinate
     * @param bottom        Bottom coordinate
     * @param colorLeft     Base left color (ARGB)
     * @param colorRight    Base right color (ARGB)
     * @param wavelengthPx  Unused (kept for API compatibility)
     * @param speedCps      Speed multiplier for animation
     * @param amplitude     Animation intensity (0-1)
     * @param phaseOffset   Phase offset for animation desync
     */
    public static void renderHorizontalWaveGradient(DrawContext context, int left, int top, int right, int bottom,
            int colorLeft, int colorRight, float wavelengthPx, float speedCps, float amplitude, float phaseOffset) {
        // Calculate time-based interpolation factor
        float time = getGlobalTimeSeconds() * speedCps + phaseOffset;
        float animFactor = (float) Math.sin(time * Math.PI) * 0.5f + 0.5f; // Oscillates between 0 and 1
        animFactor = animFactor * Math.min(1.0f, amplitude);
        
        // Interpolate between the two colors based on animation
        int animatedLeft = interpolateColor(colorLeft, colorRight, animFactor * 0.3f);
        int animatedRight = interpolateColor(colorRight, colorLeft, animFactor * 0.3f);
        
        // Use efficient horizontal gradient rendering
        renderHorizontalGradient(context, left, top, right, bottom, animatedLeft, animatedRight);
    }
    
    /**
     * Interpolates between two colors.
     */
    private static int interpolateColor(int color1, int color2, float t) {
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
