package io.github.waqfs.gui;

import io.github.waqfs.gui.widget.ScrollableWidget;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
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
        ScrollableWidget<ClickableWidget> category1 = new ScrollableWidget<>(10, 10).setHeader(Text.literal("Category 1"));
        ScrollableWidget<ClickableWidget> category2 = new ScrollableWidget<>(120, 10).setHeader(Text.literal("Category 2"));

        ToggleOptionsWidget toggle1 = new ToggleOptionsWidget(Text.literal("Toggle 1"), Text.literal("cigarette"));
        ToggleOptionsWidget toggle2 = new ToggleOptionsWidget(Text.literal("Toggle 2"), Text.literal("client"));
        ToggleOptionsWidget toggle3 = new ToggleOptionsWidget(Text.literal("Toggle 3"));

        ToggleOptionsWidget option1 = new ToggleOptionsWidget(Text.literal("Option 1"), Text.literal("otp1"));
        ToggleOptionsWidget option2 = new ToggleOptionsWidget(Text.literal("Option 2"), Text.literal("otp2"));
        ToggleOptionsWidget option3 = new ToggleOptionsWidget(Text.literal("Option 3"), Text.literal("otp3"));
        ToggleOptionsWidget option4 = new ToggleOptionsWidget(Text.literal("Option 4"), Text.literal("otp4"));
        ToggleOptionsWidget option5 = new ToggleOptionsWidget(Text.literal("Option 5"), Text.literal("otp5"));
        ToggleOptionsWidget option6 = new ToggleOptionsWidget(Text.literal("Option 6"), Text.literal("otp6"));
        ToggleOptionsWidget option7 = new ToggleOptionsWidget(Text.literal("Option 7"), Text.literal("otp7"));
        ToggleOptionsWidget option8 = new ToggleOptionsWidget(Text.literal("Option 8"), Text.literal("otp8"));
        ToggleOptionsWidget option9 = new ToggleOptionsWidget(Text.literal("Option 9"), Text.literal("otp9"));
        ToggleOptionsWidget option0 = new ToggleOptionsWidget(Text.literal("Option 0"), Text.literal("otp0"));
        ToggleOptionsWidget option11 = new ToggleOptionsWidget(Text.literal("Option 8"), Text.literal("otp8"));
        ToggleOptionsWidget option12 = new ToggleOptionsWidget(Text.literal("Option 9"), Text.literal("otp9"));
        ToggleOptionsWidget option13 = new ToggleOptionsWidget(Text.literal("Option 0"), Text.literal("otp0"));

        toggle1.setOptions(option1, option2);
        toggle3.setOptions(option3, option4, option5, option6, option7, option8, option9, option0, option11, option12, option13);
        category1.setChildren(toggle1, toggle2);
        category2.setChildren(toggle3);

        addDrawableChild(category1);
        addDrawableChild(category2);
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
