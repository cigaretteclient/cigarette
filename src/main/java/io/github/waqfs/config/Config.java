package io.github.waqfs.config;

import io.github.waqfs.Cigarette;
import io.github.waqfs.agent.DevWidget;
import io.github.waqfs.gui.CategoryInstance;
import io.github.waqfs.module.bedwars.*;
import io.github.waqfs.module.keybind.AddGlassBlock;
import io.github.waqfs.module.keybind.BreakBlock;
import io.github.waqfs.module.keybind.VClip;
import io.github.waqfs.module.murdermystery.GoldESP;
import io.github.waqfs.module.zombies.ZombieESP;

public class Config {
    public CategoryInstance keybinds = new CategoryInstance("Keybinds", 10, 10);
    public CategoryInstance murderMystery = new CategoryInstance("Murder Mystery", 120, 10);
    public CategoryInstance bedwars = new CategoryInstance("Bed Wars", 230, 10);
    public CategoryInstance zombies = new CategoryInstance("Zombies", 340, 10);

    public CategoryInstance[] allCategories = new CategoryInstance[]{keybinds, murderMystery, bedwars, zombies, Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    public Config() {
        this.keybinds.attach(new AddGlassBlock(), new BreakBlock(), new VClip());
        this.murderMystery.attach(new io.github.waqfs.module.murdermystery.PlayerESP(), new GoldESP());
        this.bedwars.attach(new io.github.waqfs.module.bedwars.PlayerESP(), new FireballESP(), new ProjectileESP(), new EntityESP(), new DefenseViewer(), new JumpReset(), new AutoClicker(), new PerfectHit(), new AutoTool(), new Bridger());
        this.zombies.attach(new io.github.waqfs.module.zombies.PlayerESP(), new ZombieESP());
    }
}
