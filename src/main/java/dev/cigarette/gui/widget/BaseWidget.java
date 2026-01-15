package dev.cigarette.gui.widget;

import dev.cigarette.config.FileSystem;
import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Extends the base widget providing default functionality for saving/loading custom state from the config.
 *
 * @param <StateType> The custom state this widget stores. Use {@link BaseWidget.Stateless} for widgets that should not hold state.
 */
public abstract class BaseWidget<StateType> extends ClickableWidget {
    /**
     * The default state of this widget for resetting.
     */
    private StateType defaultState;
    /**
     * The current state of this widget.
     */
    private StateType state;
    /**
     * Whether this widget is focused by the user.
     */
    protected boolean focused = false;
    /**
     * Whether this widget is capturing hover events to render tooltips.
     */
    protected boolean captureHover = false;
    /**
     * Whether this widget is currently hovered by the mouse.
     */
    protected boolean hovered = false;
    /**
     * The registered config key for saving and loading this widget's state.
     */
    protected String configKey;
    /**
     * The tooltip state of this widget.
     */
    private final TooltipState tooltip = new TooltipState();
    /**
     * A callback triggered when the state changes. For registered widgets, this is used to write the new state to the config.
     */
    protected @Nullable Consumer<StateType> stateCallback = null;
    /**
     * A callback triggered when the config is loaded setting the state. This widget must be registered before the config loads.
     */
    protected @Nullable Consumer<Object> fsCallback = null;
    /**
     * A callback triggered when the state changes. Used primarily to trigger module {@code whenEnabled} and {@code whenDisabled} events.
     */
    protected @Nullable Consumer<StateType> moduleCallback = null;

    /**
     * Set the state callback for this widget.
     * @param callback The callback to trigger when the state changes
     */
    public void setStateCallback(Consumer<StateType> callback) {
        this.stateCallback = callback;
    }

    /**
     * Create a basic widget that holds custom state and saves/loads it from the cigarette config.
     *
     * @param message The text to display when this widget is rendered
     * @param tooltip The text to display when this widget is hovered
     */
    public BaseWidget(String message, @Nullable String tooltip) {
        super(0, 0, 0, 0, message == null ? Text.empty() : Text.literal(message));
        if (tooltip != null) this.setTooltip(Tooltip.of(Text.literal(tooltip)));
    }

    /**
     * {@return whether this widget is holding custom state}
     */
    public boolean isStateless() {
        return this.state instanceof Stateless;
    }

    /**
     * Sets the state of this widget and calls any attached module and state callbacks. Those callbacks trigger module {@code whenEnabled} and {@code whenDisabled} events (given that this is a boolean widget), and writes the new state to the config.
     *
     * @param state The new state to set to this widget
     */
    public final void setRawState(StateType state) {
        this.state = state;
        if (moduleCallback != null) moduleCallback.accept(this.state);
        if (stateCallback != null) stateCallback.accept(this.state);
    }

    /**
     * Toggles the state of this widget and calls any attached module and state callbacks. Those callbacks trigger module {@code whenEnabled} and {@code whenDisabled} events, and writes the new state to the config.
     *
     * @throws IllegalStateException If this widget is not of a boolean state type
     */
    @SuppressWarnings("unchecked")
    public final void toggleRawState() {
        if (this.state instanceof Boolean booleanState) {
            this.setRawState((StateType) (Boolean) !booleanState);
            return;
        }
        throw new IllegalStateException("Cannot toggle state from a non-boolean component.");
    }

    /**
     * {@return the custom state}
     *
     * @throws IllegalStateException If this widget is a stateless widget
     */
    public final StateType getRawState() {
        if (this.state instanceof Stateless) throw new IllegalStateException("Cannot get state from a stateless component.");
        return this.state;
    }

    /**
     * Binds a callback that is triggered when the state is set. Most often used in modules to trigger {@code whenEnabled} and {@code whenDisabled} events.
     * <p>Note that there are no checks on whether the new state is different from the previous state.</p>
     *
     * @param callback The callback that accepts the newest state
     */
    public void registerModuleCallback(Consumer<StateType> callback) {
        this.moduleCallback = callback;
    }

    /**
     * The default callback attached to this widget when it is initially registered to the config. This callback receives the state of this widget from the config once it is loaded by the client. Triggers the module state callback which trigger module {@code whenEnabled} and {@code whenDisabled} events.
     *
     * @param newState The new state to set
     */
    @SuppressWarnings("unchecked")
    private void defaultFSCallback(Object newState) {
        try {
            StateType typedState = (StateType) newState;
            this.state = typedState;
            if (this.moduleCallback != null) this.moduleCallback.accept(typedState);
        } catch (ClassCastException ignored) {
        }
    }

    /**
     * Registers this widget to the config so it can save/load its state.
     *
     * @param key The key in the config to store the value of this widgets state under
     */
    public void registerConfigKey(String key) {
        if (this.state instanceof Stateless) return;
        if (this.configKey != null) throw new IllegalStateException("Cannot configure a config key more than once.");
        this.configKey = key;
        this.stateCallback = newState -> FileSystem.updateState(key, newState);
        this.fsCallback = this::defaultFSCallback;
        FileSystem.registerUpdate(key, this.fsCallback);
    }

    /**
     * Registers this widget to the config so it can save/load its state. This method has an additional callback that can be defined which will receive the state of the widget once it is loaded by the client.
     *
     * @param key         The key in the config to store the value of this widgets state under
     * @param loadedState A callback which receives the state from the config once loaded by the client
     */
    public void registerConfigKeyAnd(String key, Consumer<Object> loadedState) {
        if (this.state instanceof Stateless) return;
        if (this.configKey != null) throw new IllegalStateException("Cannot configure a config key more than once.");
        this.configKey = key;
        this.stateCallback = newState -> FileSystem.updateState(key, newState);
        this.fsCallback = newState -> {
            this.defaultFSCallback(newState);
            loadedState.accept(newState);
        };
        FileSystem.registerUpdate(key, this.fsCallback);
    }

    /**
     * Sets this widget to capture hovering events. This is required to be set for tooltips to be rendered.
     *
     * @return This widget for method chaining
     */
    protected BaseWidget<StateType> captureHover() {
        this.captureHover = true;
        return this;
    }

    /**
     * Sets this widget's focused state.
     *
     * @param state The focused state to set
     */
    public void setFocused(boolean state) {
        this.focused = state;
    }

    /**
     * Sets this widget to be focused.
     */
    public void setFocused() {
        this.focused = true;
    }

    /**
     * Sets this widget to be unfocused.
     */
    public void unfocus() {
        this.focused = false;
    }

    /**
     * Sets the hover tooltip of this widget. The tooltip will only be rendered if {@link #captureHover()} was called allowing this widget to capture hovering events.
     * <p>For basic tooltips, you can use {@code Tooltip.of(Text.literal(String))} to convert a String to a Tooltip.</p>
     *
     * @param tooltip The tooltip
     */
    @Override
    public void setTooltip(Tooltip tooltip) {
        this.tooltip.setTooltip(tooltip);
    }

    /**
     * Sets the position of this widget.
     *
     * @param x The distance from the left edge of the screen
     * @param y The distance from the top edge of the screen
     * @return This widget for method chaining
     */
    public BaseWidget<StateType> withXY(int x, int y) {
        this.setX(x);
        this.setY(y);
        return this;
    }

    /**
     * Sets the width and height of this widget.
     *
     * @param w The width of the widget
     * @param h The height of the widget
     * @return This widget for method chaining
     */
    public BaseWidget<StateType> withWH(int w, int h) {
        this.setWidth(w);
        this.setHeight(h);
        return this;
    }

    /**
     * Sets the state and stored default state of this widget.
     *
     * @param state The default state to set
     * @return This widget for method chaining
     */
    public BaseWidget<StateType> withDefault(StateType state) {
        this.defaultState = state;
        this.state = state;
        return this;
    }

    /**
     * Replacement method for the {@link ClickableWidget#render(DrawContext, int, int, float) ClickableWidget.render()} method that cannot be overridden. Automatically handles rendering tooltips when this widget is hovered and the top-most widget. Also pulls the bounding box of this widget and supplies the values to the {@link #render(DrawContext, boolean, int, int, float, int, int, int, int) render()} method for cleaner code.
     *
     * @param context    The draw context to pass through
     * @param mouseX     The mouse X position to pass through
     * @param mouseY     The mouse Y position to pass through
     * @param deltaTicks The delta ticks to pass through
     */
    public void _render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (!this.visible) return;
        this.hovered = captureHover && isMouseOver(mouseX, mouseY) && CigaretteScreen.isHoverable(this);
        this.render(context, this.hovered, mouseX, mouseY, deltaTicks, getX(), getY(), getRight(), getBottom());
        if (this.hovered) this.tooltip.render(context, mouseX, mouseY, true, this.isFocused(), this.getNavigationFocus());
    }

    /**
     * Alias that calls {@link #_render(DrawContext, int, int, float) _render()} but looks nicer.
     * <p>Automatically handles rendering tooltips when this widget is hovered and the top-most widget. Also pulls the bounding box of this widget and supplies the values to the {@link #render(DrawContext, boolean, int, int, float, int, int, int, int) render()} method for cleaner code.</p>
     *
     * @param context    The draw context to pass through
     * @param mouseX     The mouse X position to pass through
     * @param mouseY     The mouse Y position to pass through
     * @param deltaTicks The delta ticks to pass through
     */
    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this._render(context, mouseX, mouseY, deltaTicks);
    }

    /**
     * The custom rendering method that replaces the built-in {@link ClickableWidget#render(DrawContext, int, int, float) ClickableWidget.render()} method.
     *
     * @param context    The current draw context
     * @param hovered    Whether this widget is hovered by the mouse
     * @param mouseX     Current mouse X position
     * @param mouseY     Current mouse Y position
     * @param deltaTicks Current delta ticks
     * @param left       Distance to the left edge of this widget from the left of the screen
     * @param top        Distance to the top edge of this widget from the top of the screen
     * @param right      Distance to the right edge of this widget from the left of the screen
     * @param bottom     Distance to the bottom edge of this widget from the top of the screen
     */
    protected abstract void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom);

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    /**
     * Empty class for widgets that do not hold any custom state.
     */
    public static class Stateless {
    }
}
