package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.module.BaseModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleWidget extends BaseWidget<Boolean> {
    private static final int MAX_HOVER_TICKS = 35;
    private int ticksOnHover = 0;
    private @Nullable Consumer<Boolean> callback = null;

    public ToggleWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.captureHover().withDefault(false);
    }

    public ToggleWidget withDefaultState(boolean state) {
        this.withDefault(state);
        return this;
    }

    public static BaseModule.GeneratedWidgets<ToggleWidget, Boolean> module(Text displayName, @Nullable Text tooltip) {
        DropdownWidget<ToggleWidget, Boolean> wrapper = new DropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return new BaseModule.GeneratedWidgets<>(wrapper, widget);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.setRawState(!this.getRawState());
        }
        this.setFocused();
        return true;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (hovered) {
            ticksOnHover = Math.min(++ticksOnHover, MAX_HOVER_TICKS);
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            ticksOnHover = Math.max(--ticksOnHover, 0);
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        int textColor = this.getRawState() ? CigaretteScreen.ENABLED_COLOR : CigaretteScreen.PRIMARY_TEXT_COLOR;
        int borderColor = DraggableWidget.color(left, top);
        context.drawVerticalLine(left, bottom - height, bottom, borderColor);
        if (ticksOnHover > 0) {
            float progress = (float)CigaretteScreen.easeOutExpo((float) ticksOnHover / MAX_HOVER_TICKS);
            context.drawHorizontalLine(left, (int) (left + width * progress), top, borderColor);
            context.drawHorizontalLine((int) (right - width * progress), right, bottom - 1, borderColor);
            context.drawVerticalLine(right - 1, top, (int) (top + height * progress), borderColor);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, textColor);
    }
}
