package io.github.waqfs.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

// Placeholder... DO NOT USE (yet!)
public class KeybindWidget extends PassthroughWidget<BaseWidget<KeyBinding>, BaseWidget.Stateless> {
    private KeyBinding keyBinding;

    public KeybindWidget(KeyBinding keyBinding) {
        super(keyBinding.getBoundKeyLocalizedText(), Text.of(keyBinding.getCategory()));
        this.withDefault(new BaseWidget.Stateless());
        this.keyBinding = keyBinding;
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        context.fill(left, top, right, bottom, 0x111111FF);
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        context.fill(left - 2, top - 2, right + 2, bottom + 2, 0x000000FF);
        context.drawText(textRenderer, keyBinding.getBoundKeyLocalizedText(), left + 5, top + 2, bottom, hovered);
    }
}
