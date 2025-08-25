package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.notification.NotificationDisplay;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Stack;

public class CigaretteScreen extends Screen {
    public static final int PRIMARY_COLOR = 0xFFFE5F00;
    public static final int SECONDARY_COLOR = 0xFFC44700;
    public static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    public static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    public static final int DARK_BACKGROUND_COLOR = 0xFF000000;
    public static final int ENABLED_COLOR = 0xFF3AFC3A;
    public static @Nullable Object hoverHandled = null;
    private final Stack<BaseWidget<?>> priority = new Stack<>();
    private Screen parent = null;
    private boolean begin = false;
    private long openStartNanos = 0L;
    private boolean closing = false;
    private long closeStartNanos = 0L;
    private static final double OPEN_DURATION_S = 0.4;
    private static final double OPEN_STAGGER_S = 0.06;
    private static final int OPEN_DISTANCE_PX = 24;

    private static final double CLOSE_DURATION_FACTOR = 0.6;
    private static final double CLOSE_STAGGER_FACTOR = 0.6;
    private int categoryCount = 0;
    public static @Nullable KeybindWidget bindingKey = null;

    protected CigaretteScreen() {
        super(Text.literal("Cigarette Client"));
    }

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseClicked(mouseX, mouseY, button);
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
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        for (Element child : this.children()) {
            child.mouseDragged(x, y, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (BaseWidget<?> child : priority) {
            if (child.mouseReleased(mouseX, mouseY, button)) {
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CigaretteScreen.bindingKey != null) {
            CigaretteScreen.bindingKey.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    public static boolean isHoverable(Object obj) {
        if (hoverHandled == null) {
            hoverHandled = obj;
            return true;
        }
        return hoverHandled == obj;
    }

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

            context.getMatrices().push();
            for (int i = 0; i < priority.size(); i++) {
                BaseWidget<?> widget = priority.get(i);

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, priority.size() - i);

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
                        context.getMatrices().translate((float) dx, (float) dy, 0);
                    }
                } catch (Exception ignore) {
                    double dx = (1.0 - eased) * OPEN_DISTANCE_PX;
                    if (dx > 0.01)
                        context.getMatrices().translate(dx, 0, 0);
                }

                context.getMatrices().scale((float) eased, (float) eased, 1.0f);
                widget._render(context, mouseX, mouseY, deltaTicks);
                context.getMatrices().pop();
            }
            context.getMatrices().pop();

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
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, priority.size() - i);
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
                        context.getMatrices().translate((float) dx, (float) dy, 0);
                    }
                } catch (Exception ignore) {
                    double dx = (1.0 - eased) * OPEN_DISTANCE_PX;
                    if (dx > 0.01)
                        context.getMatrices().translate(dx, 0, 0);
                }

                context.getMatrices().scale((float) eased, (float) eased, 1.0f);
            }
            widget._render(context, mouseX, mouseY, deltaTicks);
            context.getMatrices().pop();
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