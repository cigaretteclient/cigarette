package dev.cigarette.lib;

import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;

public class Color {
    public static int lerpColor(int color1, int color2, float t) {
        if (t < 0f)
            t = 0f;
        else if (t > 1f)
            t = 1f;
        int a = ((int) lerp((color1 >> 24) & 0xFF, (color2 >> 24) & 0xFF, t)) << 24;
        int r = ((int) lerp((color1 >> 16) & 0xFF, (color2 >> 16) & 0xFF, t)) << 16;
        int g = ((int) lerp((color1 >> 8) & 0xFF, (color2 >> 8) & 0xFF, t)) << 8;
        int b = (int) lerp(color1 & 0xFF, color2 & 0xFF, t);
        return a | r | g | b;
    }

    public static int colorDarken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int scaleAlpha(int color, float scale) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int na = Math.max(0, Math.min(255, Math.round(a * scale)));
        return (na << 24) | (r << 16) | (g << 8) | b;
    }

    public static int colorTransparentize(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        float newA;
        if (factor <= 0f) {
            newA = a;
        } else if (factor <= 1f) {
            newA = a * (1f - factor);
        } else {
            newA = a / factor;
        }
        int ai = Math.max(0, Math.min(255, Math.round(newA)));
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (ai << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float start, float end, float t) {
        return start + (end - start) * t;
    }

    public static int color(int x, int y) {
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) x / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) y / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = lerpColor(CigaretteScreen.PRIMARY_COLOR, 0xFF020618, (float) pingpong);
        return bg;
    }

    public static int colorVertical(int x, int y) {
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) x / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) y / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = lerpColor(CigaretteScreen.PRIMARY_COLOR, 0xFF020618, (float) pingpong);
        return bg;
    }
}
