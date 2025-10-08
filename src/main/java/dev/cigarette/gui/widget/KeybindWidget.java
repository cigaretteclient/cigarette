package dev.cigarette.gui.widget;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.helper.keybind.VirtualKeybind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import java.util.function.Consumer;

public class KeybindWidget extends BaseWidget<Integer> {
    private final VirtualKeybind keyBinding = new VirtualKeybind(GLFW.GLFW_KEY_UNKNOWN);
    private InputUtil.Key utilKey = InputUtil.UNKNOWN_KEY;

    public KeybindWidget(String message, @Nullable String tooltip) {
        super(message, tooltip);
        KeybindHelper.registerVirtualKey(this.keyBinding);
    }

    public KeybindWidget withDefaultKey(int key) {
        utilKey = InputUtil.fromKeyCode(key, 0);
        keyBinding.setDefaultKey(key);
        super.withDefault(key);
        return this;
    }

    public void setBoundKey(int key) {
        utilKey = key == GLFW.GLFW_KEY_UNKNOWN ? InputUtil.UNKNOWN_KEY : InputUtil.fromKeyCode(key, 0);
        keyBinding.setKey(key);
        this.setRawState(key);
    }

    public VirtualKeybind getKeybind() {
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
            this.setBoundKey(GLFW.GLFW_KEY_UNKNOWN);
        } else {
            InputUtil.Key key = InputUtil.fromKeyCode(keyCode, scanCode);
            String keyName = key.getLocalizedText().getLiteralString();
            if (keyName == null) return true;
            this.setBoundKey(keyCode);
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
