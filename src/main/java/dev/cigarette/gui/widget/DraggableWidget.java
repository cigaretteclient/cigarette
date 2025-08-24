package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.lib.Color;
import dev.cigarette.lib.Shape;
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
    public boolean expanded = true;

    private int ticksOnCollapse = 0;
    private static final int MAX_TICKS_ON_COLLAPSE = 10;

    public DraggableWidget(int x, int y, int width, int height, String message) {
        super(message, null);
        this.captureHover().withXY(x, y).withWH(width, height);
    }

    public DraggableWidget(String message) {
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




    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left,
            int top, int right, int bottom) {
        TextRenderer textRenderer = Cigarette.REGULAR;
        int bgColor = Color.color(left, top);
        if (!this.expanded) {
            ticksOnCollapse = Math.min(ticksOnCollapse + 1, MAX_TICKS_ON_COLLAPSE);
        } else {
            ticksOnCollapse = Math.max(ticksOnCollapse - 1, 0);
        }
        double progress = ticksOnCollapse / (double) MAX_TICKS_ON_COLLAPSE;
        progress = CigaretteScreen.easeOutExpo(progress);
        Shape.roundedRect(context, left, top, right, bottom, bgColor, 2, true, !this.expanded);
        if (this.expanded) {
            int borderColor = Color.colorDarken(bgColor, 0.8f);
            context.drawHorizontalLine(left, right - 1, bottom - 1, borderColor);
        }
        Text text = getMessage();
        int textWidth = textRenderer.getWidth(text);
        int horizontalMargin = (width - textWidth) / 2;
        int verticalMargin = (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, text, left + horizontalMargin, top + verticalMargin + 1,
                CigaretteScreen.PRIMARY_TEXT_COLOR, false);
    }
}