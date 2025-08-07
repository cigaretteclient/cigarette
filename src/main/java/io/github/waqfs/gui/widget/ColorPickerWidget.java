package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ColorPickerWidget extends PassthroughWidget<ClickableWidget> {
    private boolean dropdownVisible = false;
    private int defaultColorState = 0xFFFFFFFF;
    private int colorState = 0xFFFFFFFF;
    private SliderWidget sliderRed = new SliderWidget(Text.literal("Red")).withBounds(0, 255, 255);
    private SliderWidget sliderGreen = new SliderWidget(Text.literal("Green")).withBounds(0, 255, 255);
    private SliderWidget sliderBlue = new SliderWidget(Text.literal("Blue")).withBounds(0, 255, 255);
    private SliderWidget sliderAlpha = new SliderWidget(Text.literal("Alpha")).withBounds(0, 255, 255);

    private @Nullable Consumer<Integer> colorCallback = null;

    public void setState(int color) {
        this.colorState = color;
        sliderAlpha.setAccurateState(color >> 24);
        sliderRed.setAccurateState((color >> 16) & 0xFF);
        sliderGreen.setAccurateState((color >> 8) & 0xFF);
        sliderBlue.setAccurateState(color & 0xFF);
        this.updateState();
    }

    public void updateState() {
        if (this.colorCallback != null) {
            this.colorCallback.accept(this.colorState);
        }
    }

    public int getStateARGB() {
        return this.colorState;
    }

    public int getStateRGBA() {
        return ((this.colorState & 0xFFFFFF) << 8) + (this.colorState >> 24);
    }

    public int getStateRGB() {
        return this.colorState & 0xFFFFFF;
    }

    public ColorPickerWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip, boolean withAlpha) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.attachChildren(withAlpha);
    }

    public ColorPickerWidget(int x, int y, int width, int height, Text message, boolean withAlpha) {
        super(x, y, width, height, message);
        this.attachChildren(withAlpha);
    }

    public ColorPickerWidget(Text message, Text tooltip, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.attachChildren(withAlpha);
    }

    public ColorPickerWidget(Text message, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.attachChildren(withAlpha);
    }

    public ColorPickerWidget withDefaultColor(int color) {
        this.defaultColorState = color;
        this.colorState = color;
        return this;
    }

    private void attachChildren(boolean withAlpha) {
        ScrollableWidget<ClickableWidget> wrapper = new ScrollableWidget<>(0, 0, this.sliderRed, this.sliderGreen, this.sliderBlue, withAlpha ? this.sliderAlpha : null);
        this.children = new ScrollableWidget[]{wrapper};
        this.sliderRed.registerUpdate((newColor -> {
            int red = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFF00FFFF) + (red << 16);
        }));
        this.sliderGreen.registerUpdate((newColor -> {
            int green = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFFFF00FF) + (green << 8);
        }));
        this.sliderBlue.registerUpdate((newColor -> {
            int blue = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFFFFFF00) + blue;
        }));
        this.sliderAlpha.registerUpdate((newColor -> {
            int alpha = (int) (double) newColor;
            this.colorState = (alpha << 24) + (this.colorState & 0xFFFFFF);
        }));
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.colorState = newState;
            FileSystem.updateState(key, newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Integer integerState)) return;
            this.colorState = integerState;
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
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int left = getX();
        int right = getRight();
        int top = getY();
        int bottom = getBottom();

        if (isMouseOver(mouseX, mouseY)) {
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);

        int colorBoxWidth = (bottom - top) - 6;
        context.fill(right - 5 - colorBoxWidth, top + 3, right - 5, bottom - 3, this.colorState);

        if (dropdownVisible) {
            context.drawVerticalLine(right - 3, top, bottom, CigaretteScreen.SECONDARY_COLOR);
            context.drawVerticalLine(right - 2, top, bottom, CigaretteScreen.SECONDARY_COLOR);
            context.drawVerticalLine(right - 1, top, bottom, CigaretteScreen.SECONDARY_COLOR);
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
