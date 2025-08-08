package io.github.waqfs.gui.widget;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public abstract class RootModule<Widget extends PassthroughWidget<?, StateType>, StateType> extends PassthroughWidget<BaseWidget<?>, StateType> {
    protected @Nullable Consumer<Boolean> moduleStateCallback = null;

    public RootModule(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    public abstract Widget buildModule(String message, @Nullable String tooltip);

    public void onStateSwitch(Consumer<Boolean> newState) {
        this.moduleStateCallback = newState;
    }

    public abstract void registerAsOption(String key);

    public void triggerModuleStateUpdate(boolean state) {
        if (moduleStateCallback == null) return;
        moduleStateCallback.accept(state);
    }
}
