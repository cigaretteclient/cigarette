package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class KeybindWidget extends BaseWidget<Integer> {
    /**
     * The internal Minecraft KeyBinding.
     */
    private final KeyBinding keyBinding;
    /**
     * The KeyBindings actual key for rendering and configuration.
     */
    private InputUtil.Key utilKey;

    /**
     * Creates a widget that stores a keybind and allows it to be configured.
     *
     * @param message The text to display inside this widget
     * @param tooltip The tooltip to render when this widget is hovered
     */
    public KeybindWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        this.utilKey = InputUtil.UNKNOWN_KEY;
        this.keyBinding = new KeyBinding(UUID.randomUUID().toString(), GLFW.GLFW_KEY_UNKNOWN, "cigarette.null");
        KeyBindingHelper.registerKeyBinding(this.keyBinding);
    }

    /**
     * Sets the key and stored default key of this widget.
     *
     * @param key The default key to set
     * @return This widget for method chaining
     */
    public KeybindWidget withDefaultKey(int key) {
        utilKey = InputUtil.fromKeyCode(key, 0);
        keyBinding.setBoundKey(utilKey);
        super.withDefault(key);
        return this;
    }

    /**
     * Update the stored key of this widget from an input event.
     *
     * @param key The new key to set to this widget
     */
    public void setBoundKey(@Nullable InputUtil.Key key) {
        utilKey = key == null ? InputUtil.UNKNOWN_KEY : key;
        keyBinding.setBoundKey(utilKey);
        this.setRawState(utilKey.getCode());
    }

    /**
     * {@return the internal Minecraft KeyBinding}
     */
    public KeyBinding getKeybind() {
        return this.keyBinding;
    }

    /**
     * Stops this widget from capturing keys to update binding.
     */
    protected void clearBinding() {
        CigaretteScreen.bindingKey = null;
    }

    /**
     * Toggles whether this widget is currently binding and capturing keys.
     */
    protected void toggleBinding() {
        CigaretteScreen.bindingKey = isBinding() ? null : this;
    }

    /**
     * {@return whether this widget is currently being binded by the user}
     */
    protected boolean isBinding() {
        return CigaretteScreen.bindingKey == this;
    }

    @Override
    public void registerConfigKey(String key) {
        super.registerConfigKeyAnd(key, newState -> {
            if (!(newState instanceof Integer integerState)) return;
            this.withDefaultKey(integerState);
        });
    }

    @Override
    public void registerConfigKeyAnd(String key, Consumer<Object> loadedState) {
        super.registerConfigKeyAnd(key, newState -> {
            if (newState instanceof Integer integerState) {
                this.withDefault(integerState);
            }
            loadedState.accept(newState);
        });
    }

    /**
     * Captures a mouse click to toggle whether the keybind is being binded.
     *
     * @param mouseX the X coordinate of the mouse
     * @param mouseY the Y coordinate of the mouse
     * @param button the mouse button number
     * @return Whether this widget handled the click
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                toggleBinding();
            }
            return true;
        }
        return false;
    }

    /**
     * Captures a key press to bind to the internal KeyBinding.
     *
     * @param keyCode   the named key code of the event as described in the {@link org.lwjgl.glfw.GLFW GLFW} class
     * @param scanCode  the unique/platform-specific scan code of the keyboard input
     * @param modifiers a GLFW bitfield describing the modifier keys that are held down (see <a href="https://www.glfw.org/docs/3.3/group__mods.html">GLFW Modifier key flags</a>)
     * @return Whether this widget handled the key press
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isBinding()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.setBoundKey(null);
        } else {
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            String keyName = key.getLocalizedText().getLiteralString();
            if (keyName == null) return true;
            this.setBoundKey(key);
        }
        clearBinding();
        return true;
    }

    /**
     * Renders the key or binding state of this widget at a specific location.
     *
     * @param context The draw context to draw on
     * @param top     The upper-bounding Y position to draw within
     * @param right   The right-bounding X position to draw within
     */
    public void renderKeyText(DrawContext context, int top, int right) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

        String keyText;
        if (this.isBinding()) {
            keyText = "...";
        } else if (utilKey == InputUtil.UNKNOWN_KEY) {
            keyText = "None";
        } else {
            String keyName = utilKey.getLocalizedText().getLiteralString();
            keyText = Objects.requireNonNullElse(keyName, "???");
        }
        Text value = Text.literal(keyText);
        context.drawTextWithShadow(textRenderer, value, right - textRenderer.getWidth(value) - 4, top + height / 3, CigaretteScreen.SECONDARY_COLOR);
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        context.drawTextWithShadow(Cigarette.REGULAR, getMessage(), left + 4, top + height / 3, CigaretteScreen.PRIMARY_TEXT_COLOR);
        this.renderKeyText(context, top, right);
    }
}
