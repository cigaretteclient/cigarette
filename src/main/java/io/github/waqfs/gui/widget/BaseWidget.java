package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class BaseWidget<StateType> extends ClickableWidget {
    private StateType defaultState;
    private StateType state;
    protected boolean captureHover = false;
    protected boolean hovered = false;
    private final TooltipState tooltip = new TooltipState();

    public BaseWidget(Text message, @Nullable Text tooltip) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public final void setRawState(StateType state) {
        this.state = state;
    }

    public final StateType getRawState() {
        if (this.state instanceof Stateless) throw new IllegalStateException("Cannot get state from a stateless component.");
        return this.state;
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
