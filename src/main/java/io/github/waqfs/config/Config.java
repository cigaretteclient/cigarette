package io.github.waqfs.config;

import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.module.keybind.AddGlassBlock;
import io.github.waqfs.module.keybind.BreakBlock;
import io.github.waqfs.module.keybind.VClip;

public class Config {
    public Category keybinds = new Category("Keybinds", 10, 10);

    public Category[] allCategories = new Category[]{keybinds};

    public Config() {
        this.keybinds.attach(new AddGlassBlock(), new BreakBlock(), new VClip());
    }
}
