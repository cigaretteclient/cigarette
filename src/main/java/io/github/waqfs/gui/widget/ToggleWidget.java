package io.github.waqfs.gui.widget;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.hud.notification.Notification;
import io.github.waqfs.module.BaseModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.function.Consumer;

public class ToggleWidget extends BaseWidget<Boolean> {
    private static final int MAX_HOVER_TICKS = 35;
    private int ticksOnHover = 0;
    private @Nullable Consumer<Boolean> callback = null;

    private static final float MAX_ENABLE_TICKS = 5f;
    private float ticksOnEnable = 0f;

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aA = (a >> 24) & 0xFF;
        int aR = (a >> 16) & 0xFF;
        int aG = (a >> 8) & 0xFF;
        int aB = a & 0xFF;

        int bA = (b >> 24) & 0xFF;
        int bR = (b >> 16) & 0xFF;
        int bG = (b >> 8) & 0xFF;
        int bB = b & 0xFF;

        int rA = Math.round(aA + (bA - aA) * t);
        int rR = Math.round(aR + (bR - aR) * t);
        int rG = Math.round(aG + (bG - aG) * t);
        int rB = Math.round(aB + (bB - aB) * t);

        return (rA & 0xFF) << 24 | (rR & 0xFF) << 16 | (rG & 0xFF) << 8 | (rB & 0xFF);
    }

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

        if (this.getRawState()) {
            ticksOnEnable = Math.min(ticksOnEnable + deltaTicks, MAX_ENABLE_TICKS);
        } else {
            ticksOnEnable = Math.max(ticksOnEnable - deltaTicks, 0f);
        }
        float enableT = ticksOnEnable / MAX_ENABLE_TICKS;
        float easedEnable = (float) Math.max(0.0, Math.min(1.0, enableT));
        int textColor = lerpColor(CigaretteScreen.PRIMARY_TEXT_COLOR, CigaretteScreen.ENABLED_COLOR, easedEnable);

        int borderColor = DraggableWidget.color(left, top);
        if (ticksOnHover > 0) {
            float progress = (float) ticksOnHover / MAX_HOVER_TICKS;
            context.drawHorizontalLine(left, ((int) ((left-1) + width * progress)), top, borderColor);
            context.drawHorizontalLine((int) (right - width * progress), right - 1, bottom - 1, borderColor);
            context.drawVerticalLine(left, (int) (bottom - height * progress), bottom, borderColor);
            context.drawVerticalLine(right - 1, top - 1, (int) (top + height * progress), borderColor);
        }
        TextRenderer textRenderer = Cigarette.REGULAR;
        context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + height / 3, textColor);
    }
}
