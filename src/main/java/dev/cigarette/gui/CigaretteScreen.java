package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.ColorScheme;
import dev.cigarette.gui.GradientRenderer;
import dev.cigarette.gui.hud.notification.NotificationDisplay;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import dev.cigarette.gui.widget.ToggleKeybindWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
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
    private static final double OPEN_DURATION_S = 0.2;
    private static final double OPEN_STAGGER_S = 0.03;
    private static final int OPEN_DISTANCE_PX = 24;

    /**
     * The length of the closing animation as a multiplier of {@link #OPEN_DURATION_S}.
     */
    private static final double CLOSE_DURATION_FACTOR = 0.8;
    private static final double CLOSE_STAGGER_FACTOR = 0.8;
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
     * Replaces the built-in {@link Screen#render(DrawContext, int, int, float) Screen.render()} method. Handles animations and category rendering for the GUI.
     *
     * @param context    The current draw context
     * @param mouseX     Current mouse X position
     * @param mouseY     Current mouse Y position
     * @param deltaTicks Current delta ticks
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
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
                context.getMatrices().translate(0.0f, 0.0f, new Matrix3x2f().translation(0.0f, (float) (priority.size() - i)));

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
                        context.getMatrices().translate((float) dx, 0.0f);
                }

                context.getMatrices().scale((float) eased, (float) eased);
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
            context.getMatrices().translate(0.0f, 0.0f, new Matrix3x2f().translation(0.0f, (float) (priority.size() - i)));
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
                        context.getMatrices().translate((float) dx, 0.0f);
                }

                context.getMatrices().scale((float) eased, (float) eased);
            }
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
}