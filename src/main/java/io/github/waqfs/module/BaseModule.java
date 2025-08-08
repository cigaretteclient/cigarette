package io.github.waqfs.module;

import io.github.waqfs.gui.widget.BaseWidget;
import io.github.waqfs.gui.widget.DropdownWidget;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public abstract class BaseModule<Widget extends BaseWidget<Boolean>, Boolean> {
    private final String key;
    public final DropdownWidget<Widget, Boolean> wrapper;
    public final Widget widget;

    public BaseModule(WidgetGenerator<Widget, Boolean> func, String key, String displayName, @Nullable String tooltip) {
        this.key = key;
        GeneratedWidgets<Widget, Boolean> widgets = func.accept(Text.literal(displayName), tooltip == null ? null : Text.literal(tooltip));
        this.wrapper = widgets.dropdown;
        this.widget = widgets.widget;
        this.widget.registerModuleCallback(newState -> {
            if ((boolean) newState) {
                this.whenEnabled();
            } else {
                this.whenDisabled();
            }
        });
        this.widget.registerConfigKey(key);
    }

    public boolean getRawState() {
        return (boolean) this.widget.getRawState();
    }

    public void setChildren(BaseWidget<?>... widgets) {
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

    public record GeneratedWidgets<HeaderType extends BaseWidget<?>, StateType>(DropdownWidget<HeaderType, StateType> dropdown, HeaderType widget) {
    }
}
