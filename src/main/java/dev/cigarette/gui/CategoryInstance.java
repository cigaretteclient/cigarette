package dev.cigarette.gui;

import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import dev.cigarette.module.BaseModule;
import net.minecraft.text.Text;

import java.util.HashSet;

public class CategoryInstance {
    public final ScrollableWidget<BaseWidget<?>> widget;
    public final HashSet<BaseModule<?, ?>> children = new HashSet<>();
    public boolean expanded = true;

    public CategoryInstance(String displayName, int x, int y) {
        this.widget = new ScrollableWidget<>(x, y);
        this.widget.setHeader(Text.literal(displayName), () -> {
            this.expanded = !this.expanded;
        });
    }

    public CategoryInstance attach(BaseModule<?, ?>... children) {
        BaseWidget<?>[] childWidgets = new BaseWidget[children.length];
        for (int i = 0; i < children.length; i++) {
            BaseModule<?, ?> module = children[i];
            childWidgets[i] = module.wrapper != null ? module.wrapper : module.widget;
            this.children.add(children[i]);
        }
        this.widget.setChildren(childWidgets);
        return this;
    }
}
