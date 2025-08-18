package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class DropdownWidget<Widget extends BaseWidget<?>, StateType> extends PassthroughWidget<BaseWidget<?>, BaseWidget.Stateless> {
    protected Widget header;
    protected ScrollableWidget<BaseWidget<?>> container;
    private boolean dropdownVisible = false;
    private boolean dropdownIndicator = true;

    public DropdownWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.withDefault(new BaseWidget.Stateless());
        this.container = new ScrollableWidget<>(0, 0);
        this.children = new ScrollableWidget[]{this.container};
    }

    public DropdownWidget<Widget, StateType> setHeader(Widget header) {
        this.header = header;
        return this;
    }

    public DropdownWidget<Widget, StateType> setChildren(@Nullable BaseWidget<?>... children) {
        this.container.setChildren(children);
        return this;
    }

    public DropdownWidget<Widget, StateType> withIndicator(boolean indicator) {
        this.dropdownIndicator = indicator;
        return this;
    }

    @Override
    public void registerConfigKey(String key) {
        this.header.registerConfigKey(key);
    }

    @Override
    public void unfocus() {
        if (this.header != null) this.header.unfocus();
        this.setFocused(false);
        this.dropdownVisible = false;
        this.container.setFocused(false);
        this.container.setExpanded(false);
        super.unfocus();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.header != null) this.header.mouseMoved(mouseX, mouseY);
        if (dropdownVisible) super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            this.setFocused();
            if (this.header == null) return false;
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.header.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                dropdownVisible = children != null && !dropdownVisible;
            }
            return true;
        }
        boolean captured = dropdownVisible && super.mouseClicked(mouseX, mouseY, button);
        this.setFocused(captured);
        this.dropdownVisible = captured;
        return captured;
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
        if (this.container == null) return;
        if (this.container.focused || this.dropdownVisible) {
            this.container.setExpanded(true);
        } else {
            this.container.setExpanded(false);
        }
        if (this.header == null) return;
        this.header.withXY(left, top).withWH(width, height).renderWidget(context, mouseX, mouseY, deltaTicks);

        if (this.container.children == null) return;
        if (this.container.children.length > 0 && dropdownIndicator) {
            context.drawHorizontalLine(right - 10, right - 4, top + (height / 2), CigaretteScreen.SECONDARY_COLOR);
        }
        if (!dropdownVisible || !this.focused) return;
        Scissor.pushExclusive(context, right, top, right + this.container.getWidth(), top + this.container.getHeight());
        this.container.withXY(right + childLeftOffset, top).renderWidget(context, mouseX, mouseY, deltaTicks);
        Scissor.popExclusive();
    }
}
