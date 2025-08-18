package io.github.waqfs.gui.notifications;

import java.util.ArrayList;
import java.util.List;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
import io.github.waqfs.gui.widget.DraggableWidget;
import io.github.waqfs.gui.notifications.internal.NotificationWithEasingProgress;
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
        super(MinecraftClient.getInstance().getWindow().getScaledWidth() - 200, 0,
                200, MinecraftClient.getInstance().getWindow().getScaledHeight(), Text.of("Notifications"));
        Cigarette.EVENTS.registerListener(Notification.class, (event) -> {
            handleNotification(event);
            return null;
        });
    }

    private <T> void handleNotification(T event) {
        if (event instanceof Notification) {
            Notification notification = (Notification) event;
            notifications.addFirst(new NotificationWithEasingProgress(notification));
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    
    public static void imageRender(DrawContext context, int x, int y) {
        context.drawTexture(
                RenderLayer::getGuiTextured,
                Cigarette.LOGO_IDENTIFIER, x + 7, y, 0f, 0f, 30, 30, 30, 30);
        // context.drawText(Cigarette.REGULAR, "cigarette", x, y + 40, DraggableWidget.color(x, y + 40), true);
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < "cigarette".length(); i++) {
            colors.add(DraggableWidget.color(x + i * 10, y + 40));
        }
        drawTextGradient(context, "cigarette", x, y + 40, colors.stream().mapToInt(i -> i).toArray());
    }

    public static void imageRender(DrawContext context, int x, int y, double gradientStaggerModifier) {
        context.drawTexture(
                RenderLayer::getGuiTextured,
                Cigarette.LOGO_IDENTIFIER, x + 7, y, 0f, 0f, 30, 30, 30, 30);
        List<Integer> colors = new ArrayList<>();
        for (int i = 0; i < "cigarette".length(); i++) {
            colors.add(DraggableWidget.color((int)(x + i * 10 * gradientStaggerModifier), (int)(y + 40 * gradientStaggerModifier)));
        }
        drawTextGradient(context, "cigarette", x, y + 40, colors.stream().mapToInt(i -> i).toArray());
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
        imageRender(context, 10, 10);

        notifications.forEach(n -> n.updateProgress(deltaTicks));
        notifications.removeIf(NotificationWithEasingProgress::isExpired);

        int i = 0;
        Window window = MinecraftClient.getInstance().getWindow();
        final int boxIWidth = 190;
        final int boxHeight = 40;
        final int rightMargin = 10;
        final int bottomMargin = 10;
        final int verticalSpacing = 60;
        final int stripeWidth = 4;
        final int paddingLeft = 10;
        final int paddingRight = 10;
        final int cornerRadius = 2;

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

            int baseRight = window.getScaledWidth() - rightMargin;
            int offsetX = Math.round((1f - slide) * (boxWidth + rightMargin + stripeWidth));
            int right = baseRight + offsetX;
            int left = right - boxWidth;

            int baseBottom = window.getScaledHeight() - bottomMargin - i * verticalSpacing;
            int bottom = baseBottom;
            int top = bottom - boxHeight;

            int winW = window.getScaledWidth();
            int winH = window.getScaledHeight();
            int clampedLeft = Math.max(0, Math.min(left, winW));
            int clampedRight = Math.max(0, Math.min(right, winW));
            int clampedTop = Math.max(0, Math.min(top, winH));
            int clampedBottom = Math.max(0, Math.min(bottom, winH));

            if (clampedLeft >= clampedRight || clampedTop >= clampedBottom) {
                i++;
                continue;
            }

            String notificationType = n.getNotification().getType();
            int stripeLeft = clampedLeft - stripeWidth;
            int stripeRight = clampedLeft;
            int stripeColor = notificationType == null ? 0xFF999999
                    : (notificationType.equals("info") ? 0xFF3AA655
                            : notificationType.equals("warning") ? 0xFFFFA500
                                    : notificationType.equals("error") ? 0xFFFF5C5C : 0xFF999999);

            int bg = CigaretteScreen.BACKGROUND_COLOR;
            DraggableWidget.roundedRect(context, clampedLeft, clampedTop, clampedRight, clampedBottom, bg,
                    cornerRadius);

            float visibleStart = NotificationWithEasingProgress.APPEAR_TICKS;
            float visibleEnd = NotificationWithEasingProgress.APPEAR_TICKS
                    + NotificationWithEasingProgress.VISIBLE_TICKS;
            float visibleLength = Math.max(1f, visibleEnd - visibleStart);

            float lifeElapsed = Math.max(0f, Math.min(1f, (n.getTicksElapsed() - visibleStart) / visibleLength));

            float eased = (float)CigaretteScreen.easeOutExpo(lifeElapsed);

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
                context.fill(barLeft, barTop, barRight, barBottom, 0xFF00FF00);
            }

            int clampedStripeLeft = Math.max(0, Math.min(stripeLeft, winW));
            int clampedStripeRight = Math.max(0, Math.min(stripeRight, winW));

            if (clampedStripeLeft < clampedStripeRight) {
                context.fill(clampedStripeLeft + 3, clampedTop, clampedStripeRight + 3, clampedBottom,
                        stripeColor);
            }

            TextRenderer regularTextRenderer = renderer;
            TextRenderer boldTextRenderer = regularTextRenderer;

            int contentLeft = clampedLeft + paddingLeft;
            int contentRight = clampedRight - paddingRight;
            int contentWidth = Math.max(0, contentRight - contentLeft);

            String titleStr = n.getNotification().getTitle();
            String msgStr = n.getNotification().getMessage();
            String titleTrim = titleStr;
            String msgTrim = msgStr;
            if (contentWidth > 0) {
                while (boldTextRenderer.getWidth(titleTrim) > contentWidth && titleTrim.length() > 0) {
                    titleTrim = titleTrim.substring(0, Math.max(0, titleTrim.length() - 1));
                }
                if (!titleTrim.equals(titleStr))
                    titleTrim = titleTrim + "…";

                while (regularTextRenderer.getWidth(msgTrim) > contentWidth && msgTrim.length() > 0) {
                    msgTrim = msgTrim.substring(0, Math.max(0, msgTrim.length() - 1));
                }
                if (!msgTrim.equals(msgStr))
                    msgTrim = msgTrim + "…";
            }
            context.drawText(boldTextRenderer, titleTrim, contentLeft, clampedTop + 3,
                    CigaretteScreen.PRIMARY_TEXT_COLOR, true);
            context.drawText(regularTextRenderer, msgTrim, contentLeft, clampedTop + 18,
                    0xDDFFFFFF, true);

            i++;
        }
    }
}