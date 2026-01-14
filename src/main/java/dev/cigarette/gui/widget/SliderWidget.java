package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.lib.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A widget that holds a value that be adjusted by a slider.
 */
public class SliderWidget extends BaseWidget<Double> {
    /**
     * The amount of padding on the left and right side of the sliders bounding box to prevent the slider bar from reaching the border.
     */
    private static final int SLIDER_PADDING = 4;
    /**
     * Callback triggered when this widget's state changes.
     */
    private @Nullable Consumer<Double> sliderCallback = null;
    /**
     * The maximum value this slider can hold.
     */
    private double maxState = 0;
    /**
     * The minimum value this slider can hold.
     */
    private double minState = 0;
    /**
     * The number of decimal places to include when getting this widgets state.
     */
    private int decimalPlaces = 0;
    /**
     * Whether the slider head is actively being dragged by the user.
     */
    private boolean dragging = false;
    /**
     * Whether this slider is disabled.
     */
    public boolean disabled = false;

    /**
     * Updates the state of the slider and triggers the state change callback.
     * <p>This returns prematurely if the state is not within bounds.</p>
     *
     * @param state The new state to set to this widget
     */
    public void setState(double state) {
        if (state > maxState) return;
        if (state < minState) return;
        this.setAccurateState(state);
        if (sliderCallback != null) sliderCallback.accept(state);
    }

    /**
     * Updates the raw state of the slider do the {@link #decimalPlaces} degree of accuracy.
     * <p>This returns prematurely if the state is not within bounds. Does not trigger any callbacks by itself.</p>
     *
     * @param state The new state to set to this widget
     */
    protected void setAccurateState(double state) {
        if (state > maxState) return;
        if (state < minState) return;
        double mult = Math.pow(10, decimalPlaces);
        this.setRawState(Math.round(state * mult) / mult);
    }

    /**
     * Updates the stored state based on the position of the slider.
     *
     * @param mouseX the X coordinate of the mouse
     */
    private void setStateFromDrag(double mouseX) {
        int left = this.getX() + SLIDER_PADDING;
        int width = this.getWidth() - 2 * SLIDER_PADDING;
        double percentage = (mouseX - left) / width;
        double value = percentage * (maxState - minState) + minState;
        this.setState(value);
    }

    /**
     * Creates a widget with a slider to adjust its value.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public SliderWidget(int x, int y, int width, int height, String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.captureHover().withXY(x, y).withWH(width, height).withDefault(0d);
    }

    /**
     * Creates a widget with a slider to adjust its value.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     */
    public SliderWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height).withDefault(0d);
    }

    /**
     * Creates a widget with a slider to adjust its value.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public SliderWidget(String message, String tooltip) {
        super(message, tooltip);
        this.captureHover().withDefault(0d);
    }

    /**
     * Creates a widget with a slider to adjust its value.
     *
     * @param message The text to display inside this widget
     */
    public SliderWidget(String message) {
        super(message, null);
        this.captureHover().withDefault(0d);
    }

    /**
     * Sets the min, max, and default state of this widget.
     *
     * @param min The minimum value the state can be
     * @param def The default state to set
     * @param max The maximum value the state can be
     * @return This widget for method chaining
     */
    public SliderWidget withBounds(double min, double def, double max) {
        this.minState = min;
        this.maxState = max;
        this.setRawState(def);
        return this;
    }

    /**
     * Sets how many decimal places will be reported when getting the state.
     *
     * @param decimalPlaces The number of decimal places
     * @return This widget for method chaining
     */
    public SliderWidget withAccuracy(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    /**
     * Captures a mouse click to initiate dragging on the slider.
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
        if (this.disabled) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;
        this.dragging = true;
        this.setFocused();
        return true;
    }

    /**
     * Captures a mouse drag to update the state and position of the slider.
     *
     * @param mouseX   the X coordinate of the mouse
     * @param mouseY   the Y coordinate of the mouse
     * @param button   the mouse button number
     * @param ignored  the mouse delta X
     * @param ignored_ the mouse delta Y
     * @return Whether this widget handled the drag
     */
    @Override
    public boolean mouseDragged(Click mouseInput, double ignored, double ignored_) {
        double mouseX = mouseInput.x();
        if (this.disabled) return false;
        if (!dragging) return false;
        this.setStateFromDrag(mouseX);
        return true;
    }

    /**
     * Captures a mouse release to stop the dragging of the slider.
     * <p>Does not prevent this event from propagating to other elements.</p>
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return {@code false}
     */
    @Override
    public boolean mouseReleased(Click mouseInput) {
        if (this.disabled) return false;
        dragging = false;
        return false;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (hovered) {
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        int textColor = this.disabled ? Color.colorDarken(CigaretteScreen.PRIMARY_TEXT_COLOR, 0.4f) : CigaretteScreen.PRIMARY_TEXT_COLOR;
        int primaryColor = this.disabled ? Color.colorDarken(CigaretteScreen.PRIMARY_COLOR, 0.4f) : CigaretteScreen.PRIMARY_COLOR;
        int secondaryColor = this.disabled ? Color.colorDarken(CigaretteScreen.SECONDARY_COLOR, 0.4f) : CigaretteScreen.SECONDARY_COLOR;

        TextRenderer textRenderer = Cigarette.REGULAR;
        context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + 4, textColor);

        Text value = Text.literal(Double.toString(this.getRawState()));
        context.drawTextWithShadow(textRenderer, value, right - textRenderer.getWidth(value) - 4, top + 4, primaryColor);

        int sliderXState = (int) ((this.getRawState() - minState) / (maxState - minState) * (width - 2 * SLIDER_PADDING)) + (left + SLIDER_PADDING);
        context.drawHorizontalLine(left + SLIDER_PADDING, left + width - SLIDER_PADDING, bottom - 4, secondaryColor);
        context.drawVerticalLine(sliderXState - 1, bottom - 6, bottom - 2, primaryColor);
        context.drawVerticalLine(sliderXState, bottom - 7, bottom - 1, primaryColor);
        context.drawVerticalLine(sliderXState + 1, bottom - 6, bottom - 2, primaryColor);
    }
}
