package io.github.waqfs.gui.widget;

import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

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
            this.dragging = true;
            this.startingX = this.getX();
            this.startingY = this.getY();
            this.startingMouseX = mouseX;
            this.startingMouseY = mouseY;
            this.setFocused();
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
            this.setX(newX);
            this.setY(newY);
            if (dragCallback != null) {
                dragCallback.updateParentPosition(newX, newY, deltaX, deltaY);
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.startingX == this.getX() && this.startingY == this.getY()) {
            if (clickCallback != null) {
                clickCallback.onClick(mouseX, mouseY, button);
            }
        }
        dragging = false;
        return false;
    }

    public void onDrag(DragCallback callback) {
        this.dragCallback = callback;
    }

    public void onClick(ClickCallback callback) {
        this.clickCallback = callback;
    }

    protected class ColorUtil {
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

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
            int top, int right, int bottom) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        double seconds = (System.nanoTime() / 1_000_000_000.0);
        int screenW = 1920, screenH = 1080;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            screenW = mc.getWindow().getScaledWidth();
            screenH = mc.getWindow().getScaledHeight();
        }
        float xNorm = Math.max(0f, Math.min(1f, (float) getX() / Math.max(1, screenW)));
        float yNorm = Math.max(0f, Math.min(1f, (float) getY() / Math.max(1, screenH)));
        float s = xNorm + 0.2f * yNorm;
        double speedHz = 0.3;
        double phase = 2 * Math.PI * (speedHz * seconds - s);
        float pingpong = 0.5f * (1.0f + (float) Math.sin(phase));
        int bg = ColorUtil.lerpColor(CigaretteScreen.PRIMARY_COLOR, CigaretteScreen.SECONDARY_COLOR, (float) pingpong);
        context.fill(left, top, right, bottom, bg);
        Text text = getMessage();
        int textWidth = textRenderer.getWidth(text);
        int horizontalMargin = (width - textWidth) / 2;
        int verticalMargin = (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, text, left + horizontalMargin, top + verticalMargin + 1,
                CigaretteScreen.PRIMARY_TEXT_COLOR, true);
    }
}