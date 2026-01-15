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

    /**
     * Converts HSL to RGB.
     * @param hue 0-360
     * @param saturation 0-100
     * @param lightness 0-100
     * @return ARGB int
     */
    public static int hslToRgb(double hue, double saturation, double lightness) {
        double h = hue / 360.0;
        double s = saturation / 100.0;
        double l = lightness / 100.0;

        double r, g, b;
        if (s == 0) {
            r = g = b = l;
        } else {
            double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
            double p = 2 * l - q;
            r = hueToRgb(p, q, h + 1.0/3.0);
            g = hueToRgb(p, q, h);
            b = hueToRgb(p, q, h - 1.0/3.0);
        }

        int ri = (int) Math.round(r * 255);
        int gi = (int) Math.round(g * 255);
        int bi = (int) Math.round(b * 255);

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    private static double hueToRgb(double p, double q, double t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0/6.0) return p + (q - p) * 6 * t;
        if (t < 1.0/2.0) return q;
        if (t < 2.0/3.0) return p + (q - p) * (2.0/3.0 - t) * 6;
        return p;
    }

    /**
     * Converts RGB to HSL.
     * @param rgb ARGB int
     * @return double[] {hue 0-360, saturation 0-100, lightness 0-100}
     */
    public static double[] rgbToHsl(int rgb) {
        double r = ((rgb >> 16) & 0xFF) / 255.0;
        double g = ((rgb >> 8) & 0xFF) / 255.0;
        double b = (rgb & 0xFF) / 255.0;

        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double diff = max - min;

        double h, s, l = (max + min) / 2;

        if (diff == 0) {
            h = s = 0;
        } else {
            s = l > 0.5 ? diff / (2 - max - min) : diff / (max + min);
            if (max == r) {
                h = (g - b) / diff + (g < b ? 6 : 0);
            } else if (max == g) {
                h = (b - r) / diff + 2;
            } else {
                h = (r - g) / diff + 4;
            }
            h /= 6;
        }

        return new double[]{h * 360, s * 100, l * 100};
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
