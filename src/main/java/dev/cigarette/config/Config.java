package dev.cigarette.config;

import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.module.BaseModule;
import dev.cigarette.module.bedwars.*;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.JumpReset;
import dev.cigarette.module.combat.PerfectHit;
import dev.cigarette.module.keybind.AddGlassBlock;
import dev.cigarette.module.keybind.BreakBlock;
import dev.cigarette.module.keybind.VClip;
import dev.cigarette.module.murdermystery.GoldESP;
import dev.cigarette.module.murdermystery.PlayerESP;
import dev.cigarette.module.render.ProjectileESP;
import dev.cigarette.module.ui.*;
import dev.cigarette.module.zombies.Aimbot;
import dev.cigarette.module.zombies.PowerupESP;
import dev.cigarette.module.zombies.ReviveAura;
import dev.cigarette.module.zombies.ZombieESP;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

import java.util.TreeMap;

public class Config {
    public final TreeMap<String, CategoryInstance> CATEGORIES = new TreeMap<>();

    private void constructCategory(String name) {
        if (CATEGORIES.containsKey(name)) return;
        CATEGORIES.put(name, new CategoryInstance(name, 0, 0));
        positionCategories();
    }

    public void putCategory(String name, CategoryInstance category) {
        CATEGORIES.put(name, category);
        positionCategories();
    }

    public void putModules(String categoryName, BaseModule<?, ?>... modules) {
        this.constructCategory(categoryName);

        CategoryInstance category = CATEGORIES.get(categoryName);
        assert category != null;

        for (BaseModule<?, ?> module : modules) {
            category.attach(module);
        }
    }

    public void positionCategories() {
        Window window = MinecraftClient.getInstance().getWindow();
        if (window == null) return;

        int x = 10, y = 10;
        int maxX = window.getScaledWidth() - 120;
        for (CategoryInstance category : CATEGORIES.values()) {
            category.widget.withXY(x, y);
            x += 110;
            if (x > maxX) {
                x = 10;
                y += 30;
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

    public static Config construct() {
        Config cfg = new Config();
        cfg.putModules("Bedwars", AutoBlockIn.INSTANCE, AutoTool.INSTANCE, Bridger.INSTANCE, DefenseViewer.INSTANCE, EntityESP.INSTANCE, FireballESP.INSTANCE);
        cfg.putModules("Combat", AutoClicker.INSTANCE, JumpReset.INSTANCE, PerfectHit.INSTANCE);
        cfg.putModules("Keybind", AddGlassBlock.INSTANCE, BreakBlock.INSTANCE, VClip.INSTANCE);
        cfg.putModules("Murder Mystery", GoldESP.INSTANCE, PlayerESP.INSTANCE);
        cfg.putModules("Render", dev.cigarette.module.render.PlayerESP.INSTANCE, ProjectileESP.INSTANCE);
        cfg.putModules("UI", GUI.INSTANCE, ModuleList.INSTANCE, Notifications.INSTANCE, TargetHUD.INSTANCE, Watermark.INSTANCE);
        cfg.putModules("Zombies", Aimbot.INSTANCE, PowerupESP.INSTANCE, ReviveAura.INSTANCE, ZombieESP.INSTANCE);
        if (Cigarette.IN_DEV_ENVIRONMENT) {
            cfg.putCategory("Agents", DevWidget.CATEGORY_INSTANCE);
        }
        return cfg;
    }
}
