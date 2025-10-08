package dev.cigarette.gui.widget;

import dev.cigarette.module.BaseModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * A widget that can be toggled by the user and lets a key be bound.
 */
public class ToggleKeybindWidget extends ToggleWidget {
    /**
     * The {@link KeybindWidget} for handling configuration of the keybind.
     */
    protected KeybindWidget widget;

    /**
     * Creates a widget that stores a keybind and can be toggled.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public ToggleKeybindWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.widget = new KeybindWidget(message, null);
    }

    public ToggleKeybindWidget withDefaultState(boolean state) {
        super.withDefaultState(state);
        return this;
    }

    /**
     * Sets the key and stored default key of this widget.
     *
     * @param keyCode The default key to set
     * @return This widget for method chaining
     */
    public ToggleKeybindWidget withDefaultKey(int keyCode) {
        this.widget.withDefaultKey(keyCode);
        return this;
    }

    /**
     * {@return the internal Minecraft KeyBinding}
     */
    public KeyBinding getKeybind() {
        return this.widget.getKeybind();
    }

    /**
     * Generator for modules using this as a top-level widget.
     *
     * @param displayName The text to display inside this widget
     * @param tooltip     The tooltip to render when this widget is hovered
     * @return A {@link BaseModule.GeneratedWidgets} object for use in {@link BaseModule} constructing
     */
    public static BaseModule.GeneratedWidgets<ToggleKeybindWidget, Boolean> keybindModule(String displayName, @Nullable String tooltip) {
        ToggleKeybindWidget widget = new ToggleKeybindWidget(displayName, tooltip);
        return new BaseModule.GeneratedWidgets<>(null, widget);
    }

    /**
     * Sets the width of this widget.
     *
     * @param width The widget of the widget
     */
    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        this.widget.setWidth(width);
    }

    /**
     * Sets the height of this widget.
     *
     * @param height The height of the widget
     */
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
