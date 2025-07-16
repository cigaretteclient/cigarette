package io.github.waqfs.config;

import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.module.ExampleModule;

public class Config {
    public Category category1 = new Category("Category 1", 10, 10);
    public ExampleModule example = new ExampleModule();

    public Category[] allCategories = new Category[]{category1};

    public Config() {
        this.category1.attach(example);
    }
}
