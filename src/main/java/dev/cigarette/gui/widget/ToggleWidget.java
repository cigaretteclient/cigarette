package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.RenderUtil;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Renderer;
import dev.cigarette.module.BaseModule;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A widget that can be toggled by the user.
 */
public class ToggleWidget extends BaseWidget<Boolean> {
    /**
     * The max number of ticks the hover animation runs for.
     */
    private static final int MAX_HOVER_TICKS = 35;
    /**
     * The current number of ticks the hover animation has run for.
     */
    private int ticksOnHover = 0;
    private @Nullable Consumer<Boolean> callback = null;

    /**
     * The max number of ticks the enable animation runs for.
     */
    private static final float MAX_ENABLE_TICKS = 5f;
    /**
     * The current number of ticks the enable animation has run for.
     */
    private float ticksOnEnable = 0f;

    /**
     * Smoothly transition from color {@code a} to color {@code b} in {@code t} partial ticks.
     *
     * @param a The starting ARGB color to transition from
     * @param b The new ARGB color to transition to
     * @param t Partial ticks
     * @return The current ARGB color at {@code t} partial ticks
     */
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

    /**
     * Creates a togglable widget.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public ToggleWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.captureHover().withDefault(false);
    }

    /**
     * Sets the state and stored default state of this widget.
     *
     * @param state The default state to set
     * @return This widget for method chaining
     */
    public ToggleWidget withDefaultState(boolean state) {
        this.withDefault(state);
        return this;
    }

    /**
     * Generator for modules using this as a top-level widget.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip     The tooltip to render when this widget is hovered
     * @return A {@link BaseModule.GeneratedWidgets} object for use in {@link BaseModule} constructing
     */
    public static BaseModule.GeneratedWidgets<ToggleWidget, Boolean> module(String displayName, @Nullable String tooltip) {
        DropdownWidget<ToggleWidget, Boolean> wrapper = new DropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return new BaseModule.GeneratedWidgets<>(wrapper, widget);
    }

    /**
     * Captures a mouse click to switch the state.
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return Whether this widget handled the click
     */
    @Override
    public boolean mouseClicked(Click mouseInput, boolean doubled) {
        double mouseX = mouseInput.x();
        double mouseY = mouseInput.y();
        double button = mouseInput.button();
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

        int borderColor = Color.color(left, top);
        if (ticksOnHover > 0) {
            float raw = (float) ticksOnHover / MAX_HOVER_TICKS;
            float progress = (float) Math.max(0.0, Math.min(1.0, hovered ? CigaretteScreen.easeOutExpo(raw) : CigaretteScreen.easeInExpo(raw)));
            context.drawHorizontalLine(left, ((int) ((left - 1) + width * progress)), top, borderColor);
            context.drawHorizontalLine((int) (right - width * progress), right - 1, bottom - 1, borderColor);
            context.drawVerticalLine(left, (int) (bottom - height * progress), bottom, borderColor);
            context.drawVerticalLine(right - 1, top - 1, (int) (top + height * progress), borderColor);
        }
        TextRenderer textRenderer = Cigarette.REGULAR;
        context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + height / 3, textColor);
    }

    public class ToggleWidgetDisabled extends ToggleWidget {
        /**
         * Creates a togglable widget whose state can not be changed and renders disabled.
         *
         * @param message The text to display inside this widget
         * @param tooltip The tooltip to render when this widget is hovered
         */
        public ToggleWidgetDisabled(String message, @Nullable String tooltip) {
            super(message, tooltip);
        }

        @Override
        public boolean mouseClicked(Click mouseInput, boolean doubled) {
            double mouseX = mouseInput.x();
            double mouseY = mouseInput.y();
            double button = mouseInput.button();
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && isMouseOver(mouseX, mouseY)) {
                this.setFocused();
                return true;
            }
            return false;
        }

        private int modifyOpacity(int color, float factor) {
            // e.g. 0x80FFFFFF
            int a = (color >> 24) & 0xFF;
            a = Math.min(255, Math.max(0, Math.round(a * factor)));
            return (a << 24) | (color & 0x00FFFFFF);
        }

        @Override
        protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
            this.hovered = false;
            TextRenderer textRenderer = Cigarette.REGULAR;
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
            context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + height / 3, modifyOpacity(CigaretteScreen.PRIMARY_TEXT_COLOR, 0.5f));
        }
    }
}
