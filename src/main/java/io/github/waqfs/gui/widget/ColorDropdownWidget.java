package io.github.waqfs.gui.widget;

import io.github.waqfs.module.BaseModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class ColorDropdownWidget<Widget extends BaseWidget<StateType>, StateType> extends DropdownWidget<Widget, StateType> {
    private final ColorSquareWidget colorSquare = new ColorSquareWidget();
    private final SliderWidget sliderRed = new SliderWidget(Text.literal("Red")).withBounds(0, 255, 255);
    private final SliderWidget sliderGreen = new SliderWidget(Text.literal("Green")).withBounds(0, 255, 255);
    private final SliderWidget sliderBlue = new SliderWidget(Text.literal("Blue")).withBounds(0, 255, 255);
    private final SliderWidget sliderAlpha = new SliderWidget(Text.literal("Alpha")).withBounds(0, 255, 255);

    public int getStateARGB() {
        return this.colorSquare.getRawState();
    }

    public int getStateRGBA() {
        return ((this.colorSquare.getRawState() & 0xFFFFFF) << 8) + ((this.colorSquare.getRawState() >> 24) & 0xFF);
    }

    public int getStateRGB() {
        return this.colorSquare.getRawState() & 0xFFFFFF;
    }

    public boolean getToggleState() {
        if (this.header instanceof ToggleWidget) {
            return ((ToggleWidget) this.header).getRawState();
        }
        throw new IllegalStateException("Cannot get boolean state on a stateless header in a dropdown widget.");
    }

    @SuppressWarnings("unchecked")
    public ColorDropdownWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        super.withIndicator(false);
        this.setHeader((Widget) new ToggleWidget(message, tooltip));
        this.attachChildren().captureHover();
    }

    public ColorDropdownWidget<Widget, StateType> withDefaultColor(int argb) {
        this.colorSquare.withDefault(argb);
        sliderAlpha.withDefault((double) ((argb >> 24) & 0xFF));
        sliderRed.withDefault((double) ((argb >> 16) & 0xFF));
        sliderGreen.withDefault((double) ((argb >> 8) & 0xFF));
        sliderBlue.withDefault((double) (argb & 0xFF));
        return this;
    }

    public ColorDropdownWidget<Widget, StateType> withDefaultState(StateType state) {
        this.header.withDefault(state);
        return this;
    }

    public ColorDropdownWidget<Widget, StateType> withAlpha(boolean alpha) {
        this.sliderAlpha.visible = alpha;
        if (!alpha) this.sliderAlpha.withDefault(255d);
        return this;
    }

    private ColorDropdownWidget<Widget, StateType> attachChildren() {
        this.container.setChildren(this.sliderRed, this.sliderGreen, this.sliderBlue, this.sliderAlpha);
        this.sliderRed.stateCallback = ((newColor -> {
            int red = (int) (double) newColor;
            this.colorSquare.setRawState((this.colorSquare.getRawState() & 0xFF00FFFF) + (red << 16));
        }));
        this.sliderGreen.stateCallback = ((newColor -> {
            int green = (int) (double) newColor;
            this.colorSquare.setRawState((this.colorSquare.getRawState() & 0xFFFF00FF) + (green << 8));
        }));
        this.sliderBlue.stateCallback = ((newColor -> {
            int blue = (int) (double) newColor;
            this.colorSquare.setRawState((this.colorSquare.getRawState() & 0xFFFFFF00) + blue);
        }));
        this.sliderAlpha.stateCallback = ((newColor -> {
            int alpha = (int) (double) newColor;
            this.colorSquare.setRawState((alpha << 24) + (this.colorSquare.getRawState() & 0xFFFFFF));
        }));
        return this;
    }

    public static BaseModule.GeneratedWidgets<ToggleWidget, Boolean> module(Text displayName, @Nullable Text tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return new BaseModule.GeneratedWidgets<>(wrapper, widget);
    }

    public static ColorDropdownWidget<ToggleWidget, Boolean> buildToggle(Text displayName, @Nullable Text tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return wrapper;
    }

    public static ColorDropdownWidget<TextWidget, Stateless> buildText(Text displayName, @Nullable Text tooltip) {
        ColorDropdownWidget<TextWidget, Stateless> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        TextWidget widget = new TextWidget(displayName, tooltip).centered(false);
        wrapper.setHeader(widget);
        return wrapper;
    }

    @Override
    public void registerConfigKey(String key) {
        this.header.registerConfigKey(key);
        this.colorSquare.registerConfigKeyAnd(key + ".color", loadedState -> {
            if (!(loadedState instanceof Integer integerState)) return;
            this.withDefaultColor(integerState);
        });
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        super.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
        this.colorSquare.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
    }
}
