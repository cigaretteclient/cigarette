package io.github.waqfs.module;

import io.github.waqfs.config.FileSystem;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class BaseModule {
    protected boolean state = false;
    private final String key;
    public ToggleOptionsWidget widget;

    public void enable() {
        this.state = true;
        FileSystem.updateState(this.key, true);
    }

    public void disable() {
        this.state = false;
        FileSystem.updateState(this.key, false);
    }

    public void toggle() {
        this.state = !this.state;
        FileSystem.updateState(this.key, this.state);
    }

    public boolean isEnabled() {
        return this.state;
    }

    public BaseModule(String key, String displayName, @Nullable String tooltip) {
        this.key = key;
        if (tooltip != null) {
            this.widget = new ToggleOptionsWidget(Text.literal(displayName), Text.literal(tooltip));
        } else {
            this.widget = new ToggleOptionsWidget(Text.literal(displayName));
        }
        this.widget.registerUpdate(newState -> {
            this.state = newState;
            if (newState) {
                this.whenEnabled();
            } else {
                this.whenDisabled();
            }
            FileSystem.updateState(this.key, newState);
        });
        FileSystem.registerUpdate(key, newState -> {
            if (!(newState instanceof Boolean)) return;
            this.state = (Boolean) newState;
            this.widget.setState(this.state);
        });
    }

    protected void whenEnabled() {
    }

    protected void whenDisabled() {
    }
}
