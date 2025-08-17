package io.github.waqfs.gui;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.widget.BaseWidget;
import io.github.waqfs.gui.widget.ScrollableWidget;
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
    private static final double OPEN_DURATION_S = 0.4;
    private static final double OPEN_STAGGER_S = 0.04;
    private static final int OPEN_DISTANCE_PX = 24;
    private int categoryCount = 0;

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
        for (CategoryInstance categoryInstance : Cigarette.CONFIG.allCategories) {
            if (categoryInstance == null)
                continue;
            addDrawableChild(categoryInstance.widget);
            if (categoryInstance.widget instanceof ScrollableWidget<?> sw) {
                sw.setCategoryOffsetIndex(idx);
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
        for (Element child : this.children()) {
            child.mouseReleased(mouseX, mouseY, button);
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
        this.begin = false;
        client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
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

        CigaretteScreen.hoverHandled = null;
        boolean animActive = false;
        double elapsed = 0.0;
        if (begin) {
            elapsed = (System.nanoTime() - openStartNanos) / 1_000_000_000.0;
            double totalAnim = (Math.max(0, categoryCount - 1)) * OPEN_STAGGER_S + OPEN_DURATION_S;
            animActive = elapsed < totalAnim;
        }
        for (int i = 0; i < priority.size(); i++) {
            BaseWidget<?> widget = priority.get(i);
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, priority.size() - i);
            if (begin && animActive) {
                int orderIndex = (widget instanceof ScrollableWidget<?> sw)
                        ? Math.max(0, sw.getCategoryOffsetIndex())
                        : i;
                double startDelay = (orderIndex) * OPEN_STAGGER_S;
                double t = Math.max(0.0, Math.min(1.0, (elapsed - startDelay) / OPEN_DURATION_S));
                double eased = easeOutExpo(t);
                double dx = (1.0 - eased) * OPEN_DISTANCE_PX;
                if (dx > 0.01)
                    context.getMatrices().translate(dx, 0, 0);
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
}
