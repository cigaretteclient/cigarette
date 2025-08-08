package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ColorPickerWidget extends PassthroughWidget<BaseWidget<?>, Integer> {
    private boolean dropdownVisible = false;
    private final SliderWidget sliderRed = new SliderWidget(Text.literal("Red")).withBounds(0, 255, 255);
    private final SliderWidget sliderGreen = new SliderWidget(Text.literal("Green")).withBounds(0, 255, 255);
    private final SliderWidget sliderBlue = new SliderWidget(Text.literal("Blue")).withBounds(0, 255, 255);
    private final SliderWidget sliderAlpha = new SliderWidget(Text.literal("Alpha")).withBounds(0, 255, 255);

    private @Nullable Consumer<Integer> colorCallback = null;

    public void setState(int color) {
        this.setRawState(color);
        sliderAlpha.setAccurateState(color >> 24);
        sliderRed.setAccurateState((color >> 16) & 0xFF);
        sliderGreen.setAccurateState((color >> 8) & 0xFF);
        sliderBlue.setAccurateState(color & 0xFF);
        this.updateState();
    }

    public void updateState() {
        if (this.colorCallback != null) {
            this.colorCallback.accept(this.getRawState());
        }
    }

    public int getStateARGB() {
        return this.getRawState();
    }

    public int getStateRGBA() {
        return ((this.getRawState() & 0xFFFFFF) << 8) + (this.getRawState() >> 24);
    }

    public int getStateRGB() {
        return this.getRawState() & 0xFFFFFF;
    }

    public ColorPickerWidget(Text message, Text tooltip, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ColorPickerWidget(Text message, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ColorPickerWidget withDefaultColor(Integer state) {
        this.withDefault(state);
        return this;
    }

    private ColorPickerWidget attachChildren(boolean withAlpha) {
        ScrollableWidget<BaseWidget<?>> wrapper = new ScrollableWidget<BaseWidget<?>>(0, 0, this.sliderRed, this.sliderGreen, this.sliderBlue, withAlpha ? this.sliderAlpha : null);
        this.children = new ScrollableWidget[]{wrapper};
        this.sliderRed.stateCallback = ((newColor -> {
            int red = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFF00FFFF) + (red << 16));
        }));
        this.sliderGreen.stateCallback = ((newColor -> {
            int green = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFFFF00FF) + (green << 8));
        }));
        this.sliderBlue.stateCallback = ((newColor -> {
            int blue = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFFFFFF00) + blue);
        }));
        this.sliderAlpha.stateCallback = ((newColor -> {
            int alpha = (int) (double) newColor;
            this.setRawState((alpha << 24) + (this.getRawState() & 0xFFFFFF));
        }));
        return this;
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.setRawState(newState);
            FileSystem.updateState(key, newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Integer integerState)) return;
            this.setRawState(integerState);
            this.setState(integerState);
        });
    }

    public void registerUpdate(Consumer<Integer> callback) {
        this.colorCallback = callback;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            dropdownVisible = !dropdownVisible;
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
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (hovered) {
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);

        int colorBoxWidth = (bottom - top) - 6;
        context.fill(right - 3 - colorBoxWidth, top + 3, right - 3, bottom - 3, this.getRawState());

        if (dropdownVisible) {
            context.drawVerticalLine(right - 3, top, bottom, CigaretteScreen.SECONDARY_COLOR);
            context.drawVerticalLine(right - 2, top, bottom, CigaretteScreen.SECONDARY_COLOR);
            context.drawVerticalLine(right - 1, top, bottom, CigaretteScreen.SECONDARY_COLOR);
            for (BaseWidget<?> child : children) {
                if (child == null) continue;
                child.withXY(right + childLeftOffset, top).renderWidget(context, mouseX, mouseY, deltaTicks);
            }
        }
    }
}
