package io.github.waqfs.gui;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.instance.Category;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class CigaretteScreen extends Screen {
    private Screen parent = null;

    protected CigaretteScreen() {
        super(Text.literal("Cigarette Client"));
    }

    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        for (Category category : Cigarette.CONFIG.allCategories) {
            if (category == null) continue;
            addDrawableChild(category.widget);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Element child : this.children()) {
            boolean handled = child.mouseClicked(mouseX, mouseY, button);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Element child : this.children()) {
            boolean handled = child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            if (handled) return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        for (Element child : this.children()) {
            child.mouseDragged(x, y, button, deltaX, deltaY);
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Element child : this.children()) {
            child.mouseReleased(mouseX, mouseY, button);
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (Element child : this.children()) {
            child.mouseMoved(mouseX, mouseY);
        }
    }


    @Override
    public void close() {
        assert client != null;
        client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }
}
