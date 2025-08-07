package io.github.waqfs.module;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.widget.RootModule;
import org.jetbrains.annotations.Nullable;

public abstract class BaseModule<T extends RootModule<T>> {
    protected boolean state = false;
    private final String key;
    public T widget;

    public BaseModule(T widgetClass, String key, String displayName, @Nullable String tooltip) {
        this.key = key;
        this.widget = widgetClass.buildModule(displayName, tooltip);
        this.widget.registerAsOption(key);
        this.widget.onStateSwitch(newState -> {
            this.state = newState;
            if (newState) {
                this.whenEnabled();
            } else {
                this.whenDisabled();
            }
            FileSystem.updateState(this.key, newState);
        });
    }

    protected void whenEnabled() {
    }

    protected void whenDisabled() {
    }
}
