package dev.cigarette.gui.widget;

import dev.cigarette.module.BaseModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleKeybindWidget extends ToggleWidget {
    protected KeybindWidget widget;

    public ToggleKeybindWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.widget = new KeybindWidget(message, null);
    }

    public ToggleKeybindWidget withDefaultState(boolean state) {
        super.withDefaultState(state);
        return this;
    }

    public ToggleKeybindWidget withDefaultKey(int keyCode) {
        this.widget.withDefaultKey(keyCode);
        return this;
    }

    public KeyBinding getKeybind() {
        return this.widget.getKeybind();
    }

    public static BaseModule.GeneratedWidgets<ToggleKeybindWidget, Boolean> keybindModule(Text displayName, @Nullable Text tooltip) {
        ToggleKeybindWidget widget = new ToggleKeybindWidget(displayName, tooltip);
        return new BaseModule.GeneratedWidgets<>(null, widget);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.widget.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        this.widget.setHeight(height);
    }

    @Override
    public void registerConfigKey(String key) {
        super.registerConfigKey(key);
        this.widget.registerConfigKey(key + ".key");
    }

    @Override
    public void registerConfigKeyAnd(String key, Consumer<Object> loadedState) {
        super.registerConfigKeyAnd(key, loadedState);
        this.widget.registerConfigKeyAnd(key, loadedState);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        System.out.println("click button=" + button);
        switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> {
                super.mouseClicked(mouseX, mouseY, button);
            }
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE, GLFW.GLFW_MOUSE_BUTTON_RIGHT -> {
                System.out.println("right click passed");
                this.widget.toggleBinding();
            }
        }
        return true;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        super.render(context, hovered, mouseX, mouseY, deltaTicks, left, top, right, bottom);
        this.widget.renderKeyText(context, top, right);
    }
}
