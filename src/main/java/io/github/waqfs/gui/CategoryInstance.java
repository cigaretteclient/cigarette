package io.github.waqfs.gui;

import io.github.waqfs.gui.widget.BaseWidget;
import io.github.waqfs.gui.widget.ScrollableWidget;
import io.github.waqfs.module.BaseModule;
import net.minecraft.text.Text;

import java.util.HashSet;

public class CategoryInstance {
    public final ScrollableWidget<BaseWidget<?>> widget;
    public final HashSet<BaseModule<?, ?>> children = new HashSet<>();

    public CategoryInstance(String displayName, int x, int y) {
        this.widget = new ScrollableWidget<>(x, y).setHeader(Text.literal(displayName));
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
