package dev.cigarette.gui.widget;

import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;

/**
 * A widget that renders a color wheel for HSL-based color selection.
 */
public class ColorWheelWidget extends BaseWidget<Integer> {
    /**
     * The size of the color wheel in pixels.
     */
    private static final int WHEEL_SIZE = 120;
    /**
     * The inner radius offset to exclude the center.
     */
    private static final int INNER_RADIUS_OFFSET = 10;
    /**
     * Saturation level (0-100%).
     */
    private double saturation = 100.0;
    /**
     * Lightness level (0-100%).
     */
    private double lightness = 50.0;
    /**
     * Whether the user is dragging on the color wheel.
     */
    private boolean dragging = false;

    /**
     * Creates a color wheel widget for HSL color selection.
     */
    public ColorWheelWidget() {
        super("", null);
        this.withDefault(0xFFFF0000);
        this.captureHover();
    }

    /**
     * {@return the saturation level (0-100)}
     */
    public double getSaturation() {
        return saturation;
    }

    /**
     * Sets the saturation level.
     *
     * @param saturation The saturation level (0-100)
     */
    public void setSaturation(double saturation) {
        this.saturation = Math.max(0, Math.min(100, saturation));
        updateColorFromHSL();
    }

    /**
     * {@return the lightness level (0-100)}
     */
    public double getLightness() {
        return lightness;
    }

    /**
     * Sets the lightness level.
     *
     * @param lightness The lightness level (0-100)
     */
    public void setLightness(double lightness) {
        this.lightness = Math.max(0, Math.min(100, lightness));
        updateColorFromHSL();
    }

    /**
     * Sets the color from HSL values.
     *
     * @param hue The hue (0-360)
     * @param saturation The saturation (0-100)
     * @param lightness The lightness (0-100)
     */
    public void setColorFromHSL(double hue, double saturation, double lightness) {
        this.saturation = saturation;
        this.lightness = lightness;
        updateColorFromHSL(hue);
    }

    /**
     * Gets the current hue from the color.
     *
     * @return The hue (0-360)
     */
    public double getHue() {
        return rgbToHue(this.getRawState());
    }

    /**
     * Updates the color based on current HSL values.
     */
    private void updateColorFromHSL() {
        updateColorFromHSL(getHue());
    }

    /**
     * Updates the color based on HSL values with a specific hue.
     *
     * @param hue The hue (0-360)
     */
    private void updateColorFromHSL(double hue) {
        int argb = hslToRgb(hue, saturation, lightness);
        this.setRawState(argb);
    }

    /**
     * Converts HSL to ARGB color.
     *
     * @param hue Hue in degrees (0-360)
     * @param saturation Saturation percentage (0-100)
     * @param lightness Lightness percentage (0-100)
     * @return The color in ARGB format
     */
    public static int hslToRgb(double hue, double saturation, double lightness) {
        saturation /= 100;
        lightness /= 100;

        double c = (1 - Math.abs(2 * lightness - 1)) * saturation;
        double hPrime = hue / 60.0;
        double x = c * (1 - Math.abs(hPrime % 2 - 1));

        double r = 0, g = 0, b = 0;
        if (hPrime < 1) {
            r = c;
            g = x;
        } else if (hPrime < 2) {
            r = x;
            g = c;
        } else if (hPrime < 3) {
            g = c;
            b = x;
        } else if (hPrime < 4) {
            g = x;
            b = c;
        } else if (hPrime < 5) {
            r = x;
            b = c;
        } else {
            r = c;
            b = x;
        }

        double m = lightness - c / 2;
        r += m;
        g += m;
        b += m;

        int ri = Math.round((float) (r * 255)) & 0xFF;
        int gi = Math.round((float) (g * 255)) & 0xFF;
        int bi = Math.round((float) (b * 255)) & 0xFF;

        return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
    }

    /**
     * Converts RGB color to hue.
     *
     * @param argb The color in ARGB format
     * @return The hue (0-360)
     */
    public static double rgbToHue(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;

        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double delta = max - min;

        double hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == rf) {
            hue = 60 * (((gf - bf) / delta) % 6);
        } else if (max == gf) {
            hue = 60 * (((bf - rf) / delta) + 2);
        } else {
            hue = 60 * (((rf - gf) / delta) + 4);
        }

        return hue < 0 ? hue + 360 : hue;
    }

    /**
     * Converts RGB color to saturation and lightness.
     *
     * @param argb The color in ARGB format
     * @return Array with [saturation (0-100), lightness (0-100)]
     */
    public static double[] rgbToSaturationLightness(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;

        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double lightness = (max + min) / 2;

        double saturation;
        if (max == min) {
            saturation = 0;
        } else {
            double delta = max - min;
            saturation = delta / (1 - Math.abs(2 * lightness - 1));
        }

        return new double[]{saturation * 100, lightness * 100};
    }

    @Override
    public boolean mouseClicked(Click mouseInput, boolean doubled) {
        double mouseX = mouseInput.x();
        double mouseY = mouseInput.y();
        int button = mouseInput.button();
        if (button != 0) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;

        dragging = true;
        updateColorFromMousePosition(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseDragged(Click mouseInput, double deltaX, double deltaY) {
        if (!dragging || mouseInput.button() != 0) return false;
        updateColorFromMousePosition(mouseInput.x(), mouseInput.y());
        return true;
    }

    @Override
    public boolean mouseReleased(Click mouseInput) {
        if (mouseInput.button() != 0) return false;
        dragging = false;
        return true;
    }

    /**
     * Updates the color based on mouse position within the color wheel.
     */
    private void updateColorFromMousePosition(double mouseX, double mouseY) {
        int centerX = getX() + getWidth() / 2;
        int centerY = getY() + getHeight() / 2;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;

        double distance = Math.sqrt(dx * dx + dy * dy);
        double radius = WHEEL_SIZE / 2.0 - INNER_RADIUS_OFFSET / 2.0;

        if (distance < INNER_RADIUS_OFFSET / 2.0 || distance > radius) {
            return;
        }

        double angle = Math.atan2(dy, dx);
        double hue = Math.toDegrees(angle) + 90;
        if (hue < 0) hue += 360;

        updateColorFromHSL(hue);
        if (moduleCallback != null) moduleCallback.accept(this.getRawState());
        if (stateCallback != null) stateCallback.accept(this.getRawState());
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        int centerX = left + (right - left) / 2;
        int centerY = top + (bottom - top) / 2;
        int radius = WHEEL_SIZE / 2;

        // Draw color wheel using simple angular segments
        for (int angle = 0; angle < 360; angle += 2) {
            int nextAngle = angle + 2;

            double rad1 = Math.toRadians(angle - 90);
            double rad2 = Math.toRadians(nextAngle - 90);

            int x1Inner = (int) (centerX + (INNER_RADIUS_OFFSET / 2.0) * Math.cos(rad1));
            int y1Inner = (int) (centerY + (INNER_RADIUS_OFFSET / 2.0) * Math.sin(rad1));
            int x1Outer = (int) (centerX + radius * Math.cos(rad1));
            int y1Outer = (int) (centerY + radius * Math.sin(rad1));

            int x2Inner = (int) (centerX + (INNER_RADIUS_OFFSET / 2.0) * Math.cos(rad2));
            int y2Inner = (int) (centerY + (INNER_RADIUS_OFFSET / 2.0) * Math.sin(rad2));
            int x2Outer = (int) (centerX + radius * Math.cos(rad2));
            int y2Outer = (int) (centerY + radius * Math.sin(rad2));

            int color = hslToRgb(angle, saturation, lightness);

            // Draw line segments instead of filled triangles for better performance
            drawLine(context, x1Inner, y1Inner, x2Inner, y2Inner, color);
            drawLine(context, x1Inner, y1Inner, x1Outer, y1Outer, color);
            drawLine(context, x1Outer, y1Outer, x2Outer, y2Outer, color);
        }

        // Draw center circle showing current color
        int currentColor = this.getRawState();
        context.fill(centerX - 8, centerY - 8, centerX + 8, centerY + 8, currentColor);
        context.fill(centerX - 7, centerY - 7, centerX + 7, centerY + 7, CigaretteScreen.BACKGROUND_COLOR);
        context.fill(centerX - 6, centerY - 6, centerX + 6, centerY + 6, currentColor);

        // Draw hue indicator
        double hue = getHue();
        double hueRad = Math.toRadians(hue - 90);
        int hueX = (int) (centerX + (radius - 4) * Math.cos(hueRad));
        int hueY = (int) (centerY + (radius - 4) * Math.sin(hueRad));
        context.drawHorizontalLine(hueX - 4, hueX + 4, hueY, 0xFFFFFFFF);
        context.drawVerticalLine(hueX, hueY - 4, hueY + 4, 0xFFFFFFFF);
    }

    /**
     * Draws a line between two points using Bresenham's algorithm.
     */
    private void drawLine(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int x = x0;
        int y = y0;

        while (true) {
            context.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
