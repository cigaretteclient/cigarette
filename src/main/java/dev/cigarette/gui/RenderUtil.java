package dev.cigarette.gui;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Custom utilities for screen rendering.
 */
public final class RenderUtil {
    private RenderUtil() {}

    /**
     * Set the opacity of the renderer.
     *
     * @param alpha The opacity to set, between 0 and 1
     */
    public static void pushOpacity(float alpha) {
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
    }

    /**
     * Reset the opacity of the renderer.
     */
    public static void popOpacity() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
