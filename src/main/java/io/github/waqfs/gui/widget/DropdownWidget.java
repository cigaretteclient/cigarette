package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.util.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class DropdownWidget<Widget extends BaseWidget<?>, StateType> extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    protected Widget header;
    private boolean dropdownVisible = false;

    public DropdownWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
    }

    public DropdownWidget<Widget, StateType> setHeader(Widget header) {
        this.header = header;
        return this;
    }

    public DropdownWidget<Widget, StateType> setChildren(@Nullable BaseWidget<?>... children) {
        this.children = children;
        return this;
    }

    @Override
    public void registerConfigKey(String key) {
        this.header.registerConfigKey(key);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.header != null) this.header.mouseMoved(mouseX, mouseY);
        if (dropdownVisible) super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (this.header == null) return false;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.header.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                dropdownVisible = children != null && !dropdownVisible;
            }
            return true;
        }
        return dropdownVisible && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.header != null) this.header.mouseReleased(mouseX, mouseY, button);
        super.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.header != null) this.header.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return dropdownVisible && super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (this.header == null) return;
        this.header.withXY(left, top).withWH(width, height).renderWidget(context, mouseX, mouseY, deltaTicks);

        if (this.children == null || !dropdownVisible) return;
        int maxWidth = 0;
        int maxHeight = 0;
        for (BaseWidget<?> child : children) {
            if (child == null) continue;
            child.withWH(width, height);
            maxWidth += child.getWidth();
            maxHeight += child.getHeight();
        }
        Scissor.pushExclusive(context, right, top, right + maxWidth, top + maxHeight);
        int offsetTop = 0;
        for (BaseWidget<?> child : children) {
            if (child == null) continue;
            child.withXY(right + childLeftOffset, top + offsetTop).withWH(width, height).renderWidget(context, mouseX, mouseY, deltaTicks);
            offsetTop += child.getHeight();
        }
        Scissor.popExclusive();
    }
}
