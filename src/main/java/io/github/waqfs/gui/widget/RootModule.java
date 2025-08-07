package io.github.waqfs.gui.widget;

import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class RootModule<T extends PassthroughWidget<?>> extends PassthroughWidget<ClickableWidget> {
    protected @Nullable Consumer<Boolean> moduleStateCallback = null;

    public RootModule(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public abstract T buildModule(String message, @Nullable String tooltip);

    public void onStateSwitch(Consumer<Boolean> newState) {
        this.moduleStateCallback = newState;
    }

    public abstract void registerAsOption(String key);

    public void triggerModuleStateUpdate(boolean state) {
        if (moduleStateCallback == null) return;
        moduleStateCallback.accept(state);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
