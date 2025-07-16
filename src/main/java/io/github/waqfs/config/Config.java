package io.github.waqfs.config;

import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.module.keybind.AddGlassBlock;
import io.github.waqfs.module.keybind.BreakBlock;

public class Config {
    public Category category1 = new Category("Keybinds", 10, 10);

    public Category[] allCategories = new Category[]{category1};

    public Config() {
        this.category1.attach(new AddGlassBlock(), new BreakBlock());
    }
}
