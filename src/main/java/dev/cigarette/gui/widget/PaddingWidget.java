package dev.cigarette.gui.widget;

import net.minecraft.client.gui.DrawContext;

/**
 * A widget that adds padding around a single child widget.
 */
public class PaddingWidget extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    /**
     * The padding on all sides.
     */
    private int padding;

    /**
     * Creates a padding widget with the specified padding.
     *
     * @param padding The padding in pixels
     * @param child   The child widget to pad
     */
    public PaddingWidget(int padding, BaseWidget<?> child) {
        super(0, 0, child.getWidth() + padding * 2, child.getHeight() + padding * 2, null);
        this.padding = padding;
        this.withDefault(new BaseWidget.Stateless());
        this.setChild(child);
    }

    /**
     * Sets the child widget.
     *
     * @param child The child widget
     * @return This widget for method chaining
     */
    public PaddingWidget setChild(BaseWidget<?> child) {
        if (child != null) {
            this.children.put("child", child);
            this.setWidth(child.getWidth() + padding * 2);
            this.setHeight(child.getHeight() + padding * 2);
        }
        return this;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        // Position the child with padding
        BaseWidget<?> child = this.children.get("child");
        if (child != null) {
            child.withXY(left + padding, top + padding)
                 .renderWidget(context, mouseX, mouseY, deltaTicks);
        }
    }
}