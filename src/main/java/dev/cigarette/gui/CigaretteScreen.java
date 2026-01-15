package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.ColorScheme;
import dev.cigarette.gui.GradientRenderer;
import dev.cigarette.gui.hud.notification.NotificationDisplay;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import dev.cigarette.gui.widget.ToggleKeybindWidget;
import dev.cigarette.lib.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.GameRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

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
     * Dark background color usually used in gradients with
     * {@link #BACKGROUND_COLOR}. (Black)
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
     * An ordered list of the widgets on the screen. Ordered by time of focus
     * descending. Event propagation starts with the most recent focused to the last
     * focused.
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
    private static final double OPEN_DURATION_S = 0.2;
    private static final double OPEN_STAGGER_S = 0.03;
    private static final int OPEN_DISTANCE_PX = 24;

    /**
     * The length of the closing animation as a multiplier of
     * {@link #OPEN_DURATION_S}.
     */
    private static final double CLOSE_DURATION_FACTOR = 0.8;
    private static final double CLOSE_STAGGER_FACTOR = 0.8;
    /**
     * The total number of categories in the GUI.
     */
    private int categoryCount = 0;
    /**
     * Reference to a {@link KeybindWidget} or {@link ToggleKeybindWidget} that is
     * actively listening for keys to bind.
     */
    public static @Nullable KeybindWidget bindingKey = null;
    /**
     * Shared buffer builder for batched rendering operations
     */
    private static @Nullable net.minecraft.client.render.BufferBuilder batchedBuffer = null;
    /**
     * Whether we're currently in a batched rendering context
     */
    private static boolean inBatchedContext = false;

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

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseClicked(click, doubled);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
        }
        return false;
    }

    @Override
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
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        for (Element child : this.children()) {
            child.mouseDragged(click, offsetX, offsetY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        for (BaseWidget<?> child : priority) {
            if (child.mouseReleased(click)) {
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
    public boolean keyPressed(KeyInput input) {
        if (CigaretteScreen.bindingKey != null) {
            CigaretteScreen.bindingKey.keyPressed(input);
            return true;
        }
        switch (input.getKeycode()) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    /**
     * {@return whether the provided widget can be hovered} If so, that widget is
     * set as the hovered widget.
     * <p>
     * A widget must call {@link BaseWidget#captureHover()} captureHover()} to be
     * hoverable.
     * </p>
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
<<<<<<< Updated upstream
     * Replaces the built-in {@link Screen#render(DrawContext, int, int, float) Screen.render()} method. Handles animations and category rendering for the GUI.
=======
     * Replaces the built-in {@link Screen#render(DrawContext, int, int, float)
     * Screen.render()} method. Handles animations and category rendering for the
     * GUI.
>>>>>>> Stashed changes
     *
     * @param context    The current draw context
     * @param mouseX     Current mouse X position
     * @param mouseY     Current mouse Y position
     * @param deltaTicks Current delta ticks
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // this.renderBackground(context, mouseX, mouseY, deltaTicks); // Removed to
        // prevent multiple blur applications per frame

        MinecraftClient mc = MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();
        int scrH = mc.getWindow().getScaledHeight();

        // Render animated background wave gradient
        int[] bgGradient = ColorScheme.getBackgroundGradient();
        GradientRenderer.renderHorizontalWaveGradient(
            context,
            0, 0, scrW, scrH,
            bgGradient[0], bgGradient[1],
            ColorScheme.getWaveWavelength(),
            ColorScheme.getWaveSpeed() * 0.5f, // Slower for background
            ColorScheme.getWaveAmplitude() * 0.3f, // Subtle for background
            0.0f
        );

        NotificationDisplay.imageRender(context, scrW - 60, scrH - 70, 0.8);

        CigaretteScreen.hoverHandled = null;
        
        // Determine animation state
        boolean animActive = false;
        double elapsed = 0.0;
        double easeProgress = 1.0;

        if (begin) {
            // Opening animation
            elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000.0;
            double totalAnim = (Math.max(0, categoryCount - 1)) * OPEN_STAGGER_S + OPEN_DURATION_S;
            animActive = elapsed < totalAnim;
            easeProgress = animActive ? 1.0 : 1.0; // Will be computed per-widget
        } else if (closing) {
            // Closing animation
            elapsed = (System.nanoTime() - closeStartNanos) / 1_000_000_000.0;
            double closeDuration = OPEN_DURATION_S * CLOSE_DURATION_FACTOR;
            double closeStagger = OPEN_STAGGER_S * CLOSE_STAGGER_FACTOR;
            double totalAnim = (Math.max(0, categoryCount - 1)) * closeStagger + closeDuration;
            animActive = elapsed < totalAnim;

            if (!animActive) {
                this.closing = false;
                assert client != null;
                client.setScreen(parent);
                return;
            }
        }

        // Render all widgets with proper animation
        context.getMatrices().pushMatrix();
        
        // Render widgets (backgrounds are now rendered inline for better performance)
        for (int i = 0; i < priority.size(); i++) {
            BaseWidget<?> widget = priority.get(i);

            context.getMatrices().pushMatrix();

            // Compute animation progress for this widget
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

            double staggerFactor = begin ? OPEN_STAGGER_S : (OPEN_STAGGER_S * CLOSE_STAGGER_FACTOR);
            double totalStagger = Math.max(0, categoryCount - 1) * staggerFactor;
            double startDelay = normalizedPos * totalStagger;

            double animDuration = begin ? OPEN_DURATION_S : (OPEN_DURATION_S * CLOSE_DURATION_FACTOR);
            double t = Math.max(0.0, Math.min(1.0, (elapsed - startDelay) / animDuration));
            double eased = begin ? easeOut(t) : easeIn(t);

            // Apply transformations only during animation
            if ((begin || closing) && animActive) {
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
                        context.getMatrices().translate((float) dx, 0.0f);
                }

                // Center scaling on widget position
                double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                context.getMatrices().translate((float) widgetCenterX, (float) widgetCenterY);
                context.getMatrices().scale((float) eased, (float) eased);
                context.getMatrices().translate((float) -widgetCenterX, (float) -widgetCenterY);
            }

            // Transform mouse coordinates to account for scaling
            double transformedMouseX = mouseX;
            double transformedMouseY = mouseY;
            if ((begin || closing) && animActive && eased != 1.0) {
                double widgetCenterX = widget.getX() + (widget.getWidth() / 2.0);
                double widgetCenterY = widget.getY() + (widget.getHeight() / 2.0);
                // Reverse the scaling transformation on mouse coordinates
                double scaleFactor = 1.0 / eased;
                transformedMouseX = widgetCenterX + (mouseX - widgetCenterX) * scaleFactor;
                transformedMouseY = widgetCenterY + (mouseY - widgetCenterY) * scaleFactor;
            }

            widget._render(context, (int) transformedMouseX, (int) transformedMouseY, deltaTicks);
            context.getMatrices().popMatrix();
        }
        context.getMatrices().popMatrix();

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

    public static void drawGradientRoundedRect(DrawContext context, int left, int top, int right, int bottom, int radius,
            int colorLeft, int colorRight) {
        if (radius <= 0) {
            context.fillGradient(left, top, right, bottom, colorLeft, colorRight);
            return;
        }
        // Draw the four sides with gradient
        context.fillGradient(left + radius, top, right - radius, top + radius, colorLeft, colorRight); // top
        context.fillGradient(left + radius, bottom - radius, right - radius, bottom, colorLeft, colorRight); // bottom
        // For left and right, since horizontal gradient, left is colorLeft, right is colorRight
        context.fill(left, top + radius, left + radius, bottom - radius, colorLeft); // left
        context.fill(right - radius, top + radius, right, bottom - radius, colorRight); // right
        // Center
        context.fillGradient(left + radius, top + radius, right - radius, bottom - radius, colorLeft, colorRight);
        // Corners
        drawQuarterCircle(context, left + radius, top + radius, radius, colorLeft, 90, 180); // top-left
        drawQuarterCircle(context, right - radius, top + radius, radius, colorRight, 0, 90); // top-right
        drawQuarterCircle(context, left + radius, bottom - radius, radius, colorLeft, 180, 270); // bottom-left
        drawQuarterCircle(context, right - radius, bottom - radius, radius, colorRight, 270, 360); // bottom-right
    }

    public static void drawRoundedRect(DrawContext context, int left, int top, int right, int bottom, int radius,
            int backgroundColor) {
        if (radius <= 0) {
            context.fill(left, top, right, bottom, backgroundColor);
            return;
        }
        // Draw the four sides
        context.fill(left + radius, top, right - radius, top + radius, backgroundColor); // top
        context.fill(left + radius, bottom - radius, right - radius, bottom, backgroundColor); // bottom
        context.fill(left, top + radius, left + radius, bottom - radius, backgroundColor); // left
        context.fill(right - radius, top + radius, right, bottom - radius, backgroundColor); // right
        // Draw the center
        context.fill(left + radius, top + radius, right - radius, bottom - radius, backgroundColor);
        // Draw the corners with circles
        drawQuarterCircle(context, left + radius, top + radius, radius, backgroundColor, 90, 180); // top-left
        drawQuarterCircle(context, right - radius, top + radius, radius, backgroundColor, 0, 90); // top-right
        drawQuarterCircle(context, left + radius, bottom - radius, radius, backgroundColor, 180, 270); // bottom-left
        drawQuarterCircle(context, right - radius, bottom - radius, radius, backgroundColor, 270, 360); // bottom-right
    }

    private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        float a = (float) (color >> 24 & 0xFF) / 255.0f;
        float r = (float) (color >> 16 & 0xFF) / 255.0f;
        float g = (float) (color >> 8 & 0xFF) / 255.0f;
        float b = (float) (color & 0xFF) / 255.0f;
        // Manually render line with Bresenham's algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            context.fill(x1, y1, x1 + 1, y1 + 1, Color.rgba((int)r, (int)g, (int)b, (int)a));
            if (x1 == x2 && y1 == y2) break;
            int err2 = 2 * err;
            if (err2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (err2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private static void drawQuarterCircle(DrawContext context, int centerX, int centerY, int radius, int color, int startAngle, int endAngle) {
        for (int angle = startAngle; angle < endAngle; angle++) {
            double rad = Math.toRadians(angle);
            int x = centerX + (int) (radius * Math.cos(rad));
            int y = centerY + (int) (radius * Math.sin(rad));
            drawLine(context, centerX, centerY, x, y, color);
        }
    }
}