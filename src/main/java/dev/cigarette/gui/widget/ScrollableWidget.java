package dev.cigarette.gui.widget;

import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.Scissor;
import dev.cigarette.lib.Shape;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;

public class ScrollableWidget<Widgets extends BaseWidget<?>>
        extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    private static final int VERTICAL_SCROLL_MULTIPLIER = 6;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int DEFAULT_SCROLLBAR_WIDTH = 3;
    private static final int BOTTOM_ROUNDED_RECT_HEIGHT = 6;
    private final int rowHeight = 20;
    private boolean shouldScroll = false;
    private double scrollPosition = 0D;
    private @Nullable DraggableWidget header;
    private int categoryOffsetIndex = 0;
    public boolean expanded = true;
    private int ticksOnOpen = 0;
    private static final int MAX_TICKS_ON_OPEN = 20;
    private @Nullable Runnable onToggleExpand;
    private boolean showBottomRoundedRect = true;

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Text headerText, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.setChildren(children).setHeader(headerText, null);
    }

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.setChildren(children);
    }

    public ScrollableWidget(int x, int y) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
    }

    public ScrollableWidget(int x, int y, boolean showBottomRoundedRect) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.showBottomRoundedRect = showBottomRoundedRect;
    }

    private boolean updateShouldScroll() {
        this.shouldScroll = ((children == null ? 0 : children.size()) + (header != null ? 1 : 0))
                * rowHeight > this.height;
        return this.shouldScroll;
    }

    @SafeVarargs
    public final ScrollableWidget<Widgets> setChildren(@Nullable Widgets... children) {
        for (Widgets widget : children) {
            if (widget == null) continue;
            this.children.put(widget.getMessage().toString(), widget);
        }
        return updateChildrenSizing();
    }

    public ScrollableWidget<Widgets> setHeader(@Nullable Text headerText) {
        return this.setHeader(headerText, null);
    }

    public ScrollableWidget<Widgets> setHeader(@Nullable Text headerText, @Nullable Runnable onToggleExpand) {
        this.onToggleExpand = onToggleExpand;
        if (headerText == null) {
            this.header = null;
            return updateChildrenSizing();
        }
        this.header = new DraggableWidget(getX(), getY(), width, rowHeight, headerText);
        this.header.onDrag((newX, newY, deltaX, deltaY) -> {
            setX(newX);
            setY(newY);
        });
        this.header.onClick((mouseX, mouseY, button) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                if (this.onToggleExpand != null) {
                    this.onToggleExpand.run();
                }
                if (!this.expanded) {
                    this.unfocus();
                }
            }
        });
        return updateChildrenSizing();
    }

    private ScrollableWidget<Widgets> updateChildrenSizing() {
        if (this.children != null) {
            int rightMargin = this.updateShouldScroll() ? DEFAULT_SCROLLBAR_WIDTH : 0;
            for (ClickableWidget child : children.values()) {
                if (child == null)
                    continue;
                child.setHeight(rowHeight);
                child.setWidth(width - rightMargin);
                if (this.shouldScroll && child instanceof PassthroughWidget<?, ?> widget) {
                    widget.childLeftOffset = DEFAULT_SCROLLBAR_WIDTH;
                }
            }
        }
        return this;
    }

    public int getCategoryOffsetIndex() {
        return this.categoryOffsetIndex;
    }

    public void setCategoryOffsetIndex(int index) {
        this.categoryOffsetIndex = Math.max(0, index);
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded)
            return;
        this.expanded = expanded;
        this.ticksOnOpen = expanded ? 0 : MAX_TICKS_ON_OPEN;
    }

    @Override
    public void unfocus() {
        if (this.header != null)
            this.header.unfocus();
        this.ticksOnOpen = this.expanded ? MAX_TICKS_ON_OPEN : 0;
        this.setFocused(false);
        super.unfocus();
    }

    private double getEasedProgress() {
        double t = ticksOnOpen / (double) MAX_TICKS_ON_OPEN;
        return this.expanded ? CigaretteScreen.easeOutExpo(t) : CigaretteScreen.easeInExpo(t);
    }

    private int getHeaderHeight() {
        return this.header != null ? this.header.getHeight() : 0;
    }

    private int getVisibleBottom(int top, int bottom) {
        int headerHeight = getHeaderHeight();
        int areaHeight = Math.max(0, bottom - top - headerHeight);
        int contentHeight = (children == null ? 0 : children.size() * rowHeight);
        int maxVisible = Math.min(areaHeight, contentHeight);

        double eased = getEasedProgress();
        int visibleHeight = this.expanded
                ? (int) Math.ceil(eased * maxVisible)
                : (int) Math.floor(eased * maxVisible);

        if (showBottomRoundedRect && !this.expanded && visibleHeight > 0) {
            visibleHeight = Math.max(0, visibleHeight - BOTTOM_ROUNDED_RECT_HEIGHT);
        }
        return top + headerHeight + visibleHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        boolean wasHandled = this.header != null && this.header.mouseClicked(mouseX, mouseY, button);
        if (wasHandled) {
            super.unfocus();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {

        if (header != null) {
            boolean handled = this.header.mouseReleased(mouseX, mouseY, button);
            if (handled) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return (this.header != null && this.header.mouseDragged(mouseX, mouseY, button, deltaX, deltaY))
                || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            int top = this.getY();
            int bottom = top + this.height;
            int headerHeight = getHeaderHeight();
            int areaHeight = Math.max(0, bottom - top - headerHeight);
            double eased = getEasedProgress();
            int animatedArea = this.expanded
                    ? (int) Math.ceil(eased * areaHeight)
                    : (int) Math.floor(eased * areaHeight);
            int rowCount = (children == null ? 0 : children.size());
            int contentHeight = rowCount * rowHeight;
            int maxScroll = Math.max(0, contentHeight - animatedArea);
            scrollPosition = Math.max(0,
                    Math.min(scrollPosition - verticalAmount * VERTICAL_SCROLL_MULTIPLIER, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public static double lerp(double start, double end, double t) {
        return start + (end - start) * t;
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
                       int top, int right, int bottom) {
        context.getMatrices().push();

        Collection<BaseWidget<?>> localChildren = this.children.values();
        int childCount = localChildren.size();

        if (childCount > 0) {
            int target = this.expanded ? MAX_TICKS_ON_OPEN : 0;
            if (this.ticksOnOpen < target) {
                this.ticksOnOpen = Math.min(++this.ticksOnOpen, MAX_TICKS_ON_OPEN);
                if (this.header != null && this.expanded) {
                    this.header.expanded = true;
                }
            } else if (this.ticksOnOpen > target) {
                this.ticksOnOpen = Math.max(--this.ticksOnOpen, 0);
                if (this.header != null && !this.expanded && this.ticksOnOpen == target) {
                    this.header.expanded = false;
                }
            }

            boolean hasHeader = header != null;
            int hasHeaderInt = hasHeader ? 1 : 0;
            int headerHeight = getHeaderHeight();
            int realTop = top + headerHeight;

            int realBottomInt = getVisibleBottom(top, bottom);
            int realHeight = Math.max(0, realBottomInt - realTop);

            if (!(!this.expanded && this.ticksOnOpen == 0)) {
                int scissorRight = right + 3;
                int scissorTop = Math.max(0, realTop);
                int scissorBottom = this.expanded
                        ? realBottomInt + (showBottomRoundedRect ? BOTTOM_ROUNDED_RECT_HEIGHT : 0) + 3
                        : realBottomInt;
                Scissor.pushExclusive(context, left, scissorTop, scissorRight, scissorBottom);
                int index = -1;
                for(BaseWidget<?> child : localChildren) {
                    index++;
                    if(child == null)
                        continue;
                    child.withXY(left, top - (int) scrollPosition + (index + hasHeaderInt) * rowHeight)
                            .renderWidget(context, mouseX, mouseY, deltaTicks);
                }

                int contentHeight = childCount * rowHeight;
                int overflowHeight = Math.max(0, contentHeight - realHeight);
                if (overflowHeight > 0 && this.expanded && this.ticksOnOpen == MAX_TICKS_ON_OPEN) {
                    context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop, right, realBottomInt,
                            CigaretteScreen.BACKGROUND_COLOR);
                    int track = Math.max(1, realHeight);
                    int knobHeight = Math.max(10, (int) Math.round((track * (double) track) / (double) contentHeight));
                    int maxKnobTravel = track - knobHeight;
                    int knobTop = (int) Math
                            .round((scrollPosition / (double) overflowHeight) * Math.max(0, maxKnobTravel));
                    context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop + knobTop, right,
                            realTop + knobTop + knobHeight,
                            CigaretteScreen.SECONDARY_COLOR);
                }
                int bottomRectTop = realBottomInt;
                if (this.getEasedProgress() > 0.0 && showBottomRoundedRect) {
                    Shape.roundedRect(context, left, bottomRectTop, right,
                            bottomRectTop + BOTTOM_ROUNDED_RECT_HEIGHT,
                            CigaretteScreen.BACKGROUND_COLOR, 5, false, true);
                }
                Scissor.popExclusive();
            }
        }
        if (header != null) {
            header.renderWidget(context, mouseX, mouseY, deltaTicks);
        }
        context.getMatrices().pop();
    }
}