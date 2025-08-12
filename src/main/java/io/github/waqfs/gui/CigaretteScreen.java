package io.github.waqfs.gui;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.gui.widget.BaseWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Stack;

public class CigaretteScreen extends Screen {
    public static final int PRIMARY_COLOR = 0xFFFE5F00;
    public static final int SECONDARY_COLOR = 0xFFC44700;
    public static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    public static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    public static final int DARK_BACKGROUND_COLOR = 0xFF000000;
    public static final int ENABLED_COLOR = 0xFF3AFC3A;
    public static @Nullable Object hoverHandled = null;
    private final Stack<BaseWidget<?>> priority = new Stack<>();
    private Screen parent = null;

    protected CigaretteScreen() {
        super(Text.literal("Cigarette Client"));
    }

    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    private void unfocusChildren() {
        for (BaseWidget<?> child : priority) {
            child.unfocus();
            child.setFocused();
        }
    }

    @Override
    protected void init() {
        for (Category category : Cigarette.CONFIG.allCategories) {
            if (category == null) continue;
            addDrawableChild(category.widget);
            this.priority.addFirst(category.widget);
            category.widget.unfocus();
            category.widget.setFocused();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        unfocusChildren();
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseClicked(mouseX, mouseY, button);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (BaseWidget<?> child : priority) {
            boolean handled = child.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            if (handled) {
                priority.remove(child);
                priority.addFirst(child);
                return true;
            }
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
        unfocusChildren();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    public static boolean isHoverable(Object obj) {
        if (hoverHandled == null) {
            hoverHandled = obj;
            return true;
        }
        return hoverHandled == obj;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.renderBackground(context, mouseX, mouseY, deltaTicks);

        CigaretteScreen.hoverHandled = null;
        for (int i = 0; i < priority.size(); i++) {
            BaseWidget<?> widget = priority.get(i);
            context.getMatrices().push();
            context.getMatrices().translate(0, 0, priority.size() - i);
            widget._render(context, mouseX, mouseY, deltaTicks);
            context.getMatrices().pop();
        }
    }
}
