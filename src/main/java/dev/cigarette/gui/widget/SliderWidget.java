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
    double maxState = 0;
    double minState = 0;
    int decimalPlaces = 0;
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


    public class TwoHandedSliderWidget extends SliderWidget {
        private final SliderWidget secondSlider;

        public TwoHandedSliderWidget(int x, int y, int width, int height, String message, @Nullable String tooltip) {
            super(x, y, width, height, message, tooltip);
            secondSlider = new SliderWidget(x + width + 5, y, width, height, message + " 2", tooltip);
            secondSlider.withBounds(this.minState, this.maxState, this.maxState);
            secondSlider.withAccuracy(this.decimalPlaces);
            secondSlider.sliderCallback = (value) -> {
                if (value < this.getRawState()) {
                    this.setState(value);
                }
            };
        }

        public TwoHandedSliderWidget(int x, int y, int width, int height, String message) {
            super(x, y, width, height, message);
            secondSlider = new SliderWidget(x + width + 5, y, width, height, message + " 2");
            secondSlider.withBounds(this.minState, this.maxState, this.maxState);
            secondSlider.withAccuracy(this.decimalPlaces);
            secondSlider.sliderCallback = (value) -> {
                if (value < this.getRawState()) {
                    this.setState(value);
                }
            };
        }

        public TwoHandedSliderWidget(String message, String tooltip) {
            super(message, tooltip);
            secondSlider = new SliderWidget(message + " 2", tooltip);
            secondSlider.withBounds(this.minState, this.maxState, this.maxState);
            secondSlider.withAccuracy(this.decimalPlaces);
            secondSlider.sliderCallback = (value) -> {
                if (value < this.getRawState()) {
                    this.setState(value);
                }
            };
        }

        public TwoHandedSliderWidget(String message) {
            super(message);
            secondSlider = new SliderWidget(message + " 2");
            secondSlider.withBounds(this.minState, this.maxState, this.maxState);
            secondSlider.withAccuracy(this.decimalPlaces);
            secondSlider.sliderCallback = (value) -> {
                if (value < this.getRawState()) {
                    this.setState(value);
                }
            };
        }

        @Override
        public void setState(double state) {
            super.setState(state);
            if (secondSlider.getRawState() < state) {
                secondSlider.setState(state);
            }
        }

        @Override
        protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
            super.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
            secondSlider.withXY(left + this.getWidth() + 5, top);
            secondSlider.render(context, secondSlider.isMouseOver(mouseX, mouseY), mouseX, mouseY, deltaTicks, secondSlider.getX(), secondSlider.getY(), secondSlider.getX() + secondSlider.getWidth(), secondSlider.getY() + secondSlider.getHeight());
        }
    }

    public static class TwoHandedSlider extends SliderWidget {
        private final SliderWidget maxSlider;

        public TwoHandedSlider(String message, String tooltip) {
            super(message, tooltip);
            maxSlider = new SliderWidget(message + " Max", tooltip);
            syncBounds();
            linkCallbacks();
        }

        public TwoHandedSlider(String message) {
            super(message);
            maxSlider = new SliderWidget(message + " Max");
            syncBounds();
            linkCallbacks();
        }

        private void syncBounds() {
            maxSlider.withBounds(this.minState, this.maxState, this.maxState);
            maxSlider.withAccuracy(this.decimalPlaces);
        }

        private void linkCallbacks() {
            maxSlider.sliderCallback = (value) -> {
                if (value < this.getRawState()) {
                    this.setState(value);
                }
            };
        }

        @Override
        public TwoHandedSlider withBounds(double min, double def, double max) {
            super.withBounds(min, def, max);
            syncBounds();
            if (maxSlider.getRawState() < getRawState()) maxSlider.setState(getRawState());
            return this;
        }

        @Override
        public TwoHandedSlider withAccuracy(int decimalPlaces) {
            super.withAccuracy(decimalPlaces);
            maxSlider.withAccuracy(decimalPlaces);
            return this;
        }

        @Override
        public void setState(double state) {
            super.setState(state);
            if (maxSlider.getRawState() < state) {
                maxSlider.setState(state);
            }
        }

        @Override
        public void registerConfigKey(String key) {
            super.registerConfigKey(key + ".min");
            maxSlider.registerConfigKey(key + ".max");
        }

        public double getMinValue() { return this.getRawState(); }
        public double getMaxValue() { return maxSlider.getRawState(); }

        @Override
        protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
            super.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
            maxSlider.withXY(left + this.getWidth() + 5, top).withWH(this.getWidth(), this.getHeight());
            maxSlider.render(context, maxSlider.isMouseOver(mouseX, mouseY), mouseX, mouseY, deltaTicks, maxSlider.getX(), maxSlider.getY(), maxSlider.getX() + maxSlider.getWidth(), maxSlider.getY() + maxSlider.getHeight());
        }
    }
}
