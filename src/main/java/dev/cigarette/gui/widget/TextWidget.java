package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.Nullable;

/**
 * A widget that only renders text.
 */
public class TextWidget extends BaseWidget<BaseWidget.Stateless> {
    /**
     * Whether a line should be rendered at the bottom of this widget's bounding box.
     */
    private boolean underlined = false;
    /**
     * Whether the text should be centered inside this widget's bounding box.
     */
    private boolean centered = true;

    /**
     * Creates a widget whose only purpose is to render text and a tooltip.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public TextWidget(int x, int y, int width, int height, String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    /**
     * Creates a widget whose only purpose is to render text and a tooltip.
     *
     * @param x       The initial X position of this widget
     * @param y       The initial Y position of this widget
     * @param width   The initial width of this widget
     * @param height  The initial height of this widget
     * @param message The text to display inside this widget
     */
    public TextWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    /**
     * Creates a widget whose only purpose is to render text and a tooltip.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public TextWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.captureHover();
    }

    /**
     * Creates a widget whose only purpose is to render text and a tooltip.
     *
     * @param message The text to display inside this widget
     */
    public TextWidget(String message) {
        super(message, null);
        this.captureHover();
    }

    /**
     * Sets this widget to render a line at the bottom of its bounding box.
     *
     * @return This for method chaining
     */
    public TextWidget withUnderline() {
        underlined = true;
        return this;
    }

    /**
     * Sets whether this widget should render its text centered in its bounding box.
     *
     * @param center Whether the text should be centered
     * @return This for method chaining
     */
    public TextWidget centered(boolean center) {
        this.centered = center;
        return this;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (drawBackground) {
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

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
