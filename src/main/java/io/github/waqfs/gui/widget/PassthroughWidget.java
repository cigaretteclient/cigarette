package io.github.waqfs.gui.widget;

import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class PassthroughWidget<T extends Element> extends ClickableWidget {
    protected @Nullable T[] children;

    public PassthroughWidget(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX > this.getX() && mouseX < this.getX() + this.width && mouseY > this.getY() && mouseY < this.getY() + this.height;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (children == null) return;
        for (Element child : children) {
            if (child == null) continue;
            child.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            boolean handled = child.mouseClicked(mouseX, mouseY, button);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            child.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            child.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            boolean handled = child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            boolean handled = child.keyPressed(keyCode, scanCode, modifiers);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            boolean handled = child.keyReleased(keyCode, scanCode, modifiers);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (children == null) return false;
        for (Element child : children) {
            if (child == null) continue;
            boolean handled = child.charTyped(chr, modifiers);
            if (handled) return true;
        }
        return false;
    }
}
