package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.notification.NotificationDisplay;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import dev.cigarette.gui.widget.ToggleKeybindWidget;
import dev.cigarette.module.ui.GUI;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.util.Stack;

public class CigaretteScreen extends Screen {
    /**
     * Primary feature color. (Orange)
     */
    public static final int PRIMARY_COLOR = 0xFFFE5F00;
    /**
     * Secondary feature color. (Brown)
     */
    public static final int SECONDARY_COLOR = 0xFFC44700;
    /**
     * Primary text color. (White)
     */
    public static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    /**
     * Primary background color. (Dark Gray)
     */
    public static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    /**
     * Dark background color usually used in gradients with {@link #BACKGROUND_COLOR}. (Black)
     */
    public static final int DARK_BACKGROUND_COLOR = 0xFF000000;
    /**
     * Color of enabled things. (Bright Green)
     */
    public static final int ENABLED_COLOR = 0xFF3AFC3A;
    /**
     * Reference to the hovered widget.
     */
    public static @Nullable Object hoverHandled = null;
    /**
     * An ordered list of the widgets on the screen. Ordered by time of focus descending. Event propagation starts with the most recent focused to the last focused.
     */
    private final Stack<BaseWidget<?>> priority = new Stack<>();
    /**
     * The screen that was being rendered before this GUI was opened.
     */
    private Screen parent = null;
    /**
     * Whether the GUI is in process of opening.
     */
    private boolean begin = false;
    /**
     * The time at which the GUI was opened.
     */
    private long openStartNanos = 0L;
    /**
     * Whether the GUI is in process of closing.
     */
    private boolean closing = false;
    /**
     * The time at which the GUI was closed.
     */
    private long closeStartNanos = 0L;
    /**
     * The length of the opening animation in seconds.
     */
    private static final double OPEN_DURATION_S = 0.4;
    private static final double OPEN_STAGGER_S = 0.06;
    private static final int OPEN_DISTANCE_PX = 24;

    /**
     * The length of the closing animation as a multiplier of {@link #OPEN_DURATION_S}.
     */
    private static final double CLOSE_DURATION_FACTOR = 0.6;
    private static final double CLOSE_STAGGER_FACTOR = 0.6;
    /**
     * The total number of categories in the GUI.
     */
    private int categoryCount = 0;
    /**
     * Reference to a {@link KeybindWidget} or {@link ToggleKeybindWidget} that is actively listening for keys to bind.
     */
    public static @Nullable KeybindWidget bindingKey = null;

    public CigaretteScreen() {
        super(Text.literal("Cigarette Client"));
    }

    /**
     * Called when the GUI is being opened to set the previous screen.
     *
     * @param parent The parent screen that will be reverted to on close
     */
    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearChildren();
        this.priority.clear();
        int idx = 0;
        for (CategoryInstance categoryInstance : Cigarette.CONFIG.CATEGORIES.values()) {
            if (categoryInstance == null)
                continue;
            addDrawableChild(categoryInstance.widget);
            if (categoryInstance.widget instanceof ScrollableWidget<?> sw) {
                sw.setCategoryOffsetIndex(idx);
                sw.expanded = categoryInstance.expanded;
            }
            this.priority.addFirst(categoryInstance.widget);
            categoryInstance.widget.unfocus();
            categoryInstance.widget.setFocused();
            idx++;
        }
        this.categoryCount = idx;
        this.begin = true;
        this.openStartNanos = System.nanoTime();
    }

    Element getParent() {
        return this.parent;
    }

    @Override
    public boolean mouseClicked(Click mouseInput, boolean doubled) {
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseClicked(mouseInput, doubled);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click mouseInput, double deltaX, double deltaY) {
        double mouseX = mouseInput.x();
        double mouseY = mouseInput.y();
        double offsetX = mouseX - deltaX;
        double offsetY = mouseY - deltaY;
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseDragged(mouseInput, offsetX, offsetY);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click mouseInput) {
        for (BaseWidget<?> child : priority) {
            if (child.mouseReleased(mouseInput)) {
                priority.remove(child);
                return true;
            }
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (Element child : this.children()) {
            child.mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * Called when the GUI should be closed.
     */
    @Override
    public void close() {
        assert client != null;

        if (!closing) {
            this.begin = false;
            this.closing = true;
            this.closeStartNanos = System.nanoTime();
        }
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (CigaretteScreen.bindingKey != null) {
            CigaretteScreen.bindingKey.keyPressed(keyInput);
            return true;
        }
        switch (keyInput.getKeycode()) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    /**
     * {@return whether the provided widget can be hovered} If so, that widget is set as the hovered widget.
     * <p>A widget must call {@link BaseWidget#captureHover()} captureHover()} to be hoverable.</p>
     *
     * @param obj The widget to check if it can be hovered
     */
    public static boolean isHoverable(Object obj) {
        if (hoverHandled == null) {
            hoverHandled = obj;
            return true;
        }
        return hoverHandled == obj;
    }

    /**
     * Renders the background and border for a widget with gradient coloring.
     *
     * @param context The draw context
     * @param widget The widget to render background for
     */
    private static void renderWidgetBackgroundAndBorder(DrawContext context, BaseWidget<?> widget) {
        GUI gui = GUI.INSTANCE;
        
        int left = widget.getX();
        int top = widget.getY();
        int right = widget.getX() + widget.getWidth();
        int bottom = widget.getY() + widget.getHeight();
        
        if (left < 0 || top < 0 || right < 0 || bottom < 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();

        // Calculate gradient position
        double widgetCenterX = left + (widget.getWidth() / 2.0);
        double normalizedPos = scrW > 0 ? widgetCenterX / scrW : 0.5;
        
        // Get gradient colors for left and right edges
        int colorLeft = gui.getGradientColor(normalizedPos - 0.1);
        int colorRight = gui.getGradientColor(normalizedPos + 0.1);
        
        // Draw gradient background with rounded corners
        int radius = 4;
        drawGradientRoundedRect(context, left, top, right, bottom, radius, colorLeft, colorRight);
        
        // Draw bright saturated border
        int borderColor = gui.getGradientColor(normalizedPos);
        // Increase saturation and brightness of border
        double hue = dev.cigarette.gui.widget.ColorWheelWidget.rgbToHue(borderColor);
        int brightBorder = dev.cigarette.gui.widget.ColorWheelWidget.hslToRgb(hue, 100, 60);
        
        drawRoundedBorder(context, left, top, right, bottom, radius, 2, brightBorder);
        
        // For ScrollableWidget (category headers), add shadow
        if (widget instanceof ScrollableWidget<?>) {
            drawRoundedShadow(context, left, top, right, bottom, radius, 4);
        }
    }

    /**
     * Replaces the built-in {@link Screen#render(DrawContext, int, int, float) Screen.render()} method. Handles animations and category rendering for the GUI.
     *
     * @param context    The current draw context
     * @param mouseX     Current mouse X position
     * @param mouseY     Current mouse Y position
     * @param deltaTicks Current delta ticks
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);

        MinecraftClient mc = MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();
        int scrH = mc.getWindow().getScaledHeight();

        NotificationDisplay.imageRender(context, scrW - 60, scrH - 70, 0.8);

        CigaretteScreen.hoverHandled = null;

        if (closing) {
            double elapsedClose = (System.nanoTime() - closeStartNanos) / 1_000_000_000.0;

            double closeDuration = OPEN_DURATION_S * CLOSE_DURATION_FACTOR;
            double closeStagger = OPEN_STAGGER_S * CLOSE_STAGGER_FACTOR;
            double totalAnim = (Math.max(0, categoryCount - 1)) * closeStagger + closeDuration;
            boolean animActive = elapsedClose < totalAnim;
            double remaining = Math.max(0.0, Math.min(totalAnim, totalAnim - elapsedClose));

            context.getMatrices().pushMatrix();
            for (int i = 0; i < priority.size(); i++) {
                BaseWidget<?> widget = priority.get(i);

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(0.0f, 0.0f, new Matrix3x2f().setTranslation(0.0f, 0.0f));

                double normalizedPos = 0.0;
                try {
                    double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                    double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                    double nx = scrW > 0 ? widgetCenterX / (double) scrW : 0.0;
                    double ny = scrH > 0 ? widgetCenterY / (double) scrH : 0.0;
                    normalizedPos = Math.max(0.0, Math.min(1.0, (nx + ny) * 0.5));
                } catch (Exception ignore) {
                    normalizedPos = Math.max(0.0, Math.min(1.0, i / (double) Math.max(1, priority.size())));
                }

                double totalStagger = Math.max(0, categoryCount - 1) * closeStagger;
                double startDelay = normalizedPos * totalStagger;

                double t = Math.max(0.0, Math.min(1.0, (remaining - startDelay) / closeDuration));

                double eased = easeIn(t);

                try {
                    double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                    double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                    double nx = 0.0;
                    double ny = 0.0;
                    if (scrW > 0)
                        nx = (widgetCenterX - (scrW / 2.0)) / (scrW / 2.0);
                    if (scrH > 0)
                        ny = (widgetCenterY - (scrH / 2.0)) / (scrH / 2.0);
                    nx = Math.max(-1.0, Math.min(1.0, nx));
                    ny = Math.max(-1.0, Math.min(1.0, ny));

                    double magnitude = (1.0 - eased) * OPEN_DISTANCE_PX;
                    double dx = magnitude * nx;
                    double dy = magnitude * ny;
                    if (Math.abs(dx) > 0.01 || Math.abs(dy) > 0.01) {
                        context.getMatrices().translate((float) dx, (float) dy);
                    }
                } catch (Exception ignore) {
                    double dx = (1.0 - eased) * OPEN_DISTANCE_PX;
                    if (dx > 0.01)
                        context.getMatrices().translate((float) dx, 0);
                }

                context.getMatrices().scale((float) eased, (float) eased);
                
                // Render gradient background and border
                renderWidgetBackgroundAndBorder(context, widget);
                
                widget._render(context, mouseX, mouseY, deltaTicks);
                context.getMatrices().popMatrix();
            }
            context.getMatrices().popMatrix();

            if (!animActive) {
                this.closing = false;
                assert client != null;
                client.setScreen(parent);
            }
            return;
        }
        boolean animActive = false;
        double elapsed = 0.0;
        if (begin) {
            elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000.0;
            double totalAnim = (Math.max(0, categoryCount - 1)) * OPEN_STAGGER_S + OPEN_DURATION_S;
            animActive = elapsed < totalAnim;
        }
        for (int i = 0; i < priority.size(); i++) {
            BaseWidget<?> widget = priority.get(i);
            if (widget instanceof ScrollableWidget<?> sw) {
                for (CategoryInstance categoryInstance : Cigarette.CONFIG.CATEGORIES.values()) {
                    if (categoryInstance.widget == sw) {
                        sw.expanded = categoryInstance.expanded;
                        break;
                    }
                }
            }
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0f, 0.0f, new Matrix3x2f().setTranslation(0.0f, 0.0f));
            if (begin && animActive) {
                double totalStagger = Math.max(0, categoryCount - 1) * OPEN_STAGGER_S;

                double normalizedPos = 0.0;
                try {
                    double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                    double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                    double nx = scrW > 0 ? widgetCenterX / (double) scrW : 0.0;
                    double ny = scrH > 0 ? widgetCenterY / (double) scrH : 0.0;
                    normalizedPos = Math.max(0.0, Math.min(1.0, (nx + ny) * 0.5));
                } catch (Exception ignore) {
                    normalizedPos = Math.max(0.0, Math.min(1.0, i / (double) Math.max(1, priority.size())));
                }

                double startDelay = normalizedPos * totalStagger;
                double t = Math.max(0.0, Math.min(1.0, (elapsed - startDelay) / OPEN_DURATION_S));
                double eased = easeOut(t);

                try {
                    double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                    double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                    double nx = 0.0;
                    double ny = 0.0;
                    if (scrW > 0)
                        nx = (widgetCenterX - (scrW / 2.0)) / (scrW / 2.0);
                    if (scrH > 0)
                        ny = (widgetCenterY - (scrH / 2.0)) / (scrH / 2.0);
                    nx = Math.max(-1.0, Math.min(1.0, nx));
                    ny = Math.max(-1.0, Math.min(1.0, ny));

                    double magnitude = (1.0 - eased) * OPEN_DISTANCE_PX;
                    double dx = magnitude * nx;
                    double dy = magnitude * ny;
                    if (Math.abs(dx) > 0.01 || Math.abs(dy) > 0.01) {
                        context.getMatrices().translate((float) dx, (float) dy);
                    }
                } catch (Exception ignore) {
                    double dx = (1.0 - eased) * OPEN_DISTANCE_PX;
                    if (dx > 0.01)
                        context.getMatrices().translate((float) dx, 0);
                }

                context.getMatrices().scale((float) eased, (float) eased);
            }
            
            // Render gradient background and border
            renderWidgetBackgroundAndBorder(context, widget);
            
            widget._render(context, mouseX, mouseY, deltaTicks);
            context.getMatrices().popMatrix();
        }
        if (begin && !animActive)
            begin = false;

    }

    public static double easeOutExpo(double t) {
        if (t >= 1.0)
            return 1.0;
        if (t <= 0.0)
            return 0.0;
        return 1.0 - Math.pow(2.0, -10.0 * t);
    }

    public static double easeOutExpoBack(double t) {
        double s = 1.20158;
        return 1.0 + (t -= 1.0) * t * ((s + 1.0) * t + s);
    }

    public static double easeOutElastic(double t) {
        if (t >= 1.0)
            return 1.0;
        if (t <= 0.0)
            return 0.0;
        double p = 0.3;
        double s = p / 4.0;
        return 1.0 - Math.pow(2.0, -10.0 * t) * Math.sin((t - s) * (2.0 * Math.PI) / p);
    }

    public static double easeOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }

    public static double easeIn(double t) {
        return Math.pow(t, 3);
    }

    public static double easeInExpo(double t) {
        if (t >= 1.0)
            return 1.0;
        if (t <= 0.0)
            return 0.0;
        return Math.pow(2.0, 10.0 * (t - 1.0));
    }

    /**
     * Gets the gradient color for a widget based on its position on screen.
     *
     * @param widget The widget to get the gradient color for
     * @param isHeader Whether this is a header element (lighter)
     * @return The gradient color in ARGB format
     */
    public static int getGradientColorForWidget(BaseWidget<?> widget, boolean isHeader) {
        GUI gui = GUI.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();
        
        double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
        double normalizedPos = scrW > 0 ? widgetCenterX / scrW : 0.5;
        
        int baseColor = gui.getGradientColor(normalizedPos);
        
        if (isHeader) {
            // Make header lighter by adjusting lightness
            double hue = dev.cigarette.gui.widget.ColorWheelWidget.rgbToHue(baseColor);
            double[] sl = dev.cigarette.gui.widget.ColorWheelWidget.rgbToSaturationLightness(baseColor);
            double lighterLightness = Math.min(100, sl[1] + 20);
            return dev.cigarette.gui.widget.ColorWheelWidget.hslToRgb(hue, sl[0], lighterLightness);
        }
        
        return baseColor;
    }

    /**
     * Renders a rounded rectangle with anti-aliasing.
     *
     * @param context The draw context
     * @param left The left edge
     * @param top The top edge
     * @param right The right edge
     * @param bottom The bottom edge
     * @param radius The corner radius
     * @param color The fill color
     */
    public static void drawRoundedRect(DrawContext context, int left, int top, int right, int bottom, int radius, int color) {
        int width = right - left;
        int height = bottom - top;
        radius = Math.min(radius, Math.min(width, height) / 2);

        if (radius <= 0) {
            context.fill(left, top, right, bottom, color);
            return;
        }

        // Fill center area
        context.fill(left + radius, top, right - radius, bottom, color);
        context.fill(left, top + radius, right, bottom - radius, color);

        // Draw corners with smooth curves
        drawQuadrant(context, left + radius, top + radius, radius, color, 0); // Top-left
        drawQuadrant(context, right - radius, top + radius, radius, color, 1); // Top-right
        drawQuadrant(context, left + radius, bottom - radius, radius, color, 2); // Bottom-left
        drawQuadrant(context, right - radius, bottom - radius, radius, color, 3); // Bottom-right
    }

    /**
     * Draws a quadrant of a circle for rounded corners.
     *
     * @param context The draw context
     * @param centerX Center X of the quadrant
     * @param centerY Center Y of the quadrant
     * @param radius Radius of the circle
     * @param color Color to draw
     * @param quadrant 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
     */
    private static void drawQuadrant(DrawContext context, int centerX, int centerY, int radius, int color, int quadrant) {
        for (int i = 0; i <= radius; i++) {
            double angle = Math.acos((double) i / radius);
            int offset = (int) Math.round(radius * Math.sin(angle));

            if (quadrant == 0 || quadrant == 2) {
                context.fill(centerX - radius, centerY + (quadrant == 2 ? offset : -offset), centerX - i, centerY + (quadrant == 2 ? radius : -radius), color);
            } else {
                context.fill(centerX + i, centerY + (quadrant == 3 ? offset : -offset), centerX + radius, centerY + (quadrant == 3 ? radius : -radius), color);
            }
        }
    }

    /**
     * Renders a rounded rectangle border with anti-aliasing.
     *
     * @param context The draw context
     * @param left The left edge
     * @param top The top edge
     * @param right The right edge
     * @param bottom The bottom edge
     * @param radius The corner radius
     * @param borderWidth The border width
     * @param color The border color
     */
    public static void drawRoundedBorder(DrawContext context, int left, int top, int right, int bottom, int radius, int borderWidth, int color) {
        // Draw horizontal lines
        context.fill(left + radius, top, right - radius, top + borderWidth, color);
        context.fill(left + radius, bottom - borderWidth, right - radius, bottom, color);

        // Draw vertical lines
        context.fill(left, top + radius, left + borderWidth, bottom - radius, color);
        context.fill(right - borderWidth, top + radius, right, bottom - radius, color);

        // Draw corner borders
        drawQuadrantBorder(context, left + radius, top + radius, radius, borderWidth, color, 0);
        drawQuadrantBorder(context, right - radius, top + radius, radius, borderWidth, color, 1);
        drawQuadrantBorder(context, left + radius, bottom - radius, radius, borderWidth, color, 2);
        drawQuadrantBorder(context, right - radius, bottom - radius, radius, borderWidth, color, 3);
    }

    /**
     * Draws a quadrant border for rounded corners.
     */
    private static void drawQuadrantBorder(DrawContext context, int centerX, int centerY, int radius, int borderWidth, int color, int quadrant) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < borderWidth; j++) {
                double angle = Math.acos((double) (i + j) / (radius + borderWidth));
                int offset = (int) Math.round((radius + borderWidth) * Math.sin(angle));

                if (quadrant == 0) {
                    context.fill(centerX - i - j - 1, centerY - offset, centerX - i - j, centerY - offset + 1, color);
                } else if (quadrant == 1) {
                    context.fill(centerX + i + j, centerY - offset, centerX + i + j + 1, centerY - offset + 1, color);
                } else if (quadrant == 2) {
                    context.fill(centerX - i - j - 1, centerY + offset, centerX - i - j, centerY + offset + 1, color);
                } else {
                    context.fill(centerX + i + j, centerY + offset, centerX + i + j + 1, centerY + offset + 1, color);
                }
            }
        }
    }

    /**
     * Renders a shadow under a rounded rectangle.
     *
     * @param context The draw context
     * @param left The left edge
     * @param top The top edge
     * @param right The right edge
     * @param bottom The bottom edge
     * @param radius The corner radius
     * @param shadowSize The size of the shadow
     */
    public static void drawRoundedShadow(DrawContext context, int left, int top, int right, int bottom, int radius, int shadowSize) {
        // Draw shadow with gradient alpha
        for (int i = 0; i < shadowSize; i++) {
            float alpha = (1.0f - (i / (float) shadowSize)) * 0.3f;

            int shadowTop = bottom + i;
            int shadowLeft = left - (shadowSize - i);
            int shadowRight = right + (shadowSize - i);
            int shadowBottom = bottom + i + 1;

            // Simple shadow - just draw underneath
            int finalShadowAlpha = ((int) (alpha * 255)) << 24;
            context.fill(shadowLeft, shadowTop, shadowRight, shadowBottom, finalShadowAlpha | 0x000000);
        }
    }

    /**
     * Renders a gradient-filled rounded rectangle.
     *
     * @param context The draw context
     * @param left The left edge
     * @param top The top edge
     * @param right The right edge
     * @param bottom The bottom edge
     * @param radius The corner radius
     * @param color1 The left gradient color
     * @param color2 The right gradient color
     */
    public static void drawGradientRoundedRect(DrawContext context, int left, int top, int right, int bottom, int radius, int color1, int color2) {
        int width = right - left;
        
        // Draw gradient line by line
        for (int x = 0; x < width; x++) {
            double t = width > 0 ? x / (double) width : 0;
            int color = lerpColorSmooth(color1, color2, t);
            
            // Draw vertical line with rounded corners
            if (x < radius || x >= width - radius) {
                // Near corners, draw with varying heights
                double cornerProgress = x < radius ? x / (double) radius : (width - x) / (double) radius;
                int offset = (int) (radius * (1.0 - Math.sqrt(1.0 - cornerProgress * cornerProgress)));
                context.fill(left + x, top + offset, left + x + 1, bottom - offset, color);
            } else {
                context.fill(left + x, top, left + x + 1, bottom, color);
            }
        }
    }

    /**
     * Linearly interpolates between two colors.
     */
    private static int lerpColorSmooth(int color1, int color2, double t) {
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