package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.util.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ScrollableWidget<T extends ClickableWidget> extends PassthroughWidget<ClickableWidget> {
    private static final int VERTICAL_SCROLL_MULTIPLIER = 6;
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 200;
    private final int rowHeight = 20;
    private double scrollPosition = 0D;
    private @Nullable DraggableWidget header;
    private @Nullable T[] internalChildren;

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Text headerText, @Nullable T... children) {
        super(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, null);
        this.setChildren(children);
        this.setHeader(headerText);
    }

    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable T... children) {
        super(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, null);
        this.setChildren(children);
    }

    public ScrollableWidget(int x, int y) {
        super(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT, null);
    }

    public ScrollableWidget<T> setChildren(@Nullable T... children) {
        this.internalChildren = children;
//        this.children = children;
        if (children != null) {
            for (T child : children) {
                if (child == null) continue;
                child.setWidth(width);
                child.setHeight(rowHeight);
            }
        }
        return mergeInternalChildren();
    }

    public ScrollableWidget<T> setHeader(@Nullable Text headerText) {
        if (headerText == null) {
            this.header = null;
            return mergeInternalChildren();
        }
        this.header = new DraggableWidget(getX(), getY(), width, rowHeight, headerText);
        this.header.onDrag((newX, newY, deltaX, deltaY) -> {
            setX(newX);
            setY(newY);
        });
        return mergeInternalChildren();
    }

    private ScrollableWidget<T> mergeInternalChildren() {
        int length = (internalChildren == null ? 0 : internalChildren.length) + (header == null ? 0 : 1);
        ClickableWidget[] temp = new ClickableWidget[length];
        int index = 0;
        if (header != null) {
            temp[0] = header;
            index++;
        }
        if (internalChildren != null) {
            for (T child : internalChildren) {
                temp[index] = child;
                index++;
            }
        }
        this.children = temp;
        return this;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            int rowCount = (header == null ? 0 : 1) + (children == null ? 0 : children.length);
            scrollPosition = Math.max(0, Math.min(scrollPosition - verticalAmount * VERTICAL_SCROLL_MULTIPLIER, rowCount * rowHeight - height));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (children != null) {
            Scissor.pushExclusive(context, getX(), getY(), getRight(), getBottom());
            for (int index = header != null ? 1 : 0; index < children.length; index++) {
                ClickableWidget child = children[index];
                if (child == null) continue;
                child.setX(getX());
                child.setY(getY() - (int) scrollPosition + index * rowHeight);
                child.render(context, mouseX, mouseY, deltaTicks);
            }
            Scissor.popExclusive();
        }
        if (header != null) {
            header.render(context, mouseX, mouseY, deltaTicks);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
