package io.github.waqfs.gui.hud.wmark;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.widget.DraggableWidget;
import io.github.waqfs.lib.Color;
import io.github.waqfs.lib.Shape;
import io.github.waqfs.gui.hud.notification.NotificationDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;

public class WatermarkDisplay extends ClickableWidget {
    public static boolean TEXT_ENABLED = true;
    public static boolean SIMPLE_DISPLAY = false;

    public WatermarkDisplay() {

        super(5, 5, 200, 40, Text.of("Watermark"));
    }

    public static void watermarkFullRender(DrawContext context, int x, int y, boolean textEnabled,
            boolean simpleDisplay) {
        if (simpleDisplay) {
            int c = Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
            final int radius = 6;
            if (textEnabled) {
                final int logoSize = 24;
                final int padLeft = 7;
                final int padRight = 8;
                final int spacing = 7;
                final int boxHeight = 28;

                TextRenderer tr = Cigarette.REGULAR != null ? Cigarette.REGULAR
                        : MinecraftClient.getInstance().textRenderer;
                int textWidth = tr.getWidth("cigarette");
                int boxWidth = padLeft + logoSize + spacing + textWidth + padRight;

                Shape.roundedRect(context, x, y, x + boxWidth, y + boxHeight, c, radius);

                int logoY = y + Math.max(0, (boxHeight - logoSize) / 2);
                int logoX = x + padLeft;

                context.getMatrices().push();
                context.getMatrices().translate(logoX, logoY, 0);
                float s = 24f / 30f;
                context.getMatrices().scale(s, s, 1f);
                context.drawTexture(RenderLayer::getGuiTextured, Cigarette.LOGO_IDENTIFIER, 0, 0, 0f, 0f, 30, 30, 30,
                        30);
                context.getMatrices().pop();

                int textY = y + Math.max(0, (boxHeight - tr.fontHeight) / 2);
                int textX = x + padLeft + logoSize + spacing;
                context.drawText(tr, "cigarette", textX, textY, CigaretteScreen.PRIMARY_TEXT_COLOR, true);
            } else {
                final int logoSize = 24;
                final int boxSize = 28;
                Shape.roundedRect(context, x, y, x + boxSize, y + boxSize, c, radius);
                int logoY = y + Math.max(0, (boxSize - logoSize) / 2);
                int logoX = x + Math.max(0, (boxSize - logoSize) / 2);

                context.getMatrices().push();
                context.getMatrices().translate(logoX, logoY, 0);
                float s = 24f / 30f;
                context.getMatrices().scale(s, s, 1f);
                context.drawTexture(RenderLayer::getGuiTextured, Cigarette.LOGO_IDENTIFIER, 0, 0, 0f, 0f, 30, 30, 30,
                        30);
                context.getMatrices().pop();
            }
        } else {
            if (textEnabled) {
                NotificationDisplay.imageRender(context, x, y);
            } else {
                int logoY = y + 5;
                context.drawTexture(RenderLayer::getGuiTextured, Cigarette.LOGO_IDENTIFIER, x + 7, logoY, 0f, 0f, 30,
                        30, 30, 30);
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {

        watermarkFullRender(context, this.getX(), this.getY(), TEXT_ENABLED, SIMPLE_DISPLAY);
    }
}