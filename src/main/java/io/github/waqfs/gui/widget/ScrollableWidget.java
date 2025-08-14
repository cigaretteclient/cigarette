package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.util.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ScrollableWidget<Widgets extends BaseWidget<?>> extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    private static final int VERTICAL_SCROLL_MULTIPLIER = 6;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int DEFAULT_SCROLLBAR_WIDTH = 3;
    private final int rowHeight = 20;
    private boolean shouldScroll = false;
    private double scrollPosition = 0D;
    private @Nullable DraggableWidget header;

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Text headerText, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.setChildren(children).setHeader(headerText);
    }

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.setChildren(children);
    }

    public ScrollableWidget(int x, int y) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
    }

    private boolean updateShouldScroll() {
        this.shouldScroll = ((children.length + (header != null ? 1 : 0)) * rowHeight) > DEFAULT_HEIGHT;
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
        return updateChildrenSizing();
    }

    private ScrollableWidget<Widgets> updateChildrenSizing() {
        if (this.children != null) {
            int rightMargin = this.updateShouldScroll() ? DEFAULT_SCROLLBAR_WIDTH : 0;
            for (ClickableWidget child : children) {
                if (child == null) continue;
                child.setHeight(rowHeight);
                child.setWidth(width - rightMargin);
                if (this.shouldScroll && child instanceof PassthroughWidget widget) {
                    widget.childLeftOffset = DEFAULT_SCROLLBAR_WIDTH;
                }
            }
        }
        return this;
    }

    @Override
    public void unfocus() {
        if (this.header != null) this.header.unfocus();
        super.unfocus();
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
            this.header.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return (this.header != null && this.header.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            int rowCount = (children == null ? 0 : children.length) + (header == null ? 0 : 1);
            scrollPosition = Math.max(0, Math.min(scrollPosition - verticalAmount * VERTICAL_SCROLL_MULTIPLIER, rowCount * rowHeight - height));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (children != null) {
            boolean hasHeader = header != null;
            int hasHeaderInt = hasHeader ? 1 : 0;
            int realTop = top + (hasHeader ? header.getHeight() : 0);
            Scissor.pushExclusive(context, left, realTop, right, bottom);
            for (int index = 0; index < children.length; index++) {
                BaseWidget<?> child = children[index];
                if (child == null) continue;
                child.withXY(left, top - (int) scrollPosition + (index + hasHeaderInt) * rowHeight).renderWidget(context, mouseX, mouseY, deltaTicks);
            }
            if (this.shouldScroll) {
                context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
                int realHeight = height - hasHeaderInt * rowHeight;
                double overflowHeight = (children.length * rowHeight) - (double) realHeight;
                if (overflowHeight > realHeight - rowHeight) {
                    int topMargin = (int) ((scrollPosition / overflowHeight) * (realHeight - rowHeight));
                    context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop + topMargin, right, realTop + topMargin + rowHeight, CigaretteScreen.SECONDARY_COLOR);
                } else {
                    int scrollbarHeight = realHeight - (int) overflowHeight;
                    context.fill(right - DEFAULT_SCROLLBAR_WIDTH, realTop + (int) scrollPosition, right, realTop + (int) scrollPosition + scrollbarHeight, CigaretteScreen.SECONDARY_COLOR);
                }
            }
            Scissor.popExclusive();
        }
        if (header != null) {
            header.renderWidget(context, mouseX, mouseY, deltaTicks);
        }
    }
}
