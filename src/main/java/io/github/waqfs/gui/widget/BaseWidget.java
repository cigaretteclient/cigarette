package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.tooltip.TooltipState;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class BaseWidget extends ClickableWidget {
    protected boolean hovered = false;
    private final TooltipState tooltip = new TooltipState();

    public BaseWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip) {
        super(x, y, width, height, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public BaseWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public BaseWidget(Text message, @Nullable Text tooltip) {
        super(0, 0, 0, 0, message);
        this.setTooltip(Tooltip.of(tooltip));
    }

    public BaseWidget(Text message) {
        super(0, 0, 0, 0, message);
    }

    @Override
    public void setTooltip(Tooltip tooltip) {
        this.tooltip.setTooltip(tooltip);
    }

    public void _render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (!this.visible) return;
        this.hovered = isMouseOver(mouseX, mouseY) && CigaretteScreen.isHoverable(this);
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
}
