package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleOptionsWidget extends PassthroughWidget<ClickableWidget> {
    private static final int BASE_COLOR = 0xFF1A1A1A;
    private static final int DROPDOWN_SELECTED_COLOR = 0xFFFE5F00;
    private static final int HOVERED_COLOR = 0xFF000000;
    private static final int BASE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int ENABLED_TEXT_COLOR = 0xFFB3FF80;
    private static final byte MAX_HOVER_TICKS = 35;
    private boolean dropdownVisible = false;
    private boolean toggledState = false;
    private @Nullable Consumer<Boolean> toggledCallback = null;
    private int ticksOnHover = 0;

    private void toggleState() {
        this.toggledState = !this.toggledState;
        if (this.toggledCallback != null) {
            this.toggledCallback.accept(this.toggledState);
        }
    }

    public void setState(boolean state) {
        this.toggledState = state;
        if (this.toggledCallback != null) {
            this.toggledCallback.accept(this.toggledState);
        }
    }

    public boolean getState() {
        return this.toggledState;
    }

    public ToggleOptionsWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip, @Nullable ClickableWidget... options) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.setOptions(options);
    }

    public ToggleOptionsWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public ToggleOptionsWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public ToggleOptionsWidget(Text message, Text tooltip) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public ToggleOptionsWidget(Text message) {
        super(0, 0, 0, 0, message);
    }

    public ToggleOptionsWidget setOptions(@Nullable ClickableWidget... options) {
        this.children = new ClickableWidget[]{new ScrollableWidget<>(0, 0).setChildren(options)};
        return this;
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.toggledState = newState;
            FileSystem.updateState(key, newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Boolean booleanState)) return;
            this.toggledState = booleanState;
            this.setState(booleanState);
        });
    }

    public void registerUpdate(Consumer<Boolean> callback) {
        this.toggledCallback = callback;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (dropdownVisible) super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> this.toggleState();
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> dropdownVisible = children != null && !dropdownVisible;
            }
            return true;
        }
        return dropdownVisible && super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return dropdownVisible && super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return dropdownVisible && super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return dropdownVisible && super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return dropdownVisible && super.charTyped(chr, modifiers);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int left = getX();
        int right = getRight();
        int top = getY();
        int bottom = getBottom();

        if (isMouseOver(mouseX, mouseY)) {
            ticksOnHover = Math.min(ticksOnHover + 1, MAX_HOVER_TICKS);
            context.fillGradient(left, top, right, bottom, BASE_COLOR, HOVERED_COLOR);
        } else {
            ticksOnHover = Math.max(ticksOnHover - 1, 0);
            context.fill(left, top, right, bottom, BASE_COLOR);
        }

        int textColor = toggledState ? ENABLED_TEXT_COLOR : BASE_TEXT_COLOR;

        if (ticksOnHover > 0) {
            float progress = (float) ticksOnHover / MAX_HOVER_TICKS;
            context.drawHorizontalLine(left, (int) (left + width * progress), top, textColor);
            context.drawHorizontalLine((int) (right - width * progress), right, bottom, textColor);
            context.drawVerticalLine(left, (int) (bottom - height * progress), bottom, textColor);
            context.drawVerticalLine(right - 1, top, (int) (top + height * progress), textColor);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, textColor);
        if (dropdownVisible && children != null) {
            context.drawVerticalLine(right - 3, top, bottom, DROPDOWN_SELECTED_COLOR);
            context.drawVerticalLine(right - 2, top, bottom, DROPDOWN_SELECTED_COLOR);
            context.drawVerticalLine(right - 1, top, bottom, DROPDOWN_SELECTED_COLOR);
            for (ClickableWidget child : children) {
                if (child == null) continue;
                child.setX(right);
                child.setY(top);
                child.render(context, mouseX, mouseY, deltaTicks);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
