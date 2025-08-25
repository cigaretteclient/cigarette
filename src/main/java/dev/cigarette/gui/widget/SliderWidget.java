package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.lib.Color;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SliderWidget extends BaseWidget<Double> {
    private static final int SLIDER_PADDING = 4;
    private @Nullable Consumer<Double> sliderCallback = null;
    private double maxState = 0;
    private double minState = 0;
    private int decimalPlaces = 0;
    private boolean dragging = false;
    public boolean disabled = false;

    public void setState(double state) {
        if (state > maxState) return;
        if (state < minState) return;
        this.setAccurateState(state);
        if (sliderCallback != null) sliderCallback.accept(state);
    }

    protected void setAccurateState(double state) {
        if (state > maxState) return;
        if (state < minState) return;
        double mult = Math.pow(10, decimalPlaces);
        this.setRawState(Math.round(state * mult) / mult);
    }

    private void setStateFromDrag(double mouseX) {
        int left = this.getX() + SLIDER_PADDING;
        int width = this.getWidth() - 2 * SLIDER_PADDING;
        double percentage = (mouseX - left) / width;
        double value = percentage * (maxState - minState) + minState;
        this.setState(value);
    }

    public SliderWidget(int x, int y, int width, int height, String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.captureHover().withXY(x, y).withWH(width, height).withDefault(0d);
    }

    public SliderWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height).withDefault(0d);
    }

    public SliderWidget(String message, String tooltip) {
        super(message, tooltip);
        this.captureHover().withDefault(0d);
    }

    public SliderWidget(String message) {
        super(message, null);
        this.captureHover().withDefault(0d);
    }

    public SliderWidget withBounds(double min, double def, double max) {
        this.minState = min;
        this.maxState = max;
        this.setRawState(def);
        return this;
    }

    public SliderWidget withAccuracy(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.disabled) return false;
        if (!isMouseOver(mouseX, mouseY)) return false;
        this.dragging = true;
        this.setFocused();
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignored, double ignored_) {
        if (this.disabled) return false;
        if (!dragging) return false;
        this.setStateFromDrag(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
