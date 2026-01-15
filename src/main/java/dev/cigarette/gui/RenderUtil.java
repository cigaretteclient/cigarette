package dev.cigarette.gui;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Custom utilities for screen rendering.
 */
public final class RenderUtil {
    private RenderUtil() {}

    // 0xAARRGGBB
    public static int modifyOpacity(int color, float opacity) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int newA = Math.round(a * opacity);
        return (newA << 24) | (r << 16) | (g << 8) | b;
    }
}
