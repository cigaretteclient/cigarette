package io.github.waqfs.gui.hud.notification;

import java.util.ArrayList;
import java.util.List;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.hud.notification.internal.NotificationWithEasingProgress;
import io.github.waqfs.lib.Color;
import io.github.waqfs.lib.Shape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

public class NotificationDisplay extends ClickableWidget {
    public List<NotificationWithEasingProgress> notifications = new ArrayList<>();

    public NotificationDisplay() {
        super(0, 0, 200, 20, Text.of("Notifications"));

        Window w = MinecraftClient.getInstance().getWindow();
        this.setX(w.getScaledWidth() - 200);
        this.setY(0);
        this.setDimensions(200, w.getScaledHeight());

        Cigarette.EVENTS.registerListener(Notification.class, (event) -> {
            handleNotification(event);
            return null;
        });
    }

    @Override
    public void setX(int x) {
        Window w = MinecraftClient.getInstance().getWindow();
        if (x + this.getWidth() > w.getScaledWidth()) {
            x = w.getScaledWidth() - this.getWidth();
        }
        super.setX(x);
    }

    private <T> void handleNotification(T event) {
        if (event instanceof Notification) {
            Notification notification = (Notification) event;

            notifications.add(0, new NotificationWithEasingProgress(notification));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    public static void imageRender(DrawContext context, int x, int y) {
        context.drawTexture(
                RenderLayer::getGuiTextured,
                Cigarette.LOGO_IDENTIFIER, x + 7, y, 0f, 0f, 30, 30, 30, 30);

        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < "cigarette".length(); i++) {
            colors.add(Color.color(x + i * 10, y + 40));
        }
        drawTextGradient(context, "cigarette", x, y + 40, colors.stream().mapToInt(i -> i).toArray());
    }

    public static void imageRender(DrawContext context, int x, int y, double gradientStaggerModifier) {
        context.drawTexture(
                RenderLayer::getGuiTextured,
                Cigarette.LOGO_IDENTIFIER, x + 7, y, 0f, 0f, 30, 30, 30, 30);
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < "cigarette".length(); i++) {
            colors.add(Color.color((int) (x + i * 10 * gradientStaggerModifier),
                    (int) (y + 40 * gradientStaggerModifier)));
        }
        drawTextGradient(context, "cigarette", x, y + 30, colors.stream().mapToInt(i -> i).toArray());
    }

    public static void drawTextGradient(DrawContext context, String text, int x, int y, int[] colors) {
        int w = 0;
        for (int i = 0; i < colors.length; i++) {
            char c = text.charAt(i);
            int width = Cigarette.REGULAR.getWidth(String.valueOf(c));
            context.drawText(Cigarette.REGULAR, String.valueOf(c), x + w, y, colors[i], true);
            w += width;
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        notifications.forEach(n -> n.updateProgress(deltaTicks));
        notifications.removeIf(NotificationWithEasingProgress::isExpired);

        int i = 0;
        Window window = MinecraftClient.getInstance().getWindow();

        this.setX(window.getScaledWidth() - Math.min(this.getWidth(), 200));
        this.setY(window.getScaledHeight() - this.getHeight());

        final int boxIWidth = 190;
        final int boxHeight = 40;
        final int rightMargin = 10;
        final int bottomMargin = 10;
        final int verticalSpacing = 60;
        final int stripeWidth = 4;
        final int paddingLeft = 10;
        final int paddingRight = 10;
        final int cornerRadius = 2;

        boolean flipX = this.getX() + (this.getWidth() / 2) >= window.getScaledWidth() / 2;
        boolean flipY = this.getY() + (this.getHeight() / 2) >= window.getScaledHeight() / 2;

        int anchorLeft = this.getX();
        int anchorRight = this.getX() + this.getWidth();
        int anchorTop = this.getY();
        int anchorBottom = this.getY() + this.getHeight();

        for (NotificationWithEasingProgress n : notifications) {
            TextRenderer renderer = Cigarette.REGULAR != null ? Cigarette.REGULAR
                    : MinecraftClient.getInstance().textRenderer;
            int measuredTextMaxWidth = Math.max(renderer.getWidth(n.getNotification().getMessage()),
                    renderer.getWidth(n.getNotification().getTitle())) + paddingLeft + paddingRight;
            int boxWidth = Math.min(measuredTextMaxWidth, boxIWidth);

            float slide;
            float ticks = n.getTicksElapsed();
            float appear = NotificationWithEasingProgress.APPEAR_TICKS;
            float visible = NotificationWithEasingProgress.VISIBLE_TICKS;
            float total = NotificationWithEasingProgress.TOTAL_TICKS;
            float disappear = Math.max(0f, total - (appear + visible));

            if (ticks <= 0f) {
                slide = 0f;
            } else if (ticks < appear) {
                float t = ticks / appear;
                slide = (float) CigaretteScreen.easeOutExpo(t);
            } else if (ticks < appear + visible) {
                slide = 1f;
            } else if (ticks < total && disappear > 0f) {
                float t = (ticks - (appear + visible)) / disappear;
                slide = 1f - (float) CigaretteScreen.easeInExpo(Math.max(0f, Math.min(1f, t)));
            } else {
                slide = 0f;
            }

            int baseRight = anchorRight - rightMargin;
            int baseLeft = anchorLeft + rightMargin;

            int offsetX = Math.round((1f - slide) * (boxWidth + rightMargin + stripeWidth));
            int left, right;
            if (flipX) {

                right = baseRight + offsetX;
                left = right - boxWidth;
            } else {

                left = baseLeft - offsetX;
                right = left + boxWidth;
            }

            int top, bottom;
            if (flipY) {

                int baseBottom = anchorBottom - bottomMargin - i * verticalSpacing;
                bottom = baseBottom;
                top = bottom - boxHeight;
            } else {

                int baseTop = anchorTop + bottomMargin + i * verticalSpacing;
                top = baseTop;
                bottom = top + boxHeight;
            }

            int winW = window.getScaledWidth();
            int winH = window.getScaledHeight();
            int clampedLeft = Math.max(0, Math.min(left, winW)) - 3;
            int clampedRight = Math.max(0, Math.min(right, winW));
            int clampedTop = Math.max(0, Math.min(top, winH));
            int clampedBottom = Math.max(0, Math.min(bottom, winH));

            if (clampedLeft >= clampedRight || clampedTop >= clampedBottom) {
                i++;
                continue;
            }

            String notificationType = n.getNotification().getType();

            int stripeColor = notificationType == null ? 0xFF999999
                    : (notificationType.equals("info") ? 0xFF3AA655
                            : notificationType.equals("warning") ? 0xFFFFA500
                                    : notificationType.equals("error") ? 0xFFFF5C5C : 0xFF999999);

            int bg = CigaretteScreen.BACKGROUND_COLOR;
            Shape.roundedRect(context, clampedLeft, clampedTop, clampedRight, clampedBottom, bg,
                    cornerRadius);

            float visibleStart = NotificationWithEasingProgress.APPEAR_TICKS;
            float visibleEnd = NotificationWithEasingProgress.APPEAR_TICKS
                    + NotificationWithEasingProgress.VISIBLE_TICKS;
            float visibleLength = Math.max(1f, visibleEnd - visibleStart);

            float lifeElapsed = Math.max(0f, Math.min(1f, (n.getTicksElapsed() - visibleStart) / visibleLength));

            float eased = (float) CigaretteScreen.easeOutExpo(lifeElapsed);

            int actualBoxWidth = Math.max(0, clampedRight - clampedLeft);
            int progressBarWidth = Math.round(eased * actualBoxWidth);
            progressBarWidth = Math.max(0, Math.min(actualBoxWidth, progressBarWidth));

            int barHeight = 3;
            int barLeft = clampedLeft;
            int barRight = clampedLeft + progressBarWidth;
            int barTop = clampedBottom - barHeight;
            int barBottom = clampedBottom;

            barLeft = Math.max(clampedLeft, Math.min(barLeft, clampedRight));
            barRight = Math.max(clampedLeft, Math.min(barRight, clampedRight));

            if (barLeft < barRight && barTop < barBottom) {
                int barRadius = Math.max(0, Math.min(
                        cornerRadius,
                        Math.min((barRight - barLeft) / 2, barHeight / 2)));

                Shape.roundedRect(context,
                        barLeft + 1, barTop, barRight, barBottom,
                        0xFF00FF00,
                        0,
                        progressBarWidth > (actualBoxWidth - barRadius) ? 0 : barRadius,
                        0,
                        barRadius);
            }

            int rad = Math.max(0, Math.min(cornerRadius,
                    Math.min(Math.max(0, clampedRight - clampedLeft) / 2,
                            Math.max(0, clampedBottom - clampedTop) / 2)));
            int h = Math.max(0, clampedBottom - clampedTop);
            for (int y = clampedTop; y < clampedBottom; y++) {
                int yIndex = y - clampedTop;
                int leftEdgeX = clampedLeft;
                if (rad > 0) {
                    if (yIndex < rad) {
                        int dy = (rad - 1) - yIndex;
                        int dx = (int) Math.floor(Math.sqrt((double) rad * rad - (double) dy * dy));
                        leftEdgeX = clampedLeft + rad - dx;
                    } else if (yIndex >= h - rad) {
                        int dy = yIndex - (h - rad);
                        int dx = (int) Math.floor(Math.sqrt((double) rad * rad - (double) dy * dy));
                        leftEdgeX = clampedLeft + rad - dx;
                    }
                }
                int sx0 = Math.max(clampedLeft, leftEdgeX);
                int sx1 = Math.min(clampedRight, sx0 + stripeWidth);
                if (sx0 < sx1) {
                    context.fill(sx0, y, sx1, y + 1, stripeColor);
                }
            }

            TextRenderer regularTextRenderer = renderer;
            TextRenderer boldTextRenderer = regularTextRenderer;

            int contentLeft = clampedLeft + paddingLeft;

            String titleStr = n.getNotification().getTitle();
            String msgStr = n.getNotification().getMessage();
            String titleTrim = titleStr;
            String msgTrim = msgStr;
            context.drawText(boldTextRenderer, titleTrim, contentLeft - 3, clampedTop + 8,
                    CigaretteScreen.PRIMARY_TEXT_COLOR, true);
            context.drawText(regularTextRenderer, msgTrim, contentLeft - 3, clampedTop + 23,
                    0xDDFFFFFF, true);

            i++;
        }
    }
}