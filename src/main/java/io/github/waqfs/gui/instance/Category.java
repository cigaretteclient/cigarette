package io.github.waqfs.gui.instance;

import io.github.waqfs.gui.widget.BaseWidget;
import io.github.waqfs.gui.widget.ScrollableWidget;
import io.github.waqfs.module.BaseModule;
import net.minecraft.text.Text;

import java.util.HashSet;

public class Category {
    public final ScrollableWidget<BaseWidget> widget;
    public final HashSet<BaseModule> children = new HashSet<>();

    public Category(String displayName, int x, int y) {
        this.widget = new ScrollableWidget<>(x, y).setHeader(Text.literal(displayName));
    }

    public Category attach(BaseModule... children) {
        BaseWidget[] childWidgets = new BaseWidget[children.length];
        for (int i = 0; i < children.length; i++) {
            childWidgets[i] = children[i].widget;
            this.children.add(children[i]);
        }
        this.widget.setChildren(childWidgets);
        return this;
    }
}
