package dev.cigarette.gui.widget;

import net.minecraft.client.gui.Element;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class PassthroughWidget<ChildType extends BaseWidget<?>, StateType> extends BaseWidget<StateType> {
    protected @Nullable ChildType[] children;
    protected int childLeftOffset = 0;

    public PassthroughWidget(int x, int y, int width, int height, Text message) {
        super(message, null);
        this.withXY(x, y).withWH(width, height);
    }

    public PassthroughWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
    }

    public PassthroughWidget(Text message) {
        super(message, null);
    }

    @Override
    public void unfocus() {
        if (this.children == null) return;
        for (BaseWidget<?> child : children) {
            if (child == null) continue;
            child.unfocus();
        }
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
        boolean wasHandled = false;
        for (BaseWidget<?> child : children) {
            if (child == null) continue;
            if (wasHandled) {
                child.unfocus();
                continue;
            }
            wasHandled = child.mouseClicked(mouseX, mouseY, button);
            if (!wasHandled) child.unfocus();
            else child.setFocused();
        }
        return wasHandled;
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
