package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class TextWidget extends BaseWidget<BaseWidget.Stateless> {
    private boolean underlined = false;
    private boolean centered = true;

    public TextWidget(int x, int y, int width, int height, Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    public TextWidget(int x, int y, int width, int height, Text message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    public TextWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.captureHover();
    }

    public TextWidget(Text message) {
        super(message, null);
        this.captureHover();
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
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);

        TextRenderer textRenderer = Cigarette.REGULAR;
        if (this.centered) {
            textRenderer = Cigarette.REGULAR;
            int leftMargin = (width - textRenderer.getWidth(getMessage())) / 2;
            context.drawTextWithShadow(textRenderer, getMessage(), left + leftMargin, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);
        } else {
            context.drawTextWithShadow(textRenderer, getMessage(), left + 4, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);
        }
        if (this.underlined) context.drawHorizontalLine(left + 3, right - 3, bottom - 1, CigaretteScreen.SECONDARY_COLOR);
    }
}
