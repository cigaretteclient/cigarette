package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SliderWidget extends BaseWidget {
    private static final int BASE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int SLIDER_PADDING = 4;
    private @Nullable Consumer<Double> sliderCallback = null;
    private double maxState = 0;
    private double minState = 0;
    private int decimalPlaces = 0;
    private double sliderState = 0;
    private boolean dragging = false;

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
        this.sliderState = Math.round(state * mult) / mult;
    }

    private void setStateFromDrag(double mouseX) {
        int left = this.getX() + SLIDER_PADDING;
        int width = this.getWidth() - 2 * SLIDER_PADDING;
        double percentage = (mouseX - left) / width;
        double value = percentage * (maxState - minState) + minState;
        this.setState(value);
    }

    public double getState() {
        return this.sliderState;
    }

    public SliderWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.captureHover();
    }

    public SliderWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
        this.captureHover();
    }

    public SliderWidget(Text message, Text tooltip) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.captureHover();
    }

    public SliderWidget(Text message) {
        super(0, 0, 0, 0, message);
        this.captureHover();
    }

    public SliderWidget withBounds(double min, double def, double max) {
        this.minState = min;
        this.maxState = max;
        this.setState(def);
        return this;
    }

    public SliderWidget withAccuracy(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
        return this;
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.setAccurateState(newState);
            FileSystem.updateState(key, this.sliderState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Double doubleState)) return;
            this.setState(doubleState);
        });
    }

    public void registerUpdate(Consumer<Double> callback) {
        this.sliderCallback = callback;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        this.dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignored, double ignored_) {
        if (!dragging) return false;
        this.setStateFromDrag(mouseX);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + 4, CigaretteScreen.PRIMARY_TEXT_COLOR);

        Text value = Text.literal(Double.toString(sliderState));
        context.drawTextWithShadow(textRenderer, value, right - textRenderer.getWidth(value) - 4, top + 4, CigaretteScreen.PRIMARY_COLOR);

        int sliderXState = (int) ((sliderState - minState) / (maxState - minState) * (width - 2 * SLIDER_PADDING)) + (left + SLIDER_PADDING);
        context.drawHorizontalLine(left + SLIDER_PADDING, left + width - SLIDER_PADDING, bottom - 4, CigaretteScreen.SECONDARY_COLOR);
        context.drawVerticalLine(sliderXState - 1, bottom - 6, bottom - 2, CigaretteScreen.PRIMARY_COLOR);
        context.drawVerticalLine(sliderXState, bottom - 7, bottom - 1, CigaretteScreen.PRIMARY_COLOR);
        context.drawVerticalLine(sliderXState + 1, bottom - 6, bottom - 2, CigaretteScreen.PRIMARY_COLOR);
    }
}
