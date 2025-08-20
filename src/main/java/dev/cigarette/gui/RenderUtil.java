package dev.cigarette.gui;

import com.mojang.blaze3d.systems.RenderSystem;

public final class RenderUtil {
    private RenderUtil() {}

    public static void pushOpacity(float alpha) {
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
    }

    public static void popOpacity() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
