package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleOptionsWidget extends RootModule<ToggleOptionsWidget> {
    public static ToggleOptionsWidget base = new ToggleOptionsWidget(Text.literal(""), null);
    private static final byte MAX_HOVER_TICKS = 35;
    private boolean dropdownVisible = false;
    private boolean defaultToggledState = false;
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

    public ToggleOptionsWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip, @Nullable BaseWidget... options) {
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

    @Override
    public ToggleOptionsWidget buildModule(String message, @Nullable String tooltip) {
        return new ToggleOptionsWidget(Text.of(message), tooltip == null ? null : Text.of(tooltip));
    }

    public ToggleOptionsWidget(Text message, Text tooltip) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public ToggleOptionsWidget(Text message) {
        super(0, 0, 0, 0, message);
    }

    public ToggleOptionsWidget setOptions(@Nullable BaseWidget... options) {
        this.children = new BaseWidget[]{new ScrollableWidget<>(0, 0).setChildren(options)};
        return this;
    }

    public ToggleOptionsWidget withDefaultState(boolean state) {
        this.defaultToggledState = state;
        this.toggledState = state;
        return this;
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.toggledState = newState;
            FileSystem.updateState(key, newState);
            this.triggerModuleStateUpdate(newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Boolean booleanState)) return;
            this.toggledState = booleanState;
            this.setState(booleanState);
            this.triggerModuleStateUpdate(booleanState);
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
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (hovered) {
            ticksOnHover = Math.min(ticksOnHover + 1, MAX_HOVER_TICKS);
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            ticksOnHover = Math.max(ticksOnHover - 1, 0);
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        int textColor = toggledState ? CigaretteScreen.ENABLED_COLOR : CigaretteScreen.PRIMARY_TEXT_COLOR;

        if (ticksOnHover > 0) {
            float progress = (float) ticksOnHover / MAX_HOVER_TICKS;
            context.drawHorizontalLine(left, (int) (left + width * progress), top, textColor);
            context.drawHorizontalLine((int) (right - width * progress), right, bottom - 1, textColor);
            context.drawVerticalLine(left, (int) (bottom - height * progress), bottom, textColor);
            context.drawVerticalLine(right - 1, top, (int) (top + height * progress), textColor);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, textColor);
        if (children != null) {
            context.drawHorizontalLine(right - 10, right - 4, top + (height / 2), CigaretteScreen.SECONDARY_COLOR);
            if (dropdownVisible) {
                for (BaseWidget child : children) {
                    if (child == null) continue;
                    child.setX(right + childLeftOffset);
                    child.setY(top);
                    child.renderWidget(context, mouseX, mouseY, deltaTicks);
                }
            }
        }
    }
}
