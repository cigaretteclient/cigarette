package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleColorWidget extends PassthroughWidget<ClickableWidget> {
    private boolean dropdownVisible = false;
    private int defaultColorState = 0xFFFFFFFF;
    private int colorState = 0xFFFFFFFF;
    private final ToggleOptionsWidget toggle;
    private final SliderWidget sliderRed = new SliderWidget(Text.literal("Red")).withBounds(0, 255, 255);
    private final SliderWidget sliderGreen = new SliderWidget(Text.literal("Green")).withBounds(0, 255, 255);
    private final SliderWidget sliderBlue = new SliderWidget(Text.literal("Blue")).withBounds(0, 255, 255);
    private final SliderWidget sliderAlpha = new SliderWidget(Text.literal("Alpha")).withBounds(0, 255, 255);
    private @Nullable Consumer<Integer> colorCallback = null;

    public void setState(int color) {
        this.colorState = color;
        this.setSliderStates(color);
        this.updateState();
    }

    private void setSliderStates(int color) {
        sliderAlpha.setAccurateState(color >> 24);
        sliderRed.setAccurateState((color >> 16) & 0xFF);
        sliderGreen.setAccurateState((color >> 8) & 0xFF);
        sliderBlue.setAccurateState(color & 0xFF);
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

    public boolean getToggleState() {
        return this.toggle.getState();
    }

    public ToggleColorWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip, boolean withAlpha) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.toggle = new ToggleOptionsWidget(x, y, width, height, message);
        this.attachChildren(withAlpha);
    }

    public ToggleColorWidget(int x, int y, int width, int height, Text message, boolean withAlpha) {
        super(x, y, width, height, message);
        this.toggle = new ToggleOptionsWidget(x, y, width, height, message);
        this.attachChildren(withAlpha);
    }

    public ToggleColorWidget(Text message, Text tooltip, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.toggle = new ToggleOptionsWidget(0, 0, 0, 0, message);
        this.attachChildren(withAlpha);
    }

    public ToggleColorWidget(Text message, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.toggle = new ToggleOptionsWidget(0, 0, 0, 0, message);
        this.attachChildren(withAlpha);
    }

    public ToggleColorWidget withDefaultColor(int argb) {
        this.defaultColorState = argb;
        this.colorState = argb;
        this.setSliderStates(argb);
        return this;
    }

    public ToggleColorWidget withDefaultState(boolean state) {
        this.toggle.withDefaultState(state);
        return this;
    }

    private void attachChildren(boolean withAlpha) {
        ScrollableWidget<ClickableWidget> wrapper = new ScrollableWidget<>(0, 0, this.sliderRed, this.sliderGreen, this.sliderBlue, withAlpha ? this.sliderAlpha : null);
        this.children = new ScrollableWidget[]{wrapper};
        this.sliderRed.registerUpdate((newColor -> {
            int red = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFF00FFFF) + (red << 16);
            this.updateState();
        }));
        this.sliderGreen.registerUpdate((newColor -> {
            int green = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFFFF00FF) + (green << 8);
            this.updateState();
        }));
        this.sliderBlue.registerUpdate((newColor -> {
            int blue = (int) (double) newColor;
            this.colorState = (this.colorState & 0xFFFFFF00) + blue;
            this.updateState();
        }));
        this.sliderAlpha.registerUpdate((newColor -> {
            int alpha = (int) (double) newColor;
            this.colorState = (alpha << 24) + (this.colorState & 0xFFFFFF);
            this.updateState();
        }));
    }

    public void registerAsOption(String key) {
        this.toggle.registerAsOption(key);

        String colorKey = key + ".color";
        this.registerUpdate(newState -> {
            this.colorState = newState;
            FileSystem.updateState(colorKey, newState);
        });
        FileSystem.registerUpdate(colorKey, newState -> {
            if (!(newState instanceof Integer integerState)) return;
            this.colorState = integerState;
            this.setState(integerState);
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> toggle.mouseClicked(mouseX, mouseY, button);
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> dropdownVisible = !dropdownVisible;
            }
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

    public void registerUpdate(Consumer<Integer> callback) {
        this.colorCallback = callback;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int left = getX();
        int right = getRight();
        int top = getY();
        int bottom = getBottom();

        this.toggle.setY(top);
        this.toggle.setX(left);
        this.toggle.setWidth(width);
        this.toggle.setHeight(height);
        this.toggle.render(context, mouseX, mouseY, deltaTicks);

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
