package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class ScrollableWidget<Widgets extends BaseWidget<?>>
        extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    private static final int VERTICAL_SCROLL_MULTIPLIER = 6;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int DEFAULT_SCROLLBAR_WIDTH = 3;
    private final int rowHeight = 20;
    private boolean shouldScroll = false;
    private double scrollPosition = 0D;
    private @Nullable DraggableWidget header;
    private int categoryOffsetIndex = 0;
    private boolean expanded = true;
    private int ticksOnOpen = 0;
    private static final int MAX_TICKS_ON_OPEN = 10;

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Text headerText, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.setChildren(children).setHeader(headerText);
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

    private boolean updateShouldScroll() {

        this.shouldScroll = ((children == null ? 0 : children.length) + (header != null ? 1 : 0))
                * rowHeight > this.height;
        return this.shouldScroll;
    }

    @SafeVarargs
    public final ScrollableWidget<Widgets> setChildren(@Nullable Widgets... children) {
        this.children = children;
        return updateChildrenSizing();
    }

    public ScrollableWidget<Widgets> setHeader(@Nullable Text headerText) {
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
                this.expanded = !this.expanded;
                if (!this.expanded) {
                    this.scrollPosition = 0;
                }
                if(!this.expanded) {
                    this.unfocus();
                }
            }
        });
        return updateChildrenSizing();
    }

    private ScrollableWidget<Widgets> updateChildrenSizing() {
        if (this.children != null) {
            int rightMargin = this.updateShouldScroll() ? DEFAULT_SCROLLBAR_WIDTH : 0;
            for (ClickableWidget child : children) {
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

    @Override
    public void unfocus() {
        if (this.header != null)
            this.header.unfocus();
        super.unfocus();
    }

    private double getEasedProgress() {
        double t = ticksOnOpen / (double) MAX_TICKS_ON_OPEN;
        return CigaretteScreen.easeOutExpo(t);
    }

    private int getHeaderHeight() {
        return this.header != null ? this.header.getHeight() : 0;
    }

    private int getVisibleBottom(int top, int bottom) {
        int headerHeight = getHeaderHeight();
        int areaHeight = Math.max(0, bottom - top - headerHeight);
        int contentHeight = (children == null ? 0 : children.length * rowHeight);
        int maxVisible = Math.min(areaHeight, contentHeight);
        int visibleHeight = (int) Math.round(getEasedProgress() * maxVisible);
        return top + headerHeight + visibleHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {

        if (this.header != null) {
            boolean handled = this.header.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                return true;
            }
        }

        int top = this.getY();
        int bottom = top + this.height;
        int realTop = top + getHeaderHeight();
        int realBottomInt = getVisibleBottom(top, bottom);

        if (mouseY >= realTop && mouseY < realBottomInt) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (isMouseOver(mouseX, mouseY)) {
            return true;
        }
        return false;
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
            int animatedArea = (int) Math.round(getEasedProgress() * areaHeight);
            int rowCount = (children == null ? 0 : children.length);
            int contentHeight = rowCount * rowHeight;
            int maxScroll = Math.max(0, contentHeight - animatedArea);
            scrollPosition = Math.max(0,
                    Math.min(scrollPosition - verticalAmount * VERTICAL_SCROLL_MULTIPLIER, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
            int top, int right, int bottom) {
        if (children != null) {

            int target = this.expanded ? MAX_TICKS_ON_OPEN : 0;
            if (this.ticksOnOpen < target) {
                this.ticksOnOpen = Math.min(this.ticksOnOpen + 1, MAX_TICKS_ON_OPEN);
            } else if (this.ticksOnOpen > target) {
                this.ticksOnOpen = Math.max(this.ticksOnOpen - 1, 0);
            }

            boolean hasHeader = header != null;
            int hasHeaderInt = hasHeader ? 1 : 0;
            int headerHeight = getHeaderHeight();
            int realTop = top + headerHeight;

            int realBottomInt = getVisibleBottom(top, bottom);
            int realHeight = Math.max(0, realBottomInt - realTop);

            Scissor.pushExclusive(context, left, realTop, right, realBottomInt);
            for (int index = 0; index < children.length; index++) {
                BaseWidget<?> child = children[index];
                if (child == null)
                    continue;
                child.withXY(left, top - (int) scrollPosition + (index + hasHeaderInt) * rowHeight)
                        .renderWidget(context, mouseX, mouseY, deltaTicks);
            }

            int contentHeight = children.length * rowHeight;
            int overflowHeight = Math.max(0, contentHeight - realHeight);
            if (overflowHeight > 0) {
                context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop, right, realBottomInt,
                        CigaretteScreen.BACKGROUND_COLOR);
                int track = Math.max(1, realHeight);
                int knobHeight = Math.max(10, (int) Math.round((track * (double) track) / (double) contentHeight));
                int maxKnobTravel = track - knobHeight;
                int knobTop = (int) Math.round((scrollPosition / (double) overflowHeight) * Math.max(0, maxKnobTravel));
                context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop + knobTop, right, realTop + knobTop + knobHeight,
                        CigaretteScreen.SECONDARY_COLOR);
            }
            Scissor.popExclusive();

            int bottomRectTop = realBottomInt;
            if (this.getEasedProgress() > 0.0) {
                DraggableWidget.roundedRect(context, left, bottomRectTop, right, bottomRectTop + 4,
                        CigaretteScreen.BACKGROUND_COLOR, 2, false, true);
            }
        }
        if (header != null) {
            header.renderWidget(context, mouseX, mouseY, deltaTicks);
        }
    }
}
