package io.github.waqfs.gui.widget;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleWidget extends BaseWidget<Boolean> {
    private static final int MAX_HOVER_TICKS = 35;
    private int ticksOnHover = 0;
    private @Nullable Consumer<Boolean> callback = null;

    public ToggleWidget(Text message, @Nullable Text tooltip) {
        super(message, tooltip);
        this.captureHover().withDefault(false);
    }

    public void registerAsOption(String key) {
        this.registerUpdate(newState -> {
            this.setRawState(newState);
            FileSystem.updateState(key, newState);
            if (this.callback != null) this.callback.accept(newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Boolean booleanState)) return;
            this.setRawState(booleanState);
            if (this.callback != null) this.callback.accept(booleanState);
        });
    }

    public void registerUpdate(Consumer<Boolean> callback) {
        this.callback = callback;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.setRawState(!this.getRawState());
        }
        return true;
    }

    @Override
    protected void render(DrawContext context, boolean hovered, int mouseX, int mouseY, float deltaTicks, int left, int top, int right, int bottom) {
        if (hovered) {
            ticksOnHover = Math.min(++ticksOnHover, MAX_HOVER_TICKS);
            context.fillGradient(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR, CigaretteScreen.DARK_BACKGROUND_COLOR);
        } else {
            ticksOnHover = Math.max(--ticksOnHover, 0);
            context.fill(left, top, right, bottom, CigaretteScreen.BACKGROUND_COLOR);
        }

        int textColor = this.getRawState() ? CigaretteScreen.ENABLED_COLOR : CigaretteScreen.PRIMARY_TEXT_COLOR;

        if (ticksOnHover > 0) {
            float progress = (float) ticksOnHover / MAX_HOVER_TICKS;
            context.drawHorizontalLine(left, (int) (left + width * progress), top, textColor);
            context.drawHorizontalLine((int) (right - width * progress), right, bottom - 1, textColor);
            context.drawVerticalLine(left, (int) (bottom - height * progress), bottom, textColor);
            context.drawVerticalLine(right - 1, top, (int) (top + height * progress), textColor);
        }

        context.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, getMessage(), left + 4, top + height / 3, textColor);
    }
}
