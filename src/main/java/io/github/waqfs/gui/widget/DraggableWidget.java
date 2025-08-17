package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class DraggableWidget extends BaseWidget<BaseWidget.Stateless> {
    public interface DragCallback {
        void updateParentPosition(int newX, int newY, int deltaX, int deltaY);
    }

    public interface ClickCallback {
        void onClick(double mouseX, double mouseY, int button);
    }

    private boolean dragging = false;
    private int startingX = 0;
    private int startingY = 0;
    private double startingMouseX = 0;
    private double startingMouseY = 0;
    private @Nullable DragCallback dragCallback = null;
    private @Nullable ClickCallback clickCallback = null;

    public DraggableWidget(int x, int y, int width, int height, Text message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    public DraggableWidget(Text message) {
        super(message, null);
        this.captureHover();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            this.setFocused();
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                    this.startingX = this.getX();
                    this.startingY = this.getY();
                    this.startingMouseX = mouseX;
                    this.startingMouseY = mouseY;
                    this.dragging = true;
                    return true;
                }
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                    this.dragging = false;
                    if (clickCallback != null) {
                        clickCallback.onClick(mouseX, mouseY, button);
                    }
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double ignored, double ignored_) {
        if (dragging) {
            int deltaX = (int) (mouseX - startingMouseX);
            int deltaY = (int) (mouseY - startingMouseY);
            int newX = startingX + deltaX;
            int newY = startingY + deltaY;
            MinecraftClient mc = MinecraftClient.getInstance();
            int scrW = mc.getWindow().getScaledWidth();
            int scrH = mc.getWindow().getScaledHeight();
            newX = Math.max(0, Math.min(newX, scrW - this.width));
            newY = Math.max(0, Math.min(newY, scrH - this.height));
            this.setX(newX);
            this.setY(newY);
            if (dragCallback != null) {
                dragCallback.updateParentPosition(newX, newY, deltaX, deltaY);
            }
        }

        return dragging;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return false;
    }

    public void onDrag(DragCallback callback) {
        this.dragCallback = callback;
    }

    public void onClick(ClickCallback callback) {
        this.clickCallback = callback;
    }

    public static class ColorUtil {
        public static int lerpColor(int color1, int color2, float t) {
            if (t < 0f)
                t = 0f;
            else if (t > 1f)
                t = 1f;
            int a = ((int) lerp((color1 >> 24) & 0xFF, (color2 >> 24) & 0xFF, t)) << 24;
            int r = ((int) lerp((color1 >> 16) & 0xFF, (color2 >> 16) & 0xFF, t)) << 16;
            int g = ((int) lerp((color1 >> 8) & 0xFF, (color2 >> 8) & 0xFF, t)) << 8;
            int b = (int) lerp(color1 & 0xFF, color2 & 0xFF, t);
            return a | r | g | b;
        }

        public static int rgba2Int(int r, int g, int b, int a) {
            return (a << 24) | (r << 16) | (g << 8) | b;
        }

        private static float lerp(float start, float end, float t) {
            return start + (end - start) * t;
        }
    }

    public static int color(int x, int y) {
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) x / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) y / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = ColorUtil.lerpColor(CigaretteScreen.PRIMARY_COLOR, 0xFF020618, (float) pingpong);
        return bg;
    }

    public static void pixelAt(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 1, y + 1, color);
    }

    public static void arc(DrawContext context, int centerX, int centerY, int radius, int startAngle, int endAngle,
            int color) {
        double angleStep = 1.0 / radius;
        for (double angle = Math.toRadians(startAngle); angle <= Math.toRadians(endAngle); angle += angleStep) {
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            pixelAt(context, x, y, color);
        }
    }

    private enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    }

    private static void fillQuarterCircle(DrawContext context, int cx, int cy, int r, Corner corner, int color) {
        for (int dy = 0; dy <= r; dy++) {
            int dx = (int) Math.floor(Math.sqrt(r * r - dy * dy));
            switch (corner) {
                case TOP_LEFT -> context.fill(cx - dx, cy - dy, cx + 1, cy - dy + 1, color);
                case TOP_RIGHT -> context.fill(cx, cy - dy, cx + dx + 1, cy - dy + 1, color);
                case BOTTOM_LEFT -> context.fill(cx - dx, cy + dy, cx + 1, cy + dy + 1, color);
                case BOTTOM_RIGHT -> context.fill(cx, cy + dy, cx + dx + 1, cy + dy + 1, color);
            }
        }
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color, int r) {
        context.fill(left + r, top, right - r, bottom, color);
        context.fill(left, top + r, left + r, bottom - r, color);
        context.fill(right - r, top + r, right, bottom - r, color);
        fillQuarterCircle(context, left + r, top + r, r, Corner.TOP_LEFT, color);
        fillQuarterCircle(context, right - r - 1, top + r, r, Corner.TOP_RIGHT, color);
        fillQuarterCircle(context, left + r, bottom - r - 1, r, Corner.BOTTOM_LEFT, color);
        fillQuarterCircle(context, right - r - 1, bottom - r - 1, r, Corner.BOTTOM_RIGHT, color);
    }

    public static void roundedRect(DrawContext context, int left, int top, int right, int bottom, int color, int r,
            boolean topCorners, boolean bottomCorners) {
        context.fill(left + r, top, right - r, bottom, color);
        context.fill(left, top + (topCorners ? r : 0), left + r, bottom - (bottomCorners ? r : 0), color);
        context.fill(right - r, top + (topCorners ? r : 0), right, bottom - (bottomCorners ? r : 0), color);

        if (topCorners) {
            fillQuarterCircle(context, left + r, top + r, r, Corner.TOP_LEFT, color);
            fillQuarterCircle(context, right - r - 1, top + r, r, Corner.TOP_RIGHT, color);
        }
        if (bottomCorners) {
            fillQuarterCircle(context, left + r, bottom - r - 1, r, Corner.BOTTOM_LEFT, color);
            fillQuarterCircle(context, right - r - 1, bottom - r - 1, r, Corner.BOTTOM_RIGHT, color);
        }
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
            int top, int right, int bottom) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int bgColor = color(left, top);
        roundedRect(context, left, top, right, bottom, bgColor, 2, true, false);
        Text text = getMessage();
        int textWidth = textRenderer.getWidth(text);
        int horizontalMargin = (width - textWidth) / 2;
        int verticalMargin = (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, text, left + horizontalMargin, top + verticalMargin + 1,
                CigaretteScreen.PRIMARY_TEXT_COLOR, true);
    }
}