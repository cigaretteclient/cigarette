package dev.cigarette.gui.widget;

import dev.cigarette.module.BaseModule;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

/**
 * An extension on {@link DropdownWidget} pre-made for color configuration.
 *
 * @param <Widget>    The type of children this widget stores. Use {@code Widget extends BaseWidget<?>} to allow any types as children.
 * @param <StateType> The custom state this widget stores. Use {@link BaseWidget.Stateless} for widgets that should not hold state.
 */
public class ColorDropdownWidget<Widget extends BaseWidget<StateType>, StateType> extends DropdownWidget<Widget, StateType> {
    /**
     * Additional header widget to render the color state from the sliders.
     */
    private final ColorSquareWidget colorSquare = new ColorSquareWidget();
    /**
     * The color wheel widget for hue selection.
     */
    private final ColorWheelWidget colorWheel = new ColorWheelWidget();
    /**
     * The slider that controls the saturation of the color.
     */
    private final SliderWidget sliderSaturation = new SliderWidget("Saturation").withBounds(0, 50, 100);
    /**
     * The slider that controls the lightness of the color.
     */
    private final SliderWidget sliderLightness = new SliderWidget("Lightness").withBounds(0, 50, 100);
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
     * @throws IllegalStateException If the header of this widget is not an instance of {@link ToggleWidget}
     */
    public boolean getToggleState() {
        if (this.header instanceof ToggleWidget) {
            return ((ToggleWidget) this.header).getRawState();
        }
        throw new IllegalStateException("Cannot get boolean state on a stateless header in a dropdown widget.");
    }

    /**
     * Creates a dropdown widget pre-made for color configuration. Has a {@link ToggleWidget} and {@link ColorSquareWidget} header, and {@link SliderWidget} children for customizing the selected color.
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
        this.colorWheel.withDefault(argb);
        
        double[] sl = ColorWheelWidget.rgbToSaturationLightness(argb);
        sliderSaturation.withDefault(sl[0]);
        sliderLightness.withDefault(sl[1]);
        sliderAlpha.withDefault((double) ((argb >> 24) & 0xFF));
        
        this.colorWheel.setSaturation(sl[0]);
        this.colorWheel.setLightness(sl[1]);
        
        return this;
    }

    /**
     * Sets the state and stored default state of the heading {@link ToggleWidget}.
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
        this.container.setChildren(this.colorWheel, this.sliderSaturation, this.sliderLightness, this.sliderAlpha);
        
        this.colorWheel.stateCallback = (newColor -> {
            int alpha = (int) (double) sliderAlpha.getRawState();
            this.colorSquare.setRawState((alpha << 24) | (newColor & 0xFFFFFF));
        });
        
        this.sliderSaturation.stateCallback = (newSat -> {
            this.colorWheel.setSaturation(newSat);
            int alpha = (int) (double) sliderAlpha.getRawState();
            this.colorSquare.setRawState((alpha << 24) | (this.colorWheel.getRawState() & 0xFFFFFF));
        });
        
        this.sliderLightness.stateCallback = (newLight -> {
            this.colorWheel.setLightness(newLight);
            int alpha = (int) (double) sliderAlpha.getRawState();
            this.colorSquare.setRawState((alpha << 24) | (this.colorWheel.getRawState() & 0xFFFFFF));
        });
        
        this.sliderAlpha.stateCallback = (newAlpha -> {
            int alpha = (int) (double) newAlpha;
            this.colorSquare.setRawState((alpha << 24) | (this.colorWheel.getRawState() & 0xFFFFFF));
        });
        
        return this;
    }

    /**
     * Generator for modules using this as a top-level widget. Creates a togglable {@link ColorDropdownWidget}.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip     The tooltip to render when this widget is hovered
     * @return A {@link BaseModule.GeneratedWidgets} object for use in {@link BaseModule} constructing
     */
    public static BaseModule.GeneratedWidgets<ToggleWidget, Boolean> module(String displayName, @Nullable String tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return new BaseModule.GeneratedWidgets<>(wrapper, widget);
    }

    /**
     * Creates and returns a new togglable {@link ColorDropdownWidget}.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip     The tooltip to render when this widget is hovered
     * @return the new widget with a {@link ToggleWidget} attached as the header
     */
    public static ColorDropdownWidget<ToggleWidget, Boolean> buildToggle(String displayName, @Nullable String tooltip) {
        ColorDropdownWidget<ToggleWidget, Boolean> wrapper = new ColorDropdownWidget<>(displayName, tooltip);
        ToggleWidget widget = new ToggleWidget(displayName, tooltip);
        wrapper.setHeader(widget);
        return wrapper;
    }

    /**
     * Creates and returns a new {@link ColorDropdownWidget} with only the color configuration, no toggling.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip     The tooltip to render when this widget is hovered
     * @return thw new widget with a {@link TextWidget} attached as the header
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
        this.colorWheel.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
    }
}
