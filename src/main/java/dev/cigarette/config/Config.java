package dev.cigarette.config;

import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.module.BaseModule;
import net.minecraft.client.MinecraftClient;

import java.util.TreeMap;

public class Config {
    public final TreeMap<String, CategoryInstance> CATEGORIES = new TreeMap<>();
    public CategoryInstance[] allCategories = new CategoryInstance[]{Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    private void constructCategory(String name) {
        if (CATEGORIES.containsKey(name)) return;
        CATEGORIES.put(name, new CategoryInstance(name, 0, 0));

        int x = 10, y = 10;
        int maxX = MinecraftClient.getInstance().getWindow().getScaledWidth() - 10;
        for (CategoryInstance category : CATEGORIES.values()) {
            category.widget.withXY(x, y);
            x += category.widget.getWidth() + 10;
            if (x > maxX) {
                x = 10;
                y += category.widget.getHeight() + 10;
            }
        }
    }

    public <M extends BaseModule<?, ?>> M constructModule(M moduleRef, String categoryName) {
        this.constructCategory(categoryName);

        CategoryInstance category = CATEGORIES.get(categoryName);
        assert category != null;

        category.attach(moduleRef);

        return moduleRef;
    }
}
