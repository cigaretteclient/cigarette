package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.Scissor;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
import dev.cigarette.module.ui.Colors;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;

import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Objects;

/**
 * A scrollable widget which provides scrolling functionality.
 *
 * @param <Widgets> The type of children this widget stores. Use {@code Widget extends BaseWidget<?>} to allow any types as children.
 */
public class ScrollableWidget<Widgets extends BaseWidget<?>>
        extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    /**
     * Multiplier applied to the vertical scrolling delta to determine distance to actually scroll.
     */
    private static final int VERTICAL_SCROLL_MULTIPLIER = 6;
    /**
     * The default width of this widget.
     */
    private static final int DEFAULT_WIDTH = 100;
    /**
     * The default height of this widget.
     */
    private static final int DEFAULT_HEIGHT = 200;
    /**
     * The default row height when a child does not specify its own height.
     */
    private static final int DEFAULT_ROW_HEIGHT = 20;
    /**
     * The default scrollbar width of this widget.
     */
    private static final int DEFAULT_SCROLLBAR_WIDTH = 3;
    /**
     * The height of the bottom padding for the rounded effect.
     */
    private static final int BOTTOM_ROUNDED_RECT_HEIGHT = 6;
    /**
     * The height of each row inside this widget.
     */
    private final int rowHeight = DEFAULT_ROW_HEIGHT;
    /**
     * Whether this widget is supposed to be scrollable or not.
     */
    private boolean shouldScroll = false;
    /**
     * The current scroll position.
     */
    private double scrollPosition = 0D;
    /**
     * The optional header that can move this widget when dragged.
     */
    private @Nullable DraggableWidget header;
    /**
     * The category offset index.
     */
    private int categoryOffsetIndex = 0;
    /**
     * Whether this widget is expanded, revealing its children, or not.
     */
    public boolean expanded = true;
    /**
     * Partial ticks when the widget is expanded.
     */
    private int ticksOnOpen = 0;
    /**
     * Max ticks to complete the expanding animation.
     */
    private static final int MAX_TICKS_ON_OPEN = 20;
    /**
     * Callback triggered when the header is clicked to toggle the {@link #expanded} state of this widget.
     */
    private @Nullable Runnable onToggleExpand;
    /**
     * Whether to show the bottom rounding effect or not.
     */
    private boolean showBottomRoundedRect = true;
    /**
     * Cached gradient colors for performance.
     */
    private int cachedColorLeft = -1;
    private int cachedColorRight = -1;
    private double lastGradientPosition = -1.0;
    private long lastGradientUpdate = 0;

    /**
     * Creates a widget that renders children in a scrollable window.
     *
     * @param x          The initial X position of this widget
     * @param y          The initial Y position of this widget
     * @param headerText The text to display as the header at the top of this widget
     * @param children   The children to attach to this widget
     */
    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable String headerText, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.setChildren(children).setHeader(headerText, null);
    }

    /**
     * Creates a widget that renders children in a scrollable window.
     *
     * @param x        The initial X position of this widget
     * @param y        The initial Y position of this widget
     * @param children The children to attach to this widget
     */
    @SafeVarargs
    public ScrollableWidget(int x, int y, @Nullable Widgets... children) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.setChildren(children);
    }

    /**
     * Creates a widget that renders children in a scrollable window.
     *
     * @param x The initial X position of this widget
     * @param y The initial Y position of this widget
     */
    public ScrollableWidget(int x, int y) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
    }

    /**
     * Creates a widget that renders children in a scrollable window.
     *
     * @param x                     The initial X position of this widget
     * @param y                     The initial Y position of this widget
     * @param showBottomRoundedRect Whether to show the bottom rounded effect
     */
    public ScrollableWidget(int x, int y, boolean showBottomRoundedRect) {
        super(x, y, DEFAULT_WIDTH + DEFAULT_SCROLLBAR_WIDTH, DEFAULT_HEIGHT, null);
        this.withDefault(new BaseWidget.Stateless());
        this.showBottomRoundedRect = showBottomRoundedRect;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (this.header != null) {
            this.header.setWidth(width);
        }
        updateChildrenSizing();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        updateChildrenSizing();
    }

    @Override
    public void setDimensions(int width, int height) {
        super.setDimensions(width, height);
        if (this.header != null) {
            this.header.setWidth(width);
        }
        updateChildrenSizing();
    }

    @Override
    public BaseWidget<BaseWidget.Stateless> withXY(int x, int y) {
        if (this.header != null) this.header.withXY(x, y);
        super.withXY(x, y);
        return this;
    }

    /**
     * {@return whether the container should be scrollable} Updates {@link #shouldScroll}.
     */
    private boolean updateShouldScroll() {
        int headerHeight = getHeaderHeight();
        int contentHeight = getChildrenContentHeight();
        int available = Math.max(0, this.height - headerHeight);
        this.shouldScroll = contentHeight > available;
        return this.shouldScroll;
    }

    /**
     * Sets the children attached to this widget.
     *
     * @param children The children to attach
     * @return This widget for method chaining
     */
    @SafeVarargs
    public final ScrollableWidget<Widgets> setChildren(@Nullable Widgets... children) {
        for (Widgets widget : children) {
            if (widget == null) continue;
            widget.drawBackground = false; // Children in scrollable containers don't draw their own backgrounds
            String name = Objects.requireNonNullElse(widget.getMessage(), widget.hashCode()).toString();
            this.children.put(name, widget);
        }
        return updateChildrenSizing();
    }

    /**
     * Sets the header text of this widget. This creates a {@link DraggableWidget} that renders at the top of the widget that also controls its position.
     *
     * @param headerText The text to display
     * @return This widget for method chaining
     */
    public ScrollableWidget<Widgets> setHeader(@Nullable String headerText) {
        return this.setHeader(headerText, null);
    }

    /**
     * Sets the header text of this widget. This creates a {@link DraggableWidget} that renders at the top of the widget that also controls its position.
     *
     * @param headerText     The text to display
     * @param onToggleExpand The callback to run when the header is right-clicked
     * @return This widget for method chaining
     */
    public ScrollableWidget<Widgets> setHeader(@Nullable String headerText, @Nullable Runnable onToggleExpand) {
        this.onToggleExpand = onToggleExpand;
        if (headerText == null) {
            this.header = null;
            return updateChildrenSizing();
        }
        this.header = new DraggableWidget(getX(), getY(), width, rowHeight, headerText);
        this.header.drawBackground = false; // Background rendered by parent in batched mode
        this.header.onDrag((newX, newY, deltaX, deltaY) -> {
            withXY(newX, newY);
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

    /**
     * Gets the header text, if set.
     */
    public @Nullable String getHeaderName() {
        if (this.header == null) {
            return null;
        }
        return this.header.getMessage().getString();
    }

    /**
     * Sets all the children width and heights. This also triggers {@link #updateShouldScroll()} to set whether there needs to be a scrollbar.
     *
     * @return This widget for method chaining
     */
    private ScrollableWidget<Widgets> updateChildrenSizing() {
        if (this.children != null) {
            int rightMargin = this.updateShouldScroll() ? DEFAULT_SCROLLBAR_WIDTH : 0;
            int availableWidth = Math.max(0, width - rightMargin);

            for (ClickableWidget child : children.values()) {
                if (child == null)
                    continue;

                int desiredHeight = child.getHeight() > 0 ? child.getHeight() : rowHeight;
                child.setHeight(desiredHeight);

                int desiredWidth = child.getWidth() > 0 ? Math.min(child.getWidth(), availableWidth) : availableWidth;
                child.setWidth(desiredWidth);

                if (this.shouldScroll && child instanceof PassthroughWidget<?, ?> widget) {
                    widget.childLeftOffset = DEFAULT_SCROLLBAR_WIDTH;
                }
            }
        }
        return this;
    }

    /**
     * {@return the category offset index}
     */
    public int getCategoryOffsetIndex() {
        return this.categoryOffsetIndex;
    }

    /**
     * Sets the category offset index.
     *
     * @param index The category offset index
     */
    public void setCategoryOffsetIndex(int index) {
        this.categoryOffsetIndex = Math.max(0, index);
    }

    /**
     * Sets whether this widget is expanded.
     *
     * @param expanded Whether this widget should be expanded
     */
    public void setExpanded(boolean expanded) {
        if (this.expanded == expanded)
            return;
        this.expanded = expanded;
        this.ticksOnOpen = expanded ? 0 : MAX_TICKS_ON_OPEN;
    }

    private int getChildrenContentHeight() {
        int heightSum = 0;
        if (this.children != null) {
            for (BaseWidget<?> child : this.children.values()) {
                if (child == null) continue;
                int h = child.getHeight();
                heightSum += h > 0 ? h : rowHeight;
            }
        }
        return heightSum;
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
        int contentHeight = getChildrenContentHeight();
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
    public boolean mouseClicked(Click click, boolean doubled) {
        boolean wasHandled = this.header != null && this.header.mouseClicked(click, doubled);
        if (wasHandled) {
            super.unfocus();
            return true;
        }
        return this.expanded && super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {

        if (header != null) {
            boolean handled = this.header.mouseReleased(click);
            if (handled) {
                return true;
            }
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return (this.header != null && this.header.mouseDragged(click, deltaX, deltaY))
                || (this.expanded && super.mouseDragged(click, deltaX, deltaY));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (isMouseOver(mouseX, mouseY)) {
            int top = this.getY();
            int bottom = top + this.height;
            int headerHeight = getHeaderHeight();
            int areaHeight = Math.max(0, bottom - top - headerHeight);
            int contentHeight = getChildrenContentHeight();
            double eased = getEasedProgress();
            int animatedArea = this.expanded
                ? (int) Math.ceil(eased * areaHeight)
                : (int) Math.floor(eased * areaHeight);
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

    /**
     * Returns the header widget for external access.
     */
    public @Nullable DraggableWidget getHeader() {
        return this.header;
    }
    
    /**
     * Renders only the background for batched rendering optimization.
     */
    public void renderBackgroundOnly(DrawContext context) {
        int left = this.getX();
        int top = this.getY();
        int right = left + this.getWidth();
        int bottom = top + this.getHeight();
        renderCategoryBackground(context, left, top, right, bottom);
    }
    
    /**
     * Renders only the header background for batched rendering optimization.
     */
    public void renderHeaderBackgroundOnly(DrawContext context) {
        if (header == null) return;
        int left = header.getX();
        int top = header.getY();
        int right = left + header.getWidth();
        int bottom = top + header.getHeight();
        
        // Render header background with gradient
        dev.cigarette.module.ui.GUI gui = dev.cigarette.module.ui.GUI.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();
        double widgetCenterX = left + ((right - left) / 2.0);
        double normalizedPos = scrW > 0 ? widgetCenterX / scrW : 0.5;
        int colorLeft = Color.lerpColor(Colors.INSTANCE.primaryStart.getStateRGBA(), Colors.INSTANCE.primaryEnd.getStateRGBA(), (float)(normalizedPos - 0.1));
        int colorRight = Color.lerpColor(Colors.INSTANCE.primaryStart.getStateRGBA(), Colors.INSTANCE.primaryEnd.getStateRGBA(), (float)(normalizedPos + 0.1));
        CigaretteScreen.drawGradientRoundedRect(context, left, top, right, bottom, 2, colorLeft, colorRight);
    }
    
    /**
     * Renders the background and border for this category widget with proper height calculation.
     */
    private void renderCategoryBackground(DrawContext context, int left, int top, int right, int bottom) {
        // Calculate the actual visible bottom based on expanded state
        int visibleBottom = getVisibleBottom(top, bottom);
        int actualBottom = visibleBottom;
        
        // Add header height to the visible area
        int headerHeight = getHeaderHeight();
        if (headerHeight > 0) {
            actualBottom = visibleBottom;
        }
        
        if (left < 0 || top < 0 || right < 0 || actualBottom < 0 || actualBottom <= top) return;

        dev.cigarette.module.ui.GUI gui = dev.cigarette.module.ui.GUI.INSTANCE;
        int radius = 4;
        
        // Fast path: if gradients disabled, use solid color
        if (!gui.isGradientEnabled()) {
            CigaretteScreen.drawRoundedRect(context, left, top, right, actualBottom, radius, CigaretteScreen.BACKGROUND_COLOR);
            return;
        }

        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        int scrW = mc.getWindow().getScaledWidth();

        // Calculate gradient position
        double widgetCenterX = left + ((right - left) / 2.0);
        double normalizedPos = scrW > 0 ? widgetCenterX / scrW : 0.5;
        
        // Cache gradient colors to avoid recalculating every frame (update every 100ms or when position changes significantly)
        long currentTime = System.currentTimeMillis();
        boolean needsUpdate = (currentTime - lastGradientUpdate > 100) || 
                             Math.abs(normalizedPos - lastGradientPosition) > 0.02 ||
                             cachedColorLeft == -1;
        
        if (needsUpdate) {
            int colorLerpLeft = Color.lerpColor(Colors.INSTANCE.primaryStart.getStateRGBA(), Colors.INSTANCE.primaryEnd.getStateRGBA(), (float)(normalizedPos - 0.1));
            int colorLerpRight = Color.lerpColor(Colors.INSTANCE.primaryStart.getStateRGBA(), Colors.INSTANCE.primaryEnd.getStateRGBA(), (float)(normalizedPos + 0.1));
            lastGradientPosition = normalizedPos;
            lastGradientUpdate = currentTime;
        }
        
        // Draw gradient background with rounded corners
        CigaretteScreen.drawGradientRoundedRect(context, left, top, right, actualBottom, radius, cachedColorLeft, cachedColorRight);
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
                       int top, int right, int bottom) {
        context.getMatrices().pushMatrix();

        // Render background inline for performance
        renderCategoryBackground(context, left, top, right, bottom);

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
            int headerHeight = getHeaderHeight();
            int realTop = top + headerHeight;

            int realBottomInt = getVisibleBottom(top, bottom);
            int realHeight = Math.max(0, realBottomInt - realTop);
            int contentHeight = getChildrenContentHeight();

            if (!(!this.expanded && this.ticksOnOpen == 0)) {
                // Add margin to prevent clipping rounded corners (radius is 4-5px)
                int margin = 6;
                int scissorRight = right + margin;
                int scissorTop = Math.max(0, realTop);
                int scissorBottom = this.expanded
                        ? realBottomInt + (showBottomRoundedRect ? BOTTOM_ROUNDED_RECT_HEIGHT : 0) + margin
                        : realBottomInt + margin;
                Scissor.pushExclusive(context, left, scissorTop, scissorRight, scissorBottom);
                int yCursor = top - (int) scrollPosition + headerHeight;
                for (BaseWidget<?> child : localChildren) {
                    if (child == null)
                        continue;
                    int childHeight = Math.max(1, child.getHeight());
                    child.withXY(left, yCursor).renderWidget(context, mouseX, mouseY, deltaTicks);
                    yCursor += childHeight;
                }

                // Only render scrollbar when fully expanded and content overflows
                contentHeight = childCount * rowHeight;
                int overflowHeight = Math.max(0, contentHeight - realHeight);
                if (overflowHeight > 0 && this.expanded && this.ticksOnOpen == MAX_TICKS_ON_OPEN) {
                    int scrollBarLeft = right - DEFAULT_SCROLLBAR_WIDTH;
                    context.fill(scrollBarLeft, realTop, right, realBottomInt,
                            CigaretteScreen.BACKGROUND_COLOR);
                    int track = Math.max(1, realHeight);
                    int knobHeight = Math.max(10, (track * track) / contentHeight);
                    int maxKnobTravel = track - knobHeight;
                    int knobTop = maxKnobTravel > 0 ? (int)((scrollPosition * maxKnobTravel) / overflowHeight) : 0;
                    context.fill(scrollBarLeft, realTop + knobTop, right,
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
            if (header != null) {
                header.renderWidget(context, mouseX, mouseY, deltaTicks);
            }
        } else if (header != null) {
            header.renderWidget(context, mouseX, mouseY, deltaTicks);
            
            // Render logo in top-left of header without extra matrix operations
            int headerHeight = getHeaderHeight();
            int logoSize = 16;
            int logoPadding = 4;
            int logoX = left + logoPadding;
            int logoY = top + (headerHeight - logoSize) / 2;
            
            context.drawTexture(RenderPipelines.GUI_TEXTURED, Cigarette.LOGO_IDENTIFIER, logoX, logoY, 0.0f, 0.0f, logoSize, logoSize, logoSize, logoSize);
        }
        context.getMatrices().popMatrix();
    }
}