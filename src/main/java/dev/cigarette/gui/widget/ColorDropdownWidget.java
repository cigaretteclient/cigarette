package dev.cigarette.gui.widget;

import dev.cigarette.module.BaseModule;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

public class ColorDropdownWidget<Widget extends BaseWidget<StateType>, StateType> extends DropdownWidget<Widget, StateType> {
    /**
     * Additional header widget to render the color state from the sliders.
     */
    private final ColorSquareWidget colorSquare = new ColorSquareWidget();
    /**
     * The slider that controls the red content in the color.
     */
    private final SliderWidget sliderRed = new SliderWidget("Red").withBounds(0, 255, 255);
    /**
     * The slider that controls the green content in the color.
     */
    private final SliderWidget sliderGreen = new SliderWidget("Green").withBounds(0, 255, 255);
    /**
     * The slider that controls the blue content in the color.
     */
    private final SliderWidget sliderBlue = new SliderWidget("Blue").withBounds(0, 255, 255);
    /**
     * The slider that controls the alpha content in the color.
     */
    private final SliderWidget sliderAlpha = new SliderWidget("Alpha").withBounds(0, 255, 255);

    /**
     * {@return the color state in ARGB format}
     */
    public int getStateARGB() {
        return this.colorSquare.getRawState();
    }

    /**
     * {@return the color state in RGBA format}
     */
    public int getStateRGBA() {
        return ((this.colorSquare.getRawState() & 0xFFFFFF) << 8) + ((this.colorSquare.getRawState() >> 24) & 0xFF);
    }

    /**
     * {@return the color state in RGB format}
     */
    public int getStateRGB() {
        return this.colorSquare.getRawState() & 0xFFFFFF;
    }

    /**
     * {@return the toggled state of this widget}
     *
     * @throws IllegalStateException If the header of this widget is not an instance of {@code ToggleWidget}
     */
    public boolean getToggleState() {
        if (this.header instanceof ToggleWidget) {
            return ((ToggleWidget) this.header).getRawState();
        }
        throw new IllegalStateException("Cannot get boolean state on a stateless header in a dropdown widget.");
    }

    /**
     * Creates a dropdown widget pre-made for color configuration. Has a {@code ToggleWidget} and {@code ColorSquareWidget} header, and {@code SliderWidget} children for customizing the selected color.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    @SuppressWarnings("unchecked")
    public ColorDropdownWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        super.withIndicator(false);
        this.setHeader((Widget) new ToggleWidget(message, tooltip));
        this.attachChildren().captureHover();
    }

    /**
     * Sets the color state and stored default color state of this widget.
     *
     * @param argb The default state to set in ARGB format
     * @return This widget for method chaining
     */
    public ColorDropdownWidget<Widget, StateType> withDefaultColor(int argb) {
        this.colorSquare.withDefault(argb);
        sliderAlpha.withDefault((double) ((argb >> 24) & 0xFF));
        sliderRed.withDefault((double) ((argb >> 16) & 0xFF));
        sliderGreen.withDefault((double) ((argb >> 8) & 0xFF));
        sliderBlue.withDefault((double) (argb & 0xFF));
        return this;
    }

    /**
     * Sets the state and stored default state of the heading {@code ToggleWidget}.
     *
     * @param state The default state to set
     * @return This widget for method chaining
     */
    public ColorDropdownWidget<Widget, StateType> withDefaultState(StateType state) {
        this.header.withDefault(state);
        return this;
    }

    /**
     * Sets whether this widget should include an alpha slider.
     *
     * @param alpha Whether an alpha slider should be included
     * @return This widget for method chaining
     */
    public ColorDropdownWidget<Widget, StateType> withAlpha(boolean alpha) {
        this.sliderAlpha.disabled = !alpha;
        if (!alpha) this.sliderAlpha.withDefault(255d);
        return this;
    }

    /**
     * Attaches the children to the dropdown menu and binds callbacks when sliders are moved.
     *
     * @return This widget for method chaining
     */
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

    /**
     * Generator for modules using this as a top-level widget. Creates a togglable {@code ColorDropdownWidget}.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     * @return A {@code GeneratedWidgets} object for use in {@code BaseModule} constructing
     */
    public static BaseModule.GeneratedWidgets<ToggleWidget, Boolean> module(String displayName, @Nullable String tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return new BaseModule.GeneratedWidgets<>(wrapper, widget);
    }

    /**
     * Creates and returns a new togglable {@code ColorDropdownWidget}.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     * @return the new widget with a {@code ToggleWidget} attached as the header
     */
    public static ColorDropdownWidget<ToggleWidget, Boolean> buildToggle(String displayName, @Nullable String tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return wrapper;
    }

    /**
     * Creates and returns a new {@code ColorDropdownWidget} with only the color configuration, no toggling.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     * @return thw new widget with a {@code TextWidget} attached as the header
     */
    public static ColorDropdownWidget<TextWidget, Stateless> buildText(String displayName, @Nullable String tooltip) {
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
