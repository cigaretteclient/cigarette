package io.github.waqfs.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class DraggableWidget extends ClickableWidget {
    public interface DragCallback {
        public void updateParentPosition(int newX, int newY, int deltaX, int deltaY);
    }

    private static final int BASE_COLOR = 0xFFFE5F00;
    private static final int BASE_TEXT_COLOR = 0xFF000000;
    private boolean dragging = false;
    private int startingX = 0;
    private int startingY = 0;
    private double startingMouseX = 0;
    private double startingMouseY = 0;
    private @Nullable DragCallback dragCallback = null;

    public DraggableWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            this.dragging = true;
            this.startingX = this.getX();
            this.startingY = this.getY();
            this.startingMouseX = mouseX;
            this.startingMouseY = mouseY;
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
        dragging = false;
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX > getX() && mouseX < getRight() && mouseY > getY() && mouseY < getBottom();
    }

    public void onDrag(DragCallback callback) {
        this.dragCallback = callback;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.fill(getX(), getY(), getRight(), getBottom(), BASE_COLOR);
        context.drawCenteredTextWithShadow(textRenderer, getMessage(), getX() + width / 2, getY() + height / 3, BASE_TEXT_COLOR);
    }
}