package io.github.waqfs.gui.widget;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.CigaretteScreen;
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
    private final KeyBinding keyBinding;
    private InputUtil.Key utilKey;

    public KeybindWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.utilKey = InputUtil.UNKNOWN_KEY;
        this.keyBinding = new KeyBinding(UUID.randomUUID().toString(), GLFW.GLFW_KEY_UNKNOWN, "cigarette.null");
        KeyBindingHelper.registerKeyBinding(this.keyBinding);
    }

    public KeybindWidget withDefaultKey(int key) {
        utilKey = InputUtil.fromKeyCode(key, 0);
        keyBinding.setBoundKey(utilKey);
        super.withDefault(key);
        return this;
    }

    public void setBoundKey(@Nullable InputUtil.Key key) {
        utilKey = key == null ? InputUtil.UNKNOWN_KEY : key;
        keyBinding.setBoundKey(utilKey);
        this.setRawState(utilKey.getCode());
    }

    public KeyBinding getKeybind() {
        return this.keyBinding;
    }

    protected void clearBinding() {
        CigaretteScreen.bindingKey = null;
    }

    protected void toggleBinding() {
        CigaretteScreen.bindingKey = isBinding() ? null : this;
    }

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
