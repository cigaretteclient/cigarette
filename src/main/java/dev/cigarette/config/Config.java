package dev.cigarette.config;

import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.module.BaseModule;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.JumpReset;
import dev.cigarette.module.combat.PerfectHit;
import dev.cigarette.module.murdermystery.PlayerESP;
import dev.cigarette.module.ui.Watermark;
import dev.cigarette.module.zombies.Aimbot;
import net.minecraft.client.MinecraftClient;

import java.util.TreeMap;

public class Config {
    public final TreeMap<String, CategoryInstance> CATEGORIES = new TreeMap<>();
    public CategoryInstance[] allCategories = new CategoryInstance[]{Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    public final Watermark RENDER_WATERMARK = new Watermark();
    public final AutoClicker COMBAT_AUTOCLICKER = new AutoClicker();
    public final JumpReset COMBAT_JUMP_RESET = new JumpReset();
    public final PerfectHit COMBAT_PERFECT_HIT = new PerfectHit();
    public final PlayerESP MYSTERY_PLAYERESP = new PlayerESP();
    public final Aimbot ZOMBIES_AIMBOT = new Aimbot();

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
