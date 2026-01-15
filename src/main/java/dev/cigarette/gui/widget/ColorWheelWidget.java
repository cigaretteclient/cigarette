package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.lib.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A widget that displays three concentric rings for adjusting hue, saturation, and lightness.
 * Uses efficient texture-based rendering for smooth performance.
 */
public class ColorWheelWidget extends BaseWidget<BaseWidget.Stateless> {
    /**
     * The hue value (0-360).
     */
    private double hue = 0;
    /**
     * The saturation value (0-100).
     */
    private double saturation = 100;
    /**
     * The lightness value (0-100).
     */
    private double lightness = 50;
    /**
     * Callback triggered when the color changes.
     */
    private @Nullable Consumer<Integer> colorCallback = null;
    /**
     * Which ring is being dragged: 0=hue, 1=saturation, 2=lightness, -1=none.
     */
    private int draggingRing = -1;
    /**
     * Whether the widget is disabled.
     */
    public boolean disabled = false;

    /**
     * Cached texture for the color wheel rings.
     */
    private Identifier ringTextureId = null;
    private NativeImageBackedTexture ringTexture = null;
    private boolean textureDirty = true;
    private int lastHue = -1;
    private int lastSaturation = -1;
    private int lastLightness = -1;
    private int lastWidth = -1;
    private int lastHeight = -1;

    /**
     * The radii for the hue ring and dimensions for saturation/lightness bars.
     */
    private int hueInnerRadius;
    private int hueOuterRadius;
    // Bar dimensions (positioned inside hue ring)
    private int barWidth;
    private int barHeight;
    private int satBarY;
    private int lightBarY;

    public Text getMessage() {
        return message;
    }

    /**
     * Sets the HSL values.
     */
    public void setHSL(double h, double s, double l) {
        this.hue = Math.max(0, Math.min(360, h));
        this.saturation = Math.max(0, Math.min(100, s));
        this.lightness = Math.max(0, Math.min(100, l));
        markTextureDirty();
        if (colorCallback != null) {
            colorCallback.accept(Color.hslToRgb(hue, saturation, lightness));
        }
    }

    /**
     * Sets the callback for color changes.
     */
    public void setColorCallback(Consumer<Integer> callback) {
        this.colorCallback = callback;
    }

    /**
     * Creates a color wheel widget.
     */
    public ColorWheelWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        // Reasonable default size for usability
        this.captureHover().withWH(160, 160).withDefault(new BaseWidget.Stateless());
        updateRadii();
    }

    /**
     * Creates a color wheel widget.
     */
    public ColorWheelWidget(String message) {
        this(message, null);
    }

    /**
     * Updates the ring radii and bar dimensions based on current widget size.
     */
    private void updateRadii() {
        int size = Math.min(getWidth(), getHeight());
        int centerRadius = size / 2;

        // Hue ring dimensions
        hueOuterRadius = (int) (centerRadius * 0.95); // Almost to edge
        hueInnerRadius = (int) (centerRadius * 0.65);
        
        // Bar dimensions (inside the hue ring)
        barWidth = (int) (hueInnerRadius * 1.6); // 80% of inner circle diameter
        barHeight = 16;
        
        // Bar Y positions - centered on the wheel center with spacing between them
        int totalBarsHeight = barHeight * 2 + 10; // 10px spacing between bars
        satBarY = (size - totalBarsHeight) / 2; // Centered in widget
        lightBarY = satBarY + barHeight + 10;
    }

    /**
     * Marks the texture as dirty when HSL values change.
     */
    private void markTextureDirty() {
        textureDirty = true;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateRadii();
        markTextureDirty();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        updateRadii();
        markTextureDirty();
    }

    @Override
    public void setDimensions(int width, int height) {
        super.setDimensions(width, height);
        updateRadii();
        markTextureDirty();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (disabled) return false;
        if (!isMouseOver(click.x(), click.y())) return false;
        draggingRing = getRingAt(click.x(), click.y());
        if (draggingRing != -1) {
            updateValueFromMouse(click.x(), click.y());
            this.setFocused();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (disabled || draggingRing == -1) return false;
        updateValueFromMouse(click.x(), click.y());
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (disabled) return false;
        draggingRing = -1;
        return false;
    }

    private int getRingAt(double mouseX, double mouseY) {
        // Use widget position to calculate center with floating point precision
        double centerX = getX() + getWidth() / 2.0;
        double centerY = getY() + getHeight() / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Check hue ring
        if (distance >= hueInnerRadius && distance <= hueOuterRadius) return 0;
        
        // Check saturation bar
        double barLeft = centerX - barWidth / 2.0;
        double barRight = centerX + barWidth / 2.0;
        double satBarTop = getY() + satBarY;
        double satBarBottom = satBarTop + barHeight;
        if (mouseX >= barLeft && mouseX <= barRight && mouseY >= satBarTop && mouseY <= satBarBottom) {
            return 1; // saturation
        }
        
        // Check lightness bar
        double lightBarTop = getY() + lightBarY;
        double lightBarBottom = lightBarTop + barHeight;
        if (mouseX >= barLeft && mouseX <= barRight && mouseY >= lightBarTop && mouseY <= lightBarBottom) {
            return 2; // lightness
        }
        
        return -1;
    }

    private void updateValueFromMouse(double mouseX, double mouseY) {
        double centerX = getX() + getWidth() / 2.0;
        double centerY = getY() + getHeight() / 2.0;
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        
        switch (draggingRing) {
            case 0: // hue - angle determines hue
                double angle = Math.toDegrees(Math.atan2(dx, -dy));
                angle = (angle + 360) % 360;
                setHSL(angle, saturation, lightness);
                break;
            case 1: // saturation - horizontal position on bar (0-100%)
                double barLeft = centerX - barWidth / 2.0;
                double barRight = centerX + barWidth / 2.0;
                double satPos = Math.max(barLeft, Math.min(mouseX, barRight));
                double satPercent = ((satPos - barLeft) / barWidth) * 100.0;
                setHSL(hue, satPercent, lightness);
                break;
            case 2: // lightness - horizontal position on bar (0-100%)
                barLeft = centerX - barWidth / 2.0;
                barRight = centerX + barWidth / 2.0;
                double lightPos = Math.max(barLeft, Math.min(mouseX, barRight));
                double lightPercent = ((lightPos - barLeft) / barWidth) * 100.0;
                setHSL(hue, saturation, lightPercent);
                break;
        }
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        // Update radii if needed
        updateRadii();

        // Draw background
        context.fill(left, top, right, bottom, hovered ? CigaretteScreen.BACKGROUND_COLOR : CigaretteScreen.DARK_BACKGROUND_COLOR);

        // Check if texture needs updating
        if (textureDirty || ringTexture == null ||
            lastHue != (int)hue || lastSaturation != (int)saturation || lastLightness != (int)lightness ||
            lastWidth != getWidth() || lastHeight != getHeight()) {
            updateRingTexture();
        }

        // Draw the texture
        if (ringTextureId != null) {
            context.drawTexture(RenderPipelines.GUI_TEXTURED, ringTextureId, left, top, 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        }

        // Calculate center based on render coordinates (not widget position)
        int centerX = left + getWidth() / 2;
        int centerY = top + getHeight() / 2;

        // Draw hue ring marker
        drawMarker(context, centerX, centerY, hue, hueOuterRadius, CigaretteScreen.PRIMARY_TEXT_COLOR);
        
        // Draw saturation and lightness bars with gradients
        int barLeft = centerX - barWidth / 2;
        int satTop = top + satBarY;
        int lightTop = top + lightBarY;
        
        // Draw saturation bar gradient and marker
        drawBarGradient(context, barLeft, satTop, barWidth, barHeight, true);
        int satMarkerX = barLeft + (int)((saturation / 100.0) * barWidth);
        context.fill(satMarkerX - 2, satTop - 2, satMarkerX + 2, satTop + barHeight + 2, 0xFF000000);
        context.fill(satMarkerX - 1, satTop - 1, satMarkerX + 1, satTop + barHeight + 1, CigaretteScreen.PRIMARY_TEXT_COLOR);
        
        // Draw lightness bar gradient and marker
        drawBarGradient(context, barLeft, lightTop, barWidth, barHeight, false);
        int lightMarkerX = barLeft + (int)((lightness / 100.0) * barWidth);
        context.fill(lightMarkerX - 2, lightTop - 2, lightMarkerX + 2, lightTop + barHeight + 2, 0xFF000000);
        context.fill(lightMarkerX - 1, lightTop - 1, lightMarkerX + 1, lightTop + barHeight + 1, 0xFFFFFFFF);
    }
    
    /**
     * Draws a gradient bar for saturation or lightness.
     */
    private void drawBarGradient(DrawContext context, int x, int y, int width, int height, boolean isSaturation) {
        // Draw gradient by drawing vertical lines with interpolated colors
        for (int i = 0; i < width; i++) {
            double percent = (i / (double) width) * 100.0;
            int color;
            if (isSaturation) {
                color = Color.hslToRgb(hue, percent, 50);
            } else {
                color = Color.hslToRgb(hue, saturation, percent);
            }
            context.fill(x + i, y, x + i + 1, y + height, 0xFF000000 | color);
        }
    }

    /**
     * Updates the cached texture with the current ring colors.
     */
    private void updateRingTexture() {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        // Create or recreate texture
        if (ringTexture != null) {
            ringTexture.close();
        }
        NativeImage image = new NativeImage(width, height, true);
        // Initialize with fully transparent pixels
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setColor(x, y, 0x00000000);
            }
        }
        
        ringTexture = new NativeImageBackedTexture(() -> "color_wheel_" + System.currentTimeMillis(), image);

        // Register with texture manager
        ringTextureId = Identifier.of("cigarette", "color_wheel_" + System.currentTimeMillis());
        MinecraftClient.getInstance().getTextureManager().registerTexture(ringTextureId, ringTexture);

        int centerX = width / 2;
        int centerY = height / 2;

        // Render only the hue ring to the texture (bars are drawn dynamically)
        renderHueRingToTexture(image, centerX, centerY);

        // Upload texture to GPU
        ringTexture.upload();

        // Update cache values
        textureDirty = false;
        lastHue = (int) hue;
        lastSaturation = (int) saturation;
        lastLightness = (int) lightness;
        lastWidth = width;
        lastHeight = height;
    }

    /**
     * Swaps red and blue channels for NativeImage compatibility.
     */
    private int swapRedBlue(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (b << 16) | (g << 8) | r;
    }

    /**
     * Renders the hue ring to the texture.
     */
    private void renderHueRingToTexture(NativeImage image, int centerX, int centerY) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int dx = x - centerX;
                int dy = y - centerY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance >= hueInnerRadius && distance <= hueOuterRadius) {
                    // Calculate angle: start at top (12 o'clock), go clockwise
                    double angle = Math.toDegrees(Math.atan2(dx, -dy));
                    // Normalize angle to [0, 360)
                    angle = (angle + 360) % 360;
                    int color = Color.hslToRgb(angle, 100, 50);
                    image.setColor(x, y, swapRedBlue(color) | 0xFF000000); // Add alpha
                }
            }
        }
    }

    /**
     * Draws a marker at the specified angle and radius.
     */
    private void drawMarker(DrawContext context, int centerX, int centerY, double angleDegrees, int radius, int color) {
        // Convert angle to position: 0° is at top (12 o'clock), going clockwise
        // In screen coordinates, Y increases downward
        // For angle 0° (top): x = center, y = center - radius
        // For angle 90° (right): x = center + radius, y = center
        // Use: x = sin(angle), y = -cos(angle) to get clockwise from top
        double rad = Math.toRadians(angleDegrees);
        int x = centerX + (int) (radius * Math.sin(rad));
        int y = centerY - (int) (radius * Math.cos(rad));

        // Draw a small square marker with border
        int markerSize = 3;

        // Black border
        context.fill(x - markerSize - 1, y - markerSize - 1, x + markerSize + 1, y + markerSize + 1, 0xFF000000);
        // Colored center
        context.fill(x - markerSize, y - markerSize, x + markerSize, y + markerSize, color);
    }

    /**
     * Cleans up the texture resources.
     */
    public void cleanup() {
        if (ringTexture != null) {
            ringTexture.close();
            ringTexture = null;
        }
        ringTextureId = null;
    }
}