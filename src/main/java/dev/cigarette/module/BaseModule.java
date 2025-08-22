package dev.cigarette.module;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.hud.notification.Notification;
import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.DropdownWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class BaseModule<Widget extends BaseWidget<Boolean>, Boolean> {
    private final String key;
    public final @Nullable DropdownWidget<Widget, Boolean> wrapper;
    public final Widget widget;
    private boolean prevEnabled = false;

    public BaseModule(WidgetGenerator<Widget, Boolean> func, String key, String displayName, @Nullable String tooltip) {
        this.key = key;
        GeneratedWidgets<Widget, Boolean> widgets = func.accept(Text.literal(displayName), tooltip == null ? null : Text.literal(tooltip));
        this.wrapper = widgets.dropdown;
        this.widget = widgets.widget;
        this.prevEnabled = this.getRawState();
        this.widget.registerModuleCallback(newState -> {
            if ((boolean)newState == this.prevEnabled) return;
            if (Cigarette.EVENTS != null) {
                Cigarette.EVENTS.dispatchEvent(new Notification(Map.of(
                    "type", "info",
                    "title", "Module Toggled",
                    "message", "Module " + displayName + " was " + ((boolean)newState ? "enabled" : "disabled")
                )));
            }
            if ((boolean) newState) {
                this.whenEnabled();
            } else {
                this.whenDisabled();
            }
            prevEnabled = (boolean) newState;
        });
        if (this.wrapper != null) {
            this.wrapper.registerConfigKey(key);
        } else {
            this.widget.registerConfigKey(key);
        }
    }

    public boolean getRawState() {
        return (boolean) this.widget.getRawState();
    }

    public void setChildren(BaseWidget<?>... widgets) {
        if (this.wrapper == null) throw new IllegalStateException("Cannot define children on a non-dropdown module.");
        this.wrapper.setChildren(widgets);
    }

    protected void whenEnabled() {
    }

    protected void whenDisabled() {
    }

    @FunctionalInterface
    public interface WidgetGenerator<HeaderType extends BaseWidget<?>, StateType> {
        GeneratedWidgets<HeaderType, StateType> accept(Text displayName, @Nullable Text tooltip);
    }

    public record GeneratedWidgets<HeaderType extends BaseWidget<?>, StateType>(@Nullable DropdownWidget<HeaderType, StateType> dropdown, HeaderType widget) {
    }
}
