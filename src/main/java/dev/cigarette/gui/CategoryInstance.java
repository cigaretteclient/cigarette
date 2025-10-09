package dev.cigarette.gui;

import dev.cigarette.gui.widget.BaseWidget;
import dev.cigarette.gui.widget.ScrollableWidget;
import dev.cigarette.module.BaseModule;

import java.util.HashSet;

/**
 * A category that groups modules together to be rendered by the GUI.
 */
public class CategoryInstance {
    /**
     * The root widget that contains each module's top-level widget for rendering.
     */
    public final ScrollableWidget<BaseWidget<?>> widget;
    /**
     * The list of each module attached to this category.
     */
    public final HashSet<BaseModule<?, ?>> children = new HashSet<>();
    /**
     * Whether this category is expanded or collapsed.
     */
    public boolean expanded = false;

    /**
     * Creates a new category to group modules.
     *
     * @param displayName The text to display in the header of the category.
     * @param x           The initial X position of this category
     * @param y           The initial Y position of this category
     */
    public CategoryInstance(String displayName, int x, int y) {
        this.widget = new ScrollableWidget<>(x, y);
        this.widget.setHeader(displayName, () -> {
            this.expanded = !this.expanded;
        });
        this.widget.alphabetic();
    }

    /**
     * Attaches a list of modules to this category.
     *
     * @param children The list of modules to attach
     */
    public void attach(BaseModule<?, ?>... children) {
        BaseWidget<?>[] childWidgets = new BaseWidget[children.length];
        for (int i = 0; i < children.length; i++) {
            BaseModule<?, ?> module = children[i];
            childWidgets[i] = module.wrapper != null ? module.wrapper : module.widget;
            this.children.add(children[i]);
        }
        this.widget.setChildren(childWidgets);
    }
}
