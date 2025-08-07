package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class TextWidget extends ClickableWidget {
    private boolean underlined = false;
    private boolean centered = true;

    public TextWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip) {
        super(x, y, width, height, message);
        if (tooltip != null) this.setTooltip(Tooltip.of(tooltip));
    }

    public TextWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public TextWidget(Text message, @Nullable Text tooltip) {
        super(0, 0, 0, 0, message);
        if (tooltip != null) this.setTooltip(Tooltip.of(tooltip));
    }

    public TextWidget(Text message) {
        super(0, 0, 0, 0, message);
    }

    public TextWidget withUnderline() {
        underlined = true;
        return this;
    }

    public TextWidget centered(boolean center) {
        this.centered = center;
        return this;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int left = getX();
        int right = getRight();
        int top = getY();
        int bottom = getBottom();

        context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);

        if (this.centered) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int leftMargin = (width - textRenderer.getWidth(getMessage())) / 2;
            context.drawTextWithShadow(textRenderer, getMessage(), left + leftMargin, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);
        } else {
            context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);
        }
        if (this.underlined) context.drawHorizontalLine(left + 3, right - 3, bottom - 1, CigaretteScreen.SECONDARY_COLOR);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
