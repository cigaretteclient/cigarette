package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleColorWidget extends RootModule<ToggleColorWidget, Integer> {
    public static ToggleColorWidget base = new ToggleColorWidget(Text.literal(""), false);
    private boolean dropdownVisible = false;
    private final ToggleOptionsWidget toggle;
    private final SliderWidget sliderRed = new SliderWidget(Text.literal("Red")).withBounds(0, 255, 255);
    private final SliderWidget sliderGreen = new SliderWidget(Text.literal("Green")).withBounds(0, 255, 255);
    private final SliderWidget sliderBlue = new SliderWidget(Text.literal("Blue")).withBounds(0, 255, 255);
    private final SliderWidget sliderAlpha = new SliderWidget(Text.literal("Alpha")).withBounds(0, 255, 255);
    private @Nullable Consumer<Integer> colorCallback = null;
    @Nullable Consumer<Boolean> moduleStateCallback = null;

    public void setState(int color) {
        this.setRawState(color);
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

    public boolean getToggleState() {
        return this.toggle.getRawState();
    }

    public ToggleColorWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip, boolean withAlpha) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.toggle = new ToggleOptionsWidget(x, y, width, height, message);
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ToggleColorWidget(int x, int y, int width, int height, Text message, boolean withAlpha) {
        super(x, y, width, height, message);
        this.toggle = new ToggleOptionsWidget(x, y, width, height, message);
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ToggleColorWidget(Text message, Text tooltip, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
        this.toggle = new ToggleOptionsWidget(0, 0, 0, 0, message);
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ToggleColorWidget(Text message, boolean withAlpha) {
        super(0, 0, 0, 0, message);
        this.toggle = new ToggleOptionsWidget(0, 0, 0, 0, message);
        this.attachChildren(withAlpha).captureHover().withDefault(0xFFFFFFFF);
    }

    public ToggleColorWidget withDefaultColor(int argb) {
        this.withDefault(argb);
        this.setSliderStates(argb);
        return this;
    }

    public ToggleColorWidget withDefaultState(boolean state) {
        this.toggle.withDefaultState(state);
        return this;
    }

    private ToggleColorWidget attachChildren(boolean withAlpha) {
        ScrollableWidget<BaseWidget<?>> wrapper = new ScrollableWidget<>(0, 0, this.sliderRed, this.sliderGreen, this.sliderBlue, withAlpha ? this.sliderAlpha : null);
        this.children = new ScrollableWidget[]{wrapper};
        this.sliderRed.registerUpdate((newColor -> {
            int red = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFF00FFFF) + (red << 16));
            this.updateState();
        }));
        this.sliderGreen.registerUpdate((newColor -> {
            int green = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFFFF00FF) + (green << 8));
            this.updateState();
        }));
        this.sliderBlue.registerUpdate((newColor -> {
            int blue = (int) (double) newColor;
            this.setRawState((this.getRawState() & 0xFFFFFF00) + blue);
            this.updateState();
        }));
        this.sliderAlpha.registerUpdate((newColor -> {
            int alpha = (int) (double) newColor;
            this.setRawState((alpha << 24) + (this.getRawState() & 0xFFFFFF));
            this.updateState();
        }));
        return this;
    }

    public void registerAsOption(String key) {
        this.toggle.registerAsOption(key);
        this.toggle.moduleStateCallback = this.moduleStateCallback;

        String colorKey = key + ".color";
        this.registerUpdate(newState -> {
            this.setRawState(newState);
            FileSystem.updateState(colorKey, newState);
        });
        FileSystem.registerUpdate(colorKey, newState -> {
            if (!(newState instanceof Integer integerState)) return;
            this.setRawState(integerState);
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

    public void registerUpdate(Consumer<Integer> callback) {
        this.colorCallback = callback;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        this.toggle.withXY(left, top).withWH(width, height).renderWidget(context, mouseX, mouseY, deltaTicks);

        int colorBoxWidth = (bottom - top) - 6;
        context.fill(right - 3 - colorBoxWidth, top + 3, right - 3, bottom - 3, this.getRawState());

        if (dropdownVisible) {
            context.fill(right - 3, top, right - 1, bottom, CigaretteScreen.SECONDARY_COLOR);
            for (BaseWidget child : children) {
                if (child == null) continue;
                child.withXY(right, top).renderWidget(context, mouseX, mouseY, deltaTicks);
            }
        }

    }

    @Override
    public ToggleColorWidget buildModule(String message, @Nullable String tooltip) {
        return new ToggleColorWidget(Text.of(message), tooltip == null ? null : Text.of(tooltip), true);
    }
}
