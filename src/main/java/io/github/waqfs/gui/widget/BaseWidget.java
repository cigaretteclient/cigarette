package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class BaseWidget<StateType> extends ClickableWidget {
    private StateType defaultState;
    private StateType state;
    protected boolean captureHover = false;
    protected boolean hovered = false;
    protected String configKey;
    private final TooltipState tooltip = new TooltipState();
    protected @Nullable Consumer<StateType> stateCallback = null;
    protected @Nullable Consumer<Object> fsCallback = null;
    protected @Nullable Consumer<StateType> moduleCallback = null;

    public BaseWidget(Text message, @Nullable Text tooltip) {
        super(0, 0, 0, 0, message);
        if (tooltip != null) this.setTooltip(Tooltip.of(tooltip));
    }

    public final void setRawState(StateType state) {
        this.state = state;
        if (moduleCallback != null) moduleCallback.accept(this.state);
        if (stateCallback != null) stateCallback.accept(this.state);
    }

    @SuppressWarnings("unchecked")
    public final void toggleRawState() {
        if (this.state instanceof Boolean booleanState) {
            this.setRawState((StateType) (Boolean) !booleanState);
            return;
        }
        throw new IllegalStateException("Cannot toggle state from a non-boolean component.");
    }

    public final StateType getRawState() {
        if (this.state instanceof Stateless) throw new IllegalStateException("Cannot get state from a stateless component.");
        return this.state;
    }

    public void registerModuleCallback(Consumer<StateType> callback) {
        this.moduleCallback = callback;
    }

    @SuppressWarnings("unchecked")
    private void defaultFSCallback(Object newState) {
        try {
            StateType typedState = (StateType) newState;
            this.state = typedState;
            if (this.moduleCallback != null) this.moduleCallback.accept(typedState);
        } catch (ClassCastException ignored) {
        }
    }

    public void registerConfigKey(String key) {
        if (this.state instanceof Stateless) return;
        if (this.configKey != null) throw new IllegalStateException("Cannot configure a config key more than once.");
        this.configKey = key;
        this.stateCallback = newState -> FileSystem.updateState(key, newState);
        this.fsCallback = this::defaultFSCallback;
        FileSystem.registerUpdate(key, this.fsCallback);
    }

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

    protected BaseWidget<StateType> captureHover() {
        this.captureHover = true;
        return this;
    }

    @Override
    public void setTooltip(Tooltip tooltip) {
        this.tooltip.setTooltip(tooltip);
    }

    public BaseWidget<StateType> withXY(int x, int y) {
        this.setX(x);
        this.setY(y);
        return this;
    }

    public BaseWidget<StateType> withWH(int w, int h) {
        this.setWidth(w);
        this.setHeight(h);
        return this;
    }

    public BaseWidget<StateType> withDefault(StateType state) {
        this.defaultState = state;
        this.state = state;
        return this;
    }

    public void _render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (!this.visible) return;
        this.hovered = captureHover && isMouseOver(mouseX, mouseY) && CigaretteScreen.isHoverable(this);
        this.render(context, this.hovered, mouseX, mouseY, deltaTicks, getX(), getY(), getRight(), getBottom());
        if (this.hovered) this.tooltip.render(true, this.isFocused(), this.getNavigationFocus());
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this._render(context, mouseX, mouseY, deltaTicks);
    }

    protected abstract void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom);

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    public static class Stateless {
    }
}
